package com.audiobookshelf.app.player

import android.util.Log
import com.audiobookshelf.app.data.PlayerState
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player

class PlayerListener(var playerNotificationService:PlayerNotificationService) : Player.Listener {
  var tag = "PlayerListener"

  companion object {
    var lastPauseTime: Long = 0   //ms
  }

  private var onSeekBack: Boolean = false

  override fun onPlayerError(error: PlaybackException) {
    var errorMessage = error.message ?: "Unknown Error"
    Log.e(tag, "onPlayerError $errorMessage")
    playerNotificationService.handlePlayerPlaybackError(errorMessage) // If was direct playing session, fallback to transcode
  }

  override fun onEvents(player: Player, events: Player.Events) {
    Log.d(tag, "onEvents ${player.deviceInfo} | ${playerNotificationService.getMediaPlayer()} | ${events.size()}")

    if (events.contains(Player.EVENT_POSITION_DISCONTINUITY)) {
      Log.d(tag, "EVENT_POSITION_DISCONTINUITY")
    }

    if (events.contains(Player.EVENT_IS_LOADING_CHANGED)) {
      Log.d(tag, "EVENT_IS_LOADING_CHANGED : " + playerNotificationService.currentPlayer.isLoading)
    }

    if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)) {
      Log.d(tag, "EVENT_PLAYBACK_STATE_CHANGED MediaPlayer = ${playerNotificationService.getMediaPlayer()}")

      if (playerNotificationService.currentPlayer.playbackState == Player.STATE_READY) {
        Log.d(tag, "STATE_READY : " + playerNotificationService.currentPlayer.duration)

        if (lastPauseTime == 0L) {
          lastPauseTime = -1;
        }
        playerNotificationService.sendClientMetadata(PlayerState.READY)
      }
      if (playerNotificationService.currentPlayer.playbackState == Player.STATE_BUFFERING) {
        Log.d(tag, "STATE_BUFFERING : " + playerNotificationService.currentPlayer.currentPosition)
        playerNotificationService.sendClientMetadata(PlayerState.BUFFERING)
      }
      if (playerNotificationService.currentPlayer.playbackState == Player.STATE_ENDED) {
        Log.d(tag, "STATE_ENDED")
        playerNotificationService.sendClientMetadata(PlayerState.ENDED)
      }
      if (playerNotificationService.currentPlayer.playbackState == Player.STATE_IDLE) {
        Log.d(tag, "STATE_IDLE")
        playerNotificationService.sendClientMetadata(PlayerState.IDLE)
      }
    }

    if (events.contains(Player.EVENT_MEDIA_METADATA_CHANGED)) {
      Log.d(tag, "EVENT_MEDIA_METADATA_CHANGED ${playerNotificationService.getMediaPlayer()}")
    }
    if (events.contains(Player.EVENT_PLAYLIST_METADATA_CHANGED)) {
      Log.d(tag, "EVENT_PLAYLIST_METADATA_CHANGED ${playerNotificationService.getMediaPlayer()}")
    }
    if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
      Log.d(tag, "EVENT IS PLAYING CHANGED ${playerNotificationService.getMediaPlayer()}")

      if (player.isPlaying) {
        if (lastPauseTime > 0) {
          if (onSeekBack) onSeekBack = false
          else {
            var backTime = calcPauseSeekBackTime()
            if (backTime > 0) {
              if (backTime >= playerNotificationService.mPlayer.currentPosition) backTime = playerNotificationService.mPlayer.currentPosition - 500
              Log.d(tag, "SeekBackTime $backTime")
              onSeekBack = true
              playerNotificationService.seekBackward(backTime)
            }
          }
        }
      } else lastPauseTime = System.currentTimeMillis()

      // Start/stop progress sync interval
      Log.d(tag, "Playing ${playerNotificationService.getCurrentBookTitle()}")
      if (player.isPlaying) {
        playerNotificationService.mediaProgressSyncer.start()
      } else {
        playerNotificationService.mediaProgressSyncer.stop()
      }

      playerNotificationService.clientEventEmitter?.onPlayingUpdate(player.isPlaying)
    }
  }

  private fun calcPauseSeekBackTime() : Long {
    if (lastPauseTime <= 0) return 0
    var time: Long = System.currentTimeMillis() - lastPauseTime
    var seekback: Long
    if (time < 60000) seekback = 0
    else if (time < 120000) seekback = 10000
    else if (time < 300000) seekback = 15000
    else if (time < 1800000) seekback = 20000
    else if (time < 3600000) seekback = 25000
    else seekback = 29500
    return seekback
  }
}
