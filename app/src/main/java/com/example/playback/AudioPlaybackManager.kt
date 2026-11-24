package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.QuranCatalog
import com.example.data.ReciterCatalogRepository
import com.example.data.UserPreferencesRepository
import com.example.model.ActiveAudioTarget
import com.example.model.PlaybackUiState
import com.example.model.Reciter
import com.example.model.Riwayah
import com.example.model.SleepTimerOption
import com.example.model.Surah
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlaybackManager(
  private val context: Context,
  private val preferencesRepository: UserPreferencesRepository,
  private val catalogRepository: ReciterCatalogRepository? = null,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private val _uiState = MutableStateFlow(PlaybackUiState())
  val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

  private var controllerFuture: ListenableFuture<MediaController>? = null
  private var mediaController: MediaController? = null
  private var pendingAction: (() -> Unit)? = null
  private var positionTickerJob: Job? = null
  private var sleepTimerJob: Job? = null
  private var autoPlayNextEnabled: Boolean = true
  private var lastSavedTick: Long = 0L

  private val triedUrlsForCurrentTrack = mutableSetOf<String>()
  private var refreshedCatalogForCurrentTrack = false
  private var currentStartPositionMs = 0L
  private var currentPortraitUrl: String? = null

  private val playerListener =
    object : Player.Listener {
      override fun onIsPlayingChanged(isPlaying: Boolean) {
        _uiState.update { it.copy(isPlaying = isPlaying) }
        if (isPlaying) {
          startPositionTicker()
        } else {
          stopPositionTicker()
          persistCurrentSurahProgress()
        }
      }

      override fun onPlaybackStateChanged(playbackState: Int) {
        val controller = mediaController ?: return
        when (playbackState) {
          Player.STATE_BUFFERING -> {
            _uiState.update {
              it.copy(
                isBuffering = true,
                bufferedPositionMs = controller.bufferedPosition.coerceAtLeast(0L),
              )
            }
          }
          Player.STATE_READY -> {
            _uiState.update {
              it.copy(
                isBuffering = false,
                durationMs = controller.duration.coerceAtLeast(0L),
                currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                bufferedPositionMs = controller.bufferedPosition.coerceAtLeast(0L),
              )
            }
          }
          Player.STATE_ENDED -> {
            _uiState.update { it.copy(isBuffering = false, isPlaying = false) }
            handlePlaybackEnded()
          }
          Player.STATE_IDLE -> {
            _uiState.update { it.copy(isBuffering = false, isPlaying = false) }
          }
        }
      }

      override fun onPlayerError(error: PlaybackException) {
        val currentTarget = _uiState.value.activeTarget
        when (currentTarget) {
          is ActiveAudioTarget.SurahTrack -> handleSurahPlaybackError(currentTarget)
          ActiveAudioTarget.CairoRadio -> {
            _uiState.update { state ->
              state.copy(
                isPlaying = false,
                isBuffering = false,
                errorMessage = "تعذر الاتصال ببث إذاعة القرآن الكريم من القاهرة. تحقق من اتصال الإنترنت.",
              )
            }
          }
          null -> {
            _uiState.update { state ->
              state.copy(isPlaying = false, isBuffering = false, errorMessage = "تعذر تشغيل الصوت.")
            }
          }
        }
      }
    }

  init {
    scope.launch {
      preferencesRepository.preferencesFlow.collect { prefs ->
        autoPlayNextEnabled = prefs.autoPlayNext
      }
    }
    ensureControllerConnected()
  }

  private fun handleSurahPlaybackError(currentTarget: ActiveAudioTarget.SurahTrack) {
    val fallbackUrl =
      currentTarget.riwayah
        .candidateSurahUrls(currentTarget.surah.number)
        .firstOrNull { !triedUrlsForCurrentTrack.contains(it) }

    if (fallbackUrl != null) {
      triedUrlsForCurrentTrack.add(fallbackUrl)
      playTrackUrl(
        target = currentTarget,
        url = fallbackUrl,
        startPositionMs = currentStartPositionMs,
        portraitUrl = currentPortraitUrl,
      )
      return
    }

    val repo = catalogRepository
    if (!refreshedCatalogForCurrentTrack && repo != null) {
      refreshedCatalogForCurrentTrack = true
      _uiState.update { it.copy(isPlaying = false, isBuffering = true) }
      scope.launch {
        repo.refreshCatalog()
        val activeNow = _uiState.value.activeTarget as? ActiveAudioTarget.SurahTrack
        if (
          activeNow == null ||
            activeNow.reciter.id != currentTarget.reciter.id ||
            activeNow.surah.number != currentTarget.surah.number
        ) {
          return@launch
        }

        val updatedReciter = repo.findReciter(currentTarget.reciter.id)
        val updatedRiwayah =
          repo.findUpdatedRiwayah(
            reciterId = currentTarget.reciter.id,
            riwayahNameAr = currentTarget.riwayah.nameAr,
            previousBaseUrl = currentTarget.riwayah.baseUrl,
          )

        val freshUrl =
          updatedRiwayah
            ?.candidateSurahUrls(currentTarget.surah.number)
            ?.firstOrNull { !triedUrlsForCurrentTrack.contains(it) }

        if (updatedReciter != null && updatedRiwayah != null && freshUrl != null) {
          val refreshedTarget =
            ActiveAudioTarget.SurahTrack(
              reciter = updatedReciter,
              riwayah = updatedRiwayah,
              surah = currentTarget.surah,
            )
          triedUrlsForCurrentTrack.add(freshUrl)
          _uiState.update {
            it.copy(
              activeTarget = refreshedTarget,
              isBuffering = true,
              errorMessage = null,
            )
          }
          playTrackUrl(
            target = refreshedTarget,
            url = freshUrl,
            startPositionMs = currentStartPositionMs,
            portraitUrl = currentPortraitUrl,
          )
        } else {
          markSurahUnavailable(currentTarget)
        }
      }
      return
    }

    markSurahUnavailable(currentTarget)
  }

  private fun markSurahUnavailable(target: ActiveAudioTarget.SurahTrack) {
    val failedKey = target.trackKey
    _uiState.update { state ->
      state.copy(
        isPlaying = false,
        isBuffering = false,
        unavailableTrackKeys = state.unavailableTrackKeys + failedKey,
        errorMessage =
          "سورة ${target.surah.nameAr} غير متوفرة حالياً بصوت ${target.reciter.nameAr} (${target.riwayah.nameAr})",
      )
    }
  }

  private fun ensureControllerConnected() {
    if (controllerFuture != null) return
    val sessionToken =
      SessionToken(context, ComponentName(context, QuranPlaybackService::class.java))
    val future = MediaController.Builder(context, sessionToken).buildAsync()
    controllerFuture = future
    future.addListener(
      {
        try {
          val controller = future.get()
          mediaController = controller
          controller.addListener(playerListener)
          syncFromController(controller)
          pendingAction?.invoke()
          pendingAction = null
        } catch (_: Exception) {
          controllerFuture = null
        }
      },
      ContextCompat.getMainExecutor(context),
    )
  }

  private fun syncFromController(controller: MediaController) {
    if (controller.mediaItemCount == 0) return
    _uiState.update {
      it.copy(
        isPlaying = controller.isPlaying,
        isBuffering = controller.playbackState == Player.STATE_BUFFERING,
        currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
        bufferedPositionMs = controller.bufferedPosition.coerceAtLeast(0L),
        durationMs = controller.duration.coerceAtLeast(0L),
      )
    }
    if (controller.isPlaying) {
      startPositionTicker()
    }
  }

  private fun withController(action: (MediaController) -> Unit) {
    val controller = mediaController
    if (controller != null) {
      action(controller)
    } else {
      pendingAction = { mediaController?.let(action) }
      ensureControllerConnected()
    }
  }

  fun playSurah(
    reciter: Reciter,
    riwayah: Riwayah,
    surah: Surah,
    startPositionMs: Long = 0L,
    portraitUrl: String? = null,
  ) {
    val target = ActiveAudioTarget.SurahTrack(reciter, riwayah, surah)
    val url = riwayah.surahUrl(surah.number)

    triedUrlsForCurrentTrack.clear()
    triedUrlsForCurrentTrack.add(url)
    refreshedCatalogForCurrentTrack = false
    currentStartPositionMs = startPositionMs
    currentPortraitUrl = portraitUrl

    _uiState.update {
      it.copy(
        activeTarget = target,
        isBuffering = true,
        errorMessage = null,
        currentPositionMs = startPositionMs,
        durationMs = 0L,
      )
    }

    playTrackUrl(
      target = target,
      url = url,
      startPositionMs = startPositionMs,
      portraitUrl = portraitUrl,
    )
  }

  private fun playTrackUrl(
    target: ActiveAudioTarget.SurahTrack,
    url: String,
    startPositionMs: Long,
    portraitUrl: String?,
  ) {
    withController { controller ->
      val metadataBuilder =
        MediaMetadata.Builder()
          .setTitle("سورة ${target.surah.nameAr}")
          .setArtist("${target.reciter.nameAr} • ${target.riwayah.nameAr}")
          .setAlbumTitle("تلاوة")
      if (!portraitUrl.isNullOrBlank()) {
        metadataBuilder.setArtworkUri(Uri.parse(portraitUrl))
      }

      val mediaItem =
        MediaItem.Builder()
          .setUri(url)
          .setMediaId(target.trackKey)
          .setMediaMetadata(metadataBuilder.build())
          .build()

      controller.setMediaItem(mediaItem)
      if (startPositionMs > 0L) {
        controller.seekTo(startPositionMs)
      }
      controller.prepare()
      controller.play()

      scope.launch {
        preferencesRepository.saveLastPlayed(
          reciterId = target.reciter.id,
          riwayahName = target.riwayah.nameAr,
          surahNumber = target.surah.number,
          positionMs = startPositionMs,
        )
      }
    }
  }

  fun playCairoRadio() {
    _uiState.update {
      it.copy(
        activeTarget = ActiveAudioTarget.CairoRadio,
        isBuffering = true,
        errorMessage = null,
        currentPositionMs = 0L,
        durationMs = 0L,
      )
    }

    withController { controller ->
      val metadata =
        MediaMetadata.Builder()
          .setTitle("إذاعة القرآن الكريم من القاهرة")
          .setArtist("بث مباشر")
          .setAlbumTitle("تلاوة")
          .build()

      val mediaItem =
        MediaItem.Builder()
          .setUri(QuranCatalog.CAIRO_RADIO_STREAM_URL)
          .setMediaId("cairo_radio_live")
          .setMediaMetadata(metadata)
          .build()

      controller.setMediaItem(mediaItem)
      controller.prepare()
      controller.play()
    }
  }

  fun togglePlayPause() {
    withController { controller ->
      if (controller.isPlaying) {
        controller.pause()
      } else {
        if (_uiState.value.activeTarget == ActiveAudioTarget.CairoRadio &&
          controller.playbackState == Player.STATE_IDLE
        ) {
          playCairoRadio()
        } else {
          if (controller.playbackState == Player.STATE_IDLE) {
            controller.prepare()
          }
          controller.play()
        }
      }
    }
  }

  fun stopPlayback() {
    persistCurrentSurahProgress()
    stopPositionTicker()
    cancelSleepTimer()
    withController { controller ->
      controller.stop()
      controller.clearMediaItems()
    }
    _uiState.update {
      it.copy(
        activeTarget = null,
        isPlaying = false,
        isBuffering = false,
        currentPositionMs = 0L,
        bufferedPositionMs = 0L,
        durationMs = 0L,
      )
    }
  }

  fun seekTo(positionMs: Long) {
    if (_uiState.value.activeTarget !is ActiveAudioTarget.SurahTrack) return
    val duration = _uiState.value.durationMs
    val clamped =
      if (duration > 0L) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
    _uiState.update { it.copy(currentPositionMs = clamped) }
    withController { controller ->
      controller.seekTo(clamped)
      persistCurrentSurahProgress()
    }
  }

  fun seekBy(deltaMs: Long) {
    val state = _uiState.value
    if (state.activeTarget !is ActiveAudioTarget.SurahTrack) return
    seekTo(state.currentPositionMs + deltaMs)
  }

  fun seekToFraction(fraction: Float) {
    val state = _uiState.value
    if (state.activeTarget !is ActiveAudioTarget.SurahTrack || state.durationMs <= 0L) return
    val targetMs = (state.durationMs.toFloat() * fraction.coerceIn(0f, 1f)).toLong()
    seekTo(targetMs)
  }

  fun playNextSurah() {
    val current = _uiState.value.activeTarget as? ActiveAudioTarget.SurahTrack ?: return
    val unavailable = _uiState.value.unavailableTrackKeys
    for (nextNum in (current.surah.number + 1)..114) {
      if (!current.riwayah.isSurahListed(nextNum)) continue
      val nextSurah = QuranCatalog.findSurah(nextNum) ?: continue
      val key = "${current.reciter.id}|${current.riwayah.baseUrl}|$nextNum"
      if (!unavailable.contains(key)) {
        playSurah(current.reciter, current.riwayah, nextSurah, portraitUrl = currentPortraitUrl)
        return
      }
    }
  }

  fun playPreviousSurah() {
    val current = _uiState.value.activeTarget as? ActiveAudioTarget.SurahTrack ?: return
    val unavailable = _uiState.value.unavailableTrackKeys
    for (prevNum in (current.surah.number - 1) downTo 1) {
      if (!current.riwayah.isSurahListed(prevNum)) continue
      val prevSurah = QuranCatalog.findSurah(prevNum) ?: continue
      val key = "${current.reciter.id}|${current.riwayah.baseUrl}|$prevNum"
      if (!unavailable.contains(key)) {
        playSurah(current.reciter, current.riwayah, prevSurah, portraitUrl = currentPortraitUrl)
        return
      }
    }
  }

  fun setSleepTimer(option: SleepTimerOption) {
    cancelSleepTimer()
    _uiState.update { it.copy(sleepTimerOption = option, sleepTimerRemainingMs = null) }
    val minutes = option.durationMinutes ?: return
    if (minutes <= 0) {
      // END_OF_SURAH is handled in handlePlaybackEnded()
      return
    }
    val totalMs = minutes * 60_000L
    _uiState.update { it.copy(sleepTimerRemainingMs = totalMs) }
    sleepTimerJob =
      scope.launch {
        var remaining = totalMs
        while (remaining > 0L && isActive) {
          delay(1_000L)
          remaining = (remaining - 1_000L).coerceAtLeast(0L)
          _uiState.update { it.copy(sleepTimerRemainingMs = remaining) }
        }
        if (isActive) {
          mediaController?.pause()
          _uiState.update {
            it.copy(sleepTimerOption = SleepTimerOption.OFF, sleepTimerRemainingMs = null)
          }
        }
      }
  }

  fun clearErrorMessage() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  private fun cancelSleepTimer() {
    sleepTimerJob?.cancel()
    sleepTimerJob = null
  }

  private fun handlePlaybackEnded() {
    val state = _uiState.value
    val target = state.activeTarget
    if (state.sleepTimerOption == SleepTimerOption.END_OF_SURAH) {
      _uiState.update {
        it.copy(sleepTimerOption = SleepTimerOption.OFF, sleepTimerRemainingMs = null)
      }
      return
    }
    if (target is ActiveAudioTarget.SurahTrack && autoPlayNextEnabled) {
      playNextSurah()
    }
  }

  private fun startPositionTicker() {
    positionTickerJob?.cancel()
    positionTickerJob =
      scope.launch {
        while (isActive) {
          val controller = mediaController
          if (controller != null) {
            val pos = controller.currentPosition.coerceAtLeast(0L)
            val buffered = controller.bufferedPosition.coerceAtLeast(0L)
            val dur = controller.duration.coerceAtLeast(0L)
            _uiState.update {
              it.copy(
                currentPositionMs = pos,
                bufferedPositionMs = buffered,
                durationMs = dur,
              )
            }
            val now = System.currentTimeMillis()
            if (now - lastSavedTick >= 5_000L) {
              lastSavedTick = now
              persistCurrentSurahProgress()
            }
          }
          delay(500L)
        }
      }
  }

  private fun stopPositionTicker() {
    positionTickerJob?.cancel()
    positionTickerJob = null
  }

  private fun persistCurrentSurahProgress() {
    val target = _uiState.value.activeTarget as? ActiveAudioTarget.SurahTrack ?: return
    val pos = mediaController?.currentPosition?.coerceAtLeast(0L) ?: _uiState.value.currentPositionMs
    scope.launch {
      preferencesRepository.saveLastPlayed(
        reciterId = target.reciter.id,
        riwayahName = target.riwayah.nameAr,
        surahNumber = target.surah.number,
        positionMs = pos,
      )
    }
  }
}
