package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ActiveAudioTarget
import com.example.model.PlaybackUiState
import com.example.ui.theme.AmiriFontFamily

const val SEEK_STEP_MS = 10_000L

@Composable
fun QuranMedia3PlayerCard(
  playbackState: PlaybackUiState,
  portraits: Map<String, String?>,
  onTogglePlayPause: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onSeekBy: (Long) -> Unit,
  onPreviousSurah: () -> Unit,
  onNextSurah: () -> Unit,
  onExpandPlayer: () -> Unit,
  onStop: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val target = playbackState.activeTarget as? ActiveAudioTarget.SurahTrack ?: return

  Card(
    modifier = modifier.fillMaxWidth().testTag("quran_media3_player_card"),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                  MaterialTheme.colorScheme.surface,
                )
            )
          )
          .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ReciterPortrait(
          portraitUrl = portraits[target.reciter.id],
          contentDescription = target.reciter.nameAr,
          size = 46.dp,
          isCircular = true,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .clickable(onClick = onExpandPlayer),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(50),
            ) {
              Text(
                text =
                  when {
                    playbackState.isBuffering -> "جاري التحميل"
                    playbackState.isPlaying -> "يُتلى الآن"
                    else -> "متوقف مؤقتاً"
                  },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
            Text(
              text = "سورة ${target.surah.nameAr}",
              style = MaterialTheme.typography.titleMedium.copy(fontFamily = AmiriFontFamily),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          ReciterNameWithMic(
            name = "${target.reciter.nameAr} • ${target.riwayah.nameAr}",
            portraitUrl = portraits[target.reciter.id],
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        IconButton(
          onClick = onExpandPlayer,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("inline_player_expand"),
        ) {
          Icon(
            imageVector = Icons.Rounded.OpenInFull,
            contentDescription = "توسيع المشغل",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp),
          )
        }

        IconButton(
          onClick = onStop,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("inline_player_stop"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "إيقاف المشغل",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      QuranSeekBar(
        currentPositionMs = playbackState.currentPositionMs,
        bufferedPositionMs = playbackState.bufferedPositionMs,
        durationMs = playbackState.durationMs,
        isBuffering = playbackState.isBuffering,
        onSeekTo = onSeekTo,
        seekbarTestTag = "inline_player_seekbar",
      )

      QuranPlayerTransportBar(
        isPlaying = playbackState.isPlaying,
        isBuffering = playbackState.isBuffering,
        canSeek = playbackState.durationMs > 0L,
        onTogglePlayPause = onTogglePlayPause,
        onSeekBackward10s = { onSeekBy(-SEEK_STEP_MS) },
        onSeekForward10s = { onSeekBy(SEEK_STEP_MS) },
        onPreviousSurah = onPreviousSurah,
        onNextSurah = onNextSurah,
        compact = true,
      )
    }
  }
}

@Composable
fun QuranSeekBar(
  currentPositionMs: Long,
  bufferedPositionMs: Long,
  durationMs: Long,
  isBuffering: Boolean,
  onSeekTo: (Long) -> Unit,
  seekbarTestTag: String = "player_seekbar",
  modifier: Modifier = Modifier,
) {
  val safeDuration = durationMs.coerceAtLeast(1L)
  val seekEnabled = durationMs > 0L
  var isDragging by remember { mutableStateOf(false) }
  var dragFraction by remember { mutableFloatStateOf(0f) }

  val sliderValue =
    if (isDragging) dragFraction
    else if (seekEnabled) (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    else 0f

  val displayedCurrentMs =
    if (isDragging && seekEnabled) (dragFraction * safeDuration).toLong()
    else currentPositionMs

  Column(modifier = modifier.fillMaxWidth()) {
    Slider(
      value = sliderValue,
      enabled = seekEnabled,
      onValueChange = { value ->
        isDragging = true
        dragFraction = value
      },
      onValueChangeFinished = {
        isDragging = false
        if (seekEnabled) {
          onSeekTo((dragFraction * safeDuration).toLong())
        }
      },
      colors =
        SliderDefaults.colors(
          thumbColor = MaterialTheme.colorScheme.primary,
          activeTrackColor = MaterialTheme.colorScheme.primary,
          inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
          disabledThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
          disabledActiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
          disabledInactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
      modifier = Modifier.fillMaxWidth().testTag(seekbarTestTag),
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = formatMillis(displayedCurrentMs),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (isBuffering) {
        Text(
          text = "جاري التحميل...",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      } else if (bufferedPositionMs > currentPositionMs && durationMs > 0L) {
        val bufferedPct = ((bufferedPositionMs.toFloat() / durationMs.toFloat()) * 100).toInt().coerceIn(0, 100)
        Text(
          text = "محمّل $bufferedPct%",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
      }

      Text(
        text = if (durationMs > 0L) formatMillis(durationMs) else "--:--",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun QuranPlayerTransportBar(
  isPlaying: Boolean,
  isBuffering: Boolean,
  canSeek: Boolean,
  onTogglePlayPause: () -> Unit,
  onSeekBackward10s: () -> Unit,
  onSeekForward10s: () -> Unit,
  onPreviousSurah: () -> Unit,
  onNextSurah: () -> Unit,
  compact: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val playButtonSize = if (compact) 52.dp else 72.dp
  val playIconSize = if (compact) 30.dp else 38.dp
  val secondarySize = if (compact) 48.dp else 52.dp
  val secondaryIconSize = if (compact) 24.dp else 30.dp
  val tagPrefix = if (compact) "inline_" else ""

  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(
      onClick = onPreviousSurah,
      modifier = Modifier.size(secondarySize).testTag("${tagPrefix}player_previous_surah"),
    ) {
      Icon(
        imageVector = Icons.Rounded.SkipPrevious,
        contentDescription = "السورة السابقة",
        tint = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.size(secondaryIconSize),
      )
    }

    IconButton(
      onClick = onSeekBackward10s,
      enabled = canSeek,
      modifier = Modifier.size(secondarySize).testTag("${tagPrefix}player_rewind_10s"),
    ) {
      Icon(
        imageVector = Icons.Rounded.Replay10,
        contentDescription = "ترجيع ١٠ ثوانٍ",
        tint =
          if (canSeek) MaterialTheme.colorScheme.onSurfaceVariant
          else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.size(secondaryIconSize),
      )
    }

    FilledIconButton(
      onClick = onTogglePlayPause,
      modifier = Modifier.size(playButtonSize).testTag("${tagPrefix}player_play_pause"),
      shape = CircleShape,
      colors =
        IconButtonDefaults.filledIconButtonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
      if (isBuffering) {
        CircularProgressIndicator(
          modifier = Modifier.size(if (compact) 22.dp else 28.dp),
          strokeWidth = 3.dp,
          color = MaterialTheme.colorScheme.onPrimary,
        )
      } else {
        Icon(
          imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
          contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
          modifier = Modifier.size(playIconSize),
        )
      }
    }

    IconButton(
      onClick = onSeekForward10s,
      enabled = canSeek,
      modifier = Modifier.size(secondarySize).testTag("${tagPrefix}player_forward_10s"),
    ) {
      Icon(
        imageVector = Icons.Rounded.Forward10,
        contentDescription = "تقديم ١٠ ثوانٍ",
        tint =
          if (canSeek) MaterialTheme.colorScheme.onSurfaceVariant
          else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.size(secondaryIconSize),
      )
    }

    IconButton(
      onClick = onNextSurah,
      modifier = Modifier.size(secondarySize).testTag("${tagPrefix}player_next_surah"),
    ) {
      Icon(
        imageVector = Icons.Rounded.SkipNext,
        contentDescription = "السورة التالية",
        tint = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.size(secondaryIconSize),
      )
    }
  }
}
