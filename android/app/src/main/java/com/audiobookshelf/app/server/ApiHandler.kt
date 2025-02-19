package com.audiobookshelf.app.server

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.audiobookshelf.app.data.*
import com.audiobookshelf.app.device.DeviceManager
import com.audiobookshelf.app.player.MediaProgressSyncData
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ApiHandler(var ctx:Context) {
  val tag = "ApiHandler"

  private var client = OkHttpClient()
  var jacksonMapper = jacksonObjectMapper().enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())

  var storageSharedPreferences: SharedPreferences? = null

  data class LocalMediaProgressSyncPayload(val localMediaProgress:List<LocalMediaProgress>)
  @JsonIgnoreProperties(ignoreUnknown = true)
  data class MediaProgressSyncResponsePayload(val numServerProgressUpdates:Int, val localProgressUpdates:List<LocalMediaProgress>)
  data class LocalMediaProgressSyncResultsPayload(var numLocalMediaProgressForServer:Int, var numServerProgressUpdates:Int, var numLocalProgressUpdates:Int)

  fun getRequest(endpoint:String, cb: (JSObject) -> Unit) {
    val request = Request.Builder()
      .url("${DeviceManager.serverAddress}$endpoint").addHeader("Authorization", "Bearer ${DeviceManager.token}")
      .build()
    makeRequest(request, cb)
  }

  fun postRequest(endpoint:String, payload: JSObject, cb: (JSObject) -> Unit) {
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = payload.toString().toRequestBody(mediaType)
    val request = Request.Builder().post(requestBody)
      .url("${DeviceManager.serverAddress}$endpoint").addHeader("Authorization", "Bearer ${DeviceManager.token}")
      .build()
    makeRequest(request, cb)
  }

  fun patchRequest(endpoint:String, payload: JSObject, cb: (JSObject) -> Unit) {
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = payload.toString().toRequestBody(mediaType)
    val request = Request.Builder().patch(requestBody)
      .url("${DeviceManager.serverAddress}$endpoint").addHeader("Authorization", "Bearer ${DeviceManager.token}")
      .build()
    makeRequest(request, cb)
  }

  fun isOnline(): Boolean {
    val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    if (capabilities != null) {
      if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
        Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
        return true
      } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
        Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
        return true
      } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
        Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
        return true
      }
    }
    return false
  }

  fun makeRequest(request:Request, cb: (JSObject) -> Unit) {
    client.newCall(request).enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        Log.d(tag, "FAILURE TO CONNECT")
        e.printStackTrace()
        cb(JSObject())
      }

      override fun onResponse(call: Call, response: Response) {
        response.use {
          if (!it.isSuccessful) throw IOException("Unexpected code $response")

              val bodyString = it.body!!.string()
          if (bodyString == "OK") {
            cb(JSObject())
          } else {
            var jsonObj = JSObject()
            if (bodyString.startsWith("[")) {
              val array = JSArray(bodyString)
              jsonObj.put("value", array)
            } else {
              jsonObj = JSObject(bodyString)
            }
            cb(jsonObj)
          }
        }
      }
    })
  }

  fun getLibraries(cb: (List<Library>) -> Unit) {
    val mapper = jacksonMapper
    getRequest("/api/libraries") {
      val libraries = mutableListOf<Library>()
      if (it.has("value")) {
        val array = it.getJSONArray("value")
        for (i in 0 until array.length()) {
          val library = mapper.readValue<Library>(array.get(i).toString())
          libraries.add(library)
        }
      }
      cb(libraries)
    }
  }

  fun getLibraryItem(libraryItemId:String, cb: (LibraryItem) -> Unit) {
    getRequest("/api/items/$libraryItemId?expanded=1") {
      val libraryItem = jacksonMapper.readValue<LibraryItem>(it.toString())
      cb(libraryItem)
    }
  }

  fun getLibraryItemWithProgress(libraryItemId:String, episodeId:String?, cb: (LibraryItem) -> Unit) {
    var requestUrl = "/api/items/$libraryItemId?expanded=1&include=progress"
    if (!episodeId.isNullOrEmpty()) requestUrl += "&episode=$episodeId"
    getRequest(requestUrl) {
      val libraryItem = jacksonMapper.readValue<LibraryItem>(it.toString())
      cb(libraryItem)
    }
  }

  fun getLibraryItems(libraryId:String, cb: (List<LibraryItem>) -> Unit) {
    getRequest("/api/libraries/$libraryId/items?limit=100&minified=1") {
      val items = mutableListOf<LibraryItem>()
      if (it.has("results")) {
        val array = it.getJSONArray("results")
        for (i in 0 until array.length()) {
          val item = jacksonMapper.readValue<LibraryItem>(array.get(i).toString())
          items.add(item)
        }
      }
      cb(items)
    }
  }

  fun getLibraryCategories(libraryId:String, cb: (List<LibraryCategory>) -> Unit) {
    getRequest("/api/libraries/$libraryId/personalized") {
      val items = mutableListOf<LibraryCategory>()
      if (it.has("value")) {
        val array = it.getJSONArray("value")
        for (i in 0 until array.length()) {
          val jsobj = array.get(i) as JSONObject

          val type = jsobj.get("type").toString()
          // Only support for podcast and book in android auto
          if (type == "podcast" || type == "book") {
            jsobj.put("isLocal", false)
            val item = jacksonMapper.readValue<LibraryCategory>(jsobj.toString())
            items.add(item)
          }
        }
      }
      cb(items)
    }
  }

  fun playLibraryItem(libraryItemId:String, episodeId:String?, forceTranscode:Boolean, mediaPlayer:String, cb: (PlaybackSession) -> Unit) {
    val payload = JSObject()
    payload.put("mediaPlayer", mediaPlayer)

    // Only if direct play fails do we force transcode
    if (!forceTranscode) payload.put("forceDirectPlay", true)
    else payload.put("forceTranscode", true)

    val endpoint = if (episodeId.isNullOrEmpty()) "/api/items/$libraryItemId/play" else "/api/items/$libraryItemId/play/$episodeId"
    postRequest(endpoint, payload) {
      it.put("serverConnectionConfigId", DeviceManager.serverConnectionConfig?.id)
      it.put("serverAddress", DeviceManager.serverAddress)
      val playbackSession = jacksonMapper.readValue<PlaybackSession>(it.toString())
      cb(playbackSession)
    }
  }

  fun sendProgressSync(sessionId:String, syncData: MediaProgressSyncData, cb: () -> Unit) {
    val payload = JSObject(jacksonMapper.writeValueAsString(syncData))

    postRequest("/api/session/$sessionId/sync", payload) {
      cb()
    }
  }

  fun sendLocalProgressSync(playbackSession:PlaybackSession, cb: () -> Unit) {
    val payload = JSObject(jacksonMapper.writeValueAsString(playbackSession))

    postRequest("/api/session/local", payload) {
      cb()
    }
  }

  fun syncMediaProgress(cb: (LocalMediaProgressSyncResultsPayload) -> Unit) {
    if (!isOnline()) {
      Log.d(tag, "Error not online")
      cb(LocalMediaProgressSyncResultsPayload(0,0,0))
      return
    }

    // Get all local media progress connected to items on the current connected server
    val localMediaProgress = DeviceManager.dbManager.getAllLocalMediaProgress().filter {
      it.serverConnectionConfigId == DeviceManager.serverConnectionConfig?.id
    }

    val localSyncResultsPayload = LocalMediaProgressSyncResultsPayload(localMediaProgress.size,0, 0)

    if (localMediaProgress.isNotEmpty()) {
      Log.d(tag, "Sending sync local progress request with ${localMediaProgress.size} progress items")
      val payload = JSObject(jacksonMapper.writeValueAsString(LocalMediaProgressSyncPayload(localMediaProgress)))
      postRequest("/api/me/sync-local-progress", payload) {
        Log.d(tag, "Media Progress Sync payload $payload - response ${it.toString()}")

        if (it.toString() == "{}") {
          Log.e(tag, "Progress sync received empty object")
        } else {
          val progressSyncResponsePayload = jacksonMapper.readValue<MediaProgressSyncResponsePayload>(it.toString())

          localSyncResultsPayload.numLocalProgressUpdates = progressSyncResponsePayload.localProgressUpdates.size
          localSyncResultsPayload.numServerProgressUpdates = progressSyncResponsePayload.numServerProgressUpdates
          Log.d(tag, "Media Progress Sync | Local Updates: $localSyncResultsPayload")
          if (progressSyncResponsePayload.localProgressUpdates.isNotEmpty()) {
            // Update all local media progress
            progressSyncResponsePayload.localProgressUpdates.forEach { localMediaProgress ->
              DeviceManager.dbManager.saveLocalMediaProgress(localMediaProgress)
            }
          }
        }

        cb(localSyncResultsPayload)
      }
    } else {
      Log.d(tag, "No local media progress to sync")
      cb(localSyncResultsPayload)
    }
  }

  fun updateMediaProgress(libraryItemId:String,episodeId:String?,updatePayload:JSObject, cb: () -> Unit) {
    Log.d(tag, "updateMediaProgress $libraryItemId $episodeId $updatePayload")
    val endpoint = if(episodeId.isNullOrEmpty()) "/api/me/progress/$libraryItemId" else "/api/me/progress/$libraryItemId/$episodeId"
    patchRequest(endpoint,updatePayload) {
      Log.d(tag, "updateMediaProgress patched progress")
      cb()
    }
  }
}
