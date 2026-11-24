package com.example.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity

class QuranPlaybackService : MediaSessionService() {

  private var mediaSession: MediaSession? = null

  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()
    val audioAttributes =
      AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
        .setUsage(C.USAGE_MEDIA)
        .build()

    val httpDataSourceFactory =
      DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setKeepPostFor302Redirects(true)
        .setConnectTimeoutMs(20_000)
        .setReadTimeoutMs(20_000)
        .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))
        .setUserAgent("Mozilla/5.0 (Linux; Android 14) Tilawah/1.0 ExoPlayerLib")
    val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
    val extractorsFactory =
      DefaultExtractorsFactory()
        .setConstantBitrateSeekingEnabled(true)
        .setConstantBitrateSeekingAlwaysEnabled(true)
    val loadControl =
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(15_000, 50_000, 1_500, 3_000)
        .build()

    val player =
      ExoPlayer.Builder(this)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory))
        .setLoadControl(loadControl)
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    val launchIntent =
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val pendingIntent =
      PendingIntent.getActivity(
        this,
        0,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    mediaSession = MediaSession.Builder(this, player).setSessionActivity(pendingIntent).build()
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
    mediaSession

  internal fun activePlayerForTest() = mediaSession?.player

  override fun onTaskRemoved(rootIntent: Intent?) {
    val player = mediaSession?.player
    if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
      stopSelf()
    }
  }

  override fun onDestroy() {
    mediaSession?.run {
      player.release()
      release()
      mediaSession = null
    }
    super.onDestroy()
  }
}
