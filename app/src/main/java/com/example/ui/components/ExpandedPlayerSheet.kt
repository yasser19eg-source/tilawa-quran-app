package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.ActiveAudioTarget
import com.example.model.PlaybackUiState
import com.example.model.SleepTimerOption
import com.example.ui.theme.AmiriFontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpandedPlayerSheet(
  playbackState: PlaybackUiState,
  portraits: Map<String, String?>,
  autoPlayNext: Boolean,
  onDismiss: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onStop: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onSeekBy: (Long) -> Unit,
  onNextSurah: () -> Unit,
  onPreviousSurah: () -> Unit,
  onToggleAutoPlayNext: (Boolean) -> Unit,
  onSelectSleepTimer: (SleepTimerOption) -> Unit,
) {
  val target = playbackState.activeTarget ?: return
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var showSleepTimerOptions by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.background,
    dragHandle = null,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                  MaterialTheme.colorScheme.background,
                )
            )
          )
          .padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("collapse_player_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "تصغير المشغل",
            tint = MaterialTheme.colorScheme.onBackground,
          )
        }

        Text(
          text = if (target is ActiveAudioTarget.CairoRadio) "البث المباشر" else "مشغل التلاوة",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        IconButton(
          onClick = onStop,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("expanded_player_stop"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Stop,
            contentDescription = "إيقاف التشغيل",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      when (target) {
        is ActiveAudioTarget.SurahTrack -> {
          ReciterPortrait(
            portraitUrl = portraits[target.reciter.id],
            contentDescription = target.reciter.nameAr,
            size = 180.dp,
            isCircular = true,
          )
          Spacer(modifier = Modifier.height(24.dp))
          Text(
            text = "سورة ${target.surah.nameAr}",
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = AmiriFontFamily),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(6.dp))
          ReciterNameWithMic(
            name = target.reciter.nameAr,
            portraitUrl = portraits[target.reciter.id],
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            centered = true,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${target.riwayah.nameAr} • ${target.surah.revelationTypeAr} • ${target.surah.ayahCount} آية",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(20.dp))

          QuranSeekBar(
            currentPositionMs = playbackState.currentPositionMs,
            bufferedPositionMs = playbackState.bufferedPositionMs,
            durationMs = playbackState.durationMs,
            isBuffering = playbackState.isBuffering,
            onSeekTo = onSeekTo,
            seekbarTestTag = "player_seekbar",
          )

          Spacer(modifier = Modifier.height(16.dp))

          QuranPlayerTransportBar(
            isPlaying = playbackState.isPlaying,
            isBuffering = playbackState.isBuffering,
            canSeek = playbackState.durationMs > 0L,
            onTogglePlayPause = onTogglePlayPause,
            onSeekBackward10s = { onSeekBy(-SEEK_STEP_MS) },
            onSeekForward10s = { onSeekBy(SEEK_STEP_MS) },
            onPreviousSurah = onPreviousSurah,
            onNextSurah = onNextSurah,
            compact = false,
          )
        }

        ActiveAudioTarget.CairoRadio -> {
          Box(
            modifier =
              Modifier.size(160.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Radio,
              contentDescription = "إذاعة القرآن الكريم من القاهرة",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(68.dp),
            )
          }
          Spacer(modifier = Modifier.height(24.dp))
          Text(
            text = "إذاعة القرآن الكريم من القاهرة",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = AmiriFontFamily),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = if (playbackState.isBuffering) "جاري الاتصال بالبث..." else "بث حي متواصل من القاهرة",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.height(28.dp))
          FilledIconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier.size(72.dp).testTag("expanded_radio_play_pause"),
            shape = CircleShape,
            colors =
              IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
          ) {
            if (playbackState.isBuffering) {
              CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            } else {
              Icon(
                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playbackState.isPlaying) "إيقاف" else "تشغيل",
                modifier = Modifier.size(38.dp),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Auto-Play & Sleep Timer Action Chips
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
      ) {
        if (target is ActiveAudioTarget.SurahTrack) {
          FilterChip(
            selected = autoPlayNext,
            onClick = { onToggleAutoPlayNext(!autoPlayNext) },
            label = { Text("تشغيل تلقائي للتالي") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            modifier = Modifier.testTag("autoplay_toggle_chip"),
          )
        }

        val timerActive = playbackState.sleepTimerOption != SleepTimerOption.OFF
        val timerLabel =
          when {
            playbackState.sleepTimerRemainingMs != null ->
              "مؤقت: ${formatMillis(playbackState.sleepTimerRemainingMs)}"
            timerActive -> playbackState.sleepTimerOption.labelAr
            else -> "مؤقت النوم"
          }

        FilterChip(
          selected = timerActive,
          onClick = { showSleepTimerOptions = !showSleepTimerOptions },
          label = { Text(timerLabel) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.Bedtime,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
          },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          modifier = Modifier.testTag("sleep_timer_chip"),
        )
      }

      if (showSleepTimerOptions) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth(),
        ) {
          FlowRow(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            SleepTimerOption.entries.forEach { option ->
              val isAllowed =
                option != SleepTimerOption.END_OF_SURAH || target is ActiveAudioTarget.SurahTrack
              if (isAllowed) {
                FilterChip(
                  selected = playbackState.sleepTimerOption == option,
                  onClick = {
                    onSelectSleepTimer(option)
                    showSleepTimerOptions = false
                  },
                  label = { Text(option.labelAr) },
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
