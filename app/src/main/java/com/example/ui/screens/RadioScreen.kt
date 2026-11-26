package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ActiveAudioTarget
import com.example.model.PlaybackUiState
import com.example.ui.theme.AmiriFontFamily

@Composable
fun RadioScreen(
  playbackState: PlaybackUiState,
  onToggleRadio: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isRadioSelected = playbackState.activeTarget == ActiveAudioTarget.CairoRadio
  val isLivePlaying = isRadioSelected && playbackState.isPlaying
  val isBuffering = isRadioSelected && playbackState.isBuffering

  val infiniteTransition = rememberInfiniteTransition(label = "radio_pulse")
  val pulseScale by
    infiniteTransition.animateFloat(
      initialValue = 1.0f,
      targetValue = 1.42f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "pulse_scale",
    )
  val pulseAlpha by
    infiniteTransition.animateFloat(
      initialValue = 0.55f,
      targetValue = 0f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "pulse_alpha",
    )

  val dotAlpha by
    infiniteTransition.animateFloat(
      initialValue = 1f,
      targetValue = 0.25f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 750),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "dot_alpha",
    )

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .widthIn(max = 720.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("cairo_radio_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
          Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
            Image(
              painter = painterResource(id = R.drawable.img_radio_studio_1795533465794),
              contentDescription = "إذاعة القرآن الكريم من القاهرة",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      colors =
                        listOf(
                          Color(0x331E2A25),
                          MaterialTheme.colorScheme.surface,
                        )
                    )
                  )
            )

            // Live status pill
            Surface(
              color =
                if (isLivePlaying) Color(0xE62E6F5E)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
              shape = RoundedCornerShape(50),
              border =
                BorderStroke(
                  1.dp,
                  if (isLivePlaying) Color(0xFFB8924A)
                  else MaterialTheme.colorScheme.outline,
                ),
              modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(
                  modifier =
                    Modifier.size(10.dp)
                      .alpha(if (isLivePlaying) dotAlpha else 1f)
                      .clip(CircleShape)
                      .background(
                        when {
                          isLivePlaying -> Color(0xFF9DF0C0)
                          isBuffering -> MaterialTheme.colorScheme.secondary
                          else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                      )
                )
                Text(
                  text =
                    when {
                      isBuffering -> "جاري الاتصال بالبث..."
                      isLivePlaying -> "بث مباشر الآن"
                      else -> "البث متوقف"
                    },
                  style = MaterialTheme.typography.labelMedium,
                  color =
                    if (isLivePlaying) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          // Pulsing Radio Emblem
          Box(
            modifier = Modifier.padding(top = 8.dp).size(132.dp),
            contentAlignment = Alignment.Center,
          ) {
            if (isLivePlaying) {
              Box(
                modifier =
                  Modifier.size(96.dp)
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
              )
              Box(
                modifier =
                  Modifier.size(96.dp)
                    .scale(1f + (pulseScale - 1f) * 0.55f)
                    .alpha(pulseAlpha * 0.7f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
              )
            }

            Surface(
              modifier = Modifier.size(92.dp),
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primaryContainer,
              border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            ) {
              Box(contentAlignment = Alignment.Center) {
                if (isBuffering) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                  )
                } else {
                  Icon(
                    imageVector = Icons.Rounded.Radio,
                    contentDescription = "أيقونة الإذاعة",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                  )
                }
              }
            }
          }

          val hasRadioError = isRadioSelected && !isLivePlaying && !isBuffering && playbackState.errorMessage != null

          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "إذاعة القرآن الكريم من القاهرة",
              style = MaterialTheme.typography.headlineMedium.copy(fontFamily = AmiriFontFamily),
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center,
            )
            Text(
              text =
                if (hasRadioError) {
                  "تعذر الوصول إلى الخادم الحالي — اضغط لإعادة المحاولة عبر الخوادم البديلة"
                } else {
                  "بث إذاعي متواصل لتلاوات كبار القراء والبرامج الدينية من القاهرة على مدار الساعة"
                },
              style = MaterialTheme.typography.bodyMedium,
              color =
                if (hasRadioError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Button(
            onClick = onToggleRadio,
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor =
                  if (isLivePlaying || isBuffering) MaterialTheme.colorScheme.error
                  else MaterialTheme.colorScheme.primary,
                contentColor =
                  if (isLivePlaying || isBuffering) MaterialTheme.colorScheme.onError
                  else MaterialTheme.colorScheme.onPrimary,
              ),
            modifier = Modifier.padding(bottom = 24.dp).testTag("radio_play_stop_button"),
          ) {
            Icon(
              imageVector =
                if (isLivePlaying || isBuffering) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text =
                when {
                  isLivePlaying || isBuffering -> "إيقاف البث"
                  hasRadioError -> "إعادة المحاولة"
                  else -> "تشغيل البث المباشر"
                },
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}
