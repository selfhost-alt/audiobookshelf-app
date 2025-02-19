package com.audiobookshelf.app.player

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.audiobookshelf.app.data.LibraryItem
import com.audiobookshelf.app.data.LibraryItemWrapper
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector

class MediaSessionPlaybackPreparer(var playerNotificationService:PlayerNotificationService) : MediaSessionConnector.PlaybackPreparer {
  var tag = "MediaSessionPlaybackPreparer"

  override fun onCommand(player: Player, command: String, extras: Bundle?, cb: ResultReceiver?): Boolean {
    Log.d(tag, "ON COMMAND $command")
    return false
  }

  override fun getSupportedPrepareActions(): Long {
    return PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
      PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
      PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
      PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
  }

  override fun onPrepare(playWhenReady: Boolean) {
    Log.d(tag, "ON PREPARE $playWhenReady")
    playerNotificationService.mediaManager.getFirstItem()?.let { li ->
      playerNotificationService.mediaManager.play(li, playerNotificationService.getMediaPlayer()) {
        Handler(Looper.getMainLooper()).post() {
          playerNotificationService.preparePlayer(it,playWhenReady,null)
        }
      }
    }
  }

  override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
    Log.d(tag, "ON PREPARE FROM MEDIA ID $mediaId $playWhenReady")

    var libraryItemWrapper: LibraryItemWrapper? = playerNotificationService.mediaManager.getById(mediaId)
    libraryItemWrapper?.let { li ->
      playerNotificationService.mediaManager.play(li, playerNotificationService.getMediaPlayer()) {
        Log.d(tag, "About to prepare player with ${it.displayTitle}")
        Handler(Looper.getMainLooper()).post() {
          playerNotificationService.preparePlayer(it,playWhenReady,null)
        }
      }
    }
  }

  override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {
    Log.d(tag, "ON PREPARE FROM SEARCH $query")
    playerNotificationService.mediaManager.getFromSearch(query)?.let { li ->
      playerNotificationService.mediaManager.play(li, playerNotificationService.getMediaPlayer()) {
        Log.d(tag, "About to prepare player with ${it.displayTitle}")
        Handler(Looper.getMainLooper()).post() {
          playerNotificationService.preparePlayer(it,playWhenReady,null)
        }
      }
    }
  }

  override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) {
    Log.d(tag, "ON PREPARE FROM URI $uri")
  }
}
