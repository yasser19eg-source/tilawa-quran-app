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
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ActiveAudioTarget
import com.example.model.PlaybackUiState
import com.example.ui.theme.AmiriFontFamily

@Composable
fun MiniPlayerBar(
  playbackState: PlaybackUiState,
  portraits: Map<String, String?>,
  onExpand: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onStop: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val target = playbackState.activeTarget ?: return

  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp)
        .testTag("mini_player_bar")
        .clickable(onClick = onExpand),
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    shadowElevation = 8.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      when {
        playbackState.isBuffering -> {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(2.5.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
          )
        }
        target is ActiveAudioTarget.SurahTrack && playbackState.durationMs > 0L -> {
          val progress =
            (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat())
              .coerceIn(0f, 1f)
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(2.5.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
          )
        }
        target is ActiveAudioTarget.CairoRadio && playbackState.isPlaying -> {
          Box(
            modifier =
              Modifier.fillMaxWidth().height(2.5.dp).background(MaterialTheme.colorScheme.primary)
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        when (target) {
          is ActiveAudioTarget.SurahTrack -> {
            ReciterPortrait(
              portraitUrl = portraits[target.reciter.id],
              contentDescription = target.reciter.nameAr,
              size = 44.dp,
              isCircular = true,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = "سورة ${target.surah.nameAr}",
                  style = MaterialTheme.typography.titleMedium.copy(fontFamily = AmiriFontFamily),
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f, fill = false),
                )
                if (playbackState.durationMs > 0L) {
                  Text(
                    text = "${formatMillis(playbackState.currentPositionMs)} / ${formatMillis(playbackState.durationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                  )
                }
              }
              ReciterNameWithMic(
                name = "${target.reciter.nameAr} • ${target.riwayah.nameAr}",
                portraitUrl = portraits[target.reciter.id],
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          ActiveAudioTarget.CairoRadio -> {
            Box(
              modifier =
                Modifier.size(44.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.Radio,
                contentDescription = "إذاعة القرآن الكريم",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
              Text(
                text = "إذاعة القرآن الكريم من القاهرة",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = if (playbackState.isBuffering) "جاري الاتصال بالبث..." else "بث مباشر",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
              )
            }
          }
        }

        IconButton(
          onClick = onTogglePlayPause,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("mini_player_play_pause"),
        ) {
          if (playbackState.isBuffering) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              strokeWidth = 2.5.dp,
              color = MaterialTheme.colorScheme.primary,
            )
          } else {
            Icon(
              imageVector =
                if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
              contentDescription = if (playbackState.isPlaying) "إيقاف مؤقت" else "تشغيل",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp),
            )
          }
        }

        IconButton(
          onClick = onStop,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("mini_player_stop"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "إغلاق المشغل",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}
