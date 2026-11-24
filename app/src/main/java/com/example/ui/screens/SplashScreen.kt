package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.QuranCatalog
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.WarmGoldMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
  onSplashFinished: () -> Unit,
  modifier: Modifier = Modifier,
  autoAdvanceDelayMs: Long = 2_450L,
) {
  val emblemScale = remember { Animatable(0.80f) }
  val emblemAlpha = remember { Animatable(0.20f) }
  val geometryReveal = remember { Animatable(0.15f) }
  val titleProgress = remember { Animatable(0f) }
  val ornamentProgress = remember { Animatable(0f) }
  val subtitleAlpha = remember { Animatable(0f) }
  val dedicationAlpha = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    launch {
      emblemAlpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
      )
    }
    launch {
      emblemScale.animateTo(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = Spring.StiffnessLow),
      )
    }
    launch {
      geometryReveal.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
      )
    }
    launch {
      titleProgress.animateTo(
        targetValue = 1f,
        animationSpec =
          tween(durationMillis = 720, delayMillis = 260, easing = FastOutSlowInEasing),
      )
    }
    launch {
      ornamentProgress.animateTo(
        targetValue = 1f,
        animationSpec =
          tween(durationMillis = 650, delayMillis = 540, easing = FastOutSlowInEasing),
      )
    }
    launch {
      subtitleAlpha.animateTo(
        targetValue = 1f,
        animationSpec =
          tween(durationMillis = 550, delayMillis = 740, easing = FastOutSlowInEasing),
      )
    }
    launch {
      dedicationAlpha.animateTo(
        targetValue = 1f,
        animationSpec =
          tween(durationMillis = 550, delayMillis = 980, easing = FastOutSlowInEasing),
      )
    }
    delay(autoAdvanceDelayMs)
    onSplashFinished()
  }

  val infiniteTransition = rememberInfiniteTransition(label = "splash_ambient")
  val orbitalRotation by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 28_000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "orbital_rotation",
    )
  val haloPulse by
    infiniteTransition.animateFloat(
      initialValue = 0.94f,
      targetValue = 1.08f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 2_200, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "halo_pulse",
    )
  val shimmerShift by
    infiniteTransition.animateFloat(
      initialValue = -0.4f,
      targetValue = 1.4f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 2_000, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "shimmer_shift",
    )

  val backgroundColor = MaterialTheme.colorScheme.background
  val surfaceColor = MaterialTheme.colorScheme.surface
  val primaryColor = MaterialTheme.colorScheme.primary
  val onBackground = MaterialTheme.colorScheme.onBackground
  val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(backgroundColor, surfaceColor, backgroundColor)
          )
        )
        .pointerInput(onSplashFinished) {
          detectTapGestures(onTap = { onSplashFinished() })
        }
        .testTag("splash_screen")
  ) {
    // Atmospheric radial glow & slowly rotating Islamic Rub el Hizb star geometry
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height * 0.41f)
      val minDim = size.minDimension

      // Ambient sage & warm gold aura
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                primaryColor.copy(alpha = 0.20f * geometryReveal.value),
                WarmGoldMuted.copy(alpha = 0.08f * geometryReveal.value),
                Color.Transparent,
              ),
            center = center,
            radius = minDim * 0.62f * haloPulse,
          ),
        center = center,
        radius = minDim * 0.62f * haloPulse,
      )

      // Outer faint concentric ring
      drawCircle(
        color = primaryColor.copy(alpha = 0.14f * geometryReveal.value),
        radius = minDim * 0.36f * geometryReveal.value,
        center = center,
        style = Stroke(width = 1.2.dp.toPx()),
      )

      // Delicate rotating 8-pointed Rub el Hizb geometric mandala
      val starSide = minDim * 0.44f * geometryReveal.value
      if (starSide > 4f) {
        val strokeWidth = 1.1.dp.toPx()
        val starColor = WarmGoldMuted.copy(alpha = 0.24f * geometryReveal.value)
        val topLeft = Offset(center.x - starSide / 2f, center.y - starSide / 2f)
        val rectSize = Size(starSide, starSide)

        rotate(degrees = orbitalRotation, pivot = center) {
          drawRect(
            color = starColor,
            topLeft = topLeft,
            size = rectSize,
            style = Stroke(width = strokeWidth),
          )
        }
        rotate(degrees = orbitalRotation + 45f, pivot = center) {
          drawRect(
            color = primaryColor.copy(alpha = 0.22f * geometryReveal.value),
            topLeft = topLeft,
            size = rectSize,
            style = Stroke(width = strokeWidth),
          )
        }
      }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(
      modifier =
        Modifier.fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
    ) {
      val isCompactHeight = maxHeight < 560.dp
      val medallionOuterSize = if (isCompactHeight) 124.dp else 168.dp
      val medallionInnerSize = if (isCompactHeight) 108.dp else 146.dp
      val titleFontSize = if (isCompactHeight) 38.sp else 48.sp
      val titleLineHeight = if (isCompactHeight) 52.sp else 64.sp

      Column(
        modifier =
          Modifier.fillMaxSize()
            .padding(horizontal = 28.dp, vertical = if (isCompactHeight) 14.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(modifier = Modifier.weight(1f))

        // Medallion Emblem with animated shimmering gold-sage ring & calligraphic artwork
        Box(
          modifier =
            Modifier.testTag("splash_emblem")
              .size(medallionOuterSize)
              .scale(emblemScale.value)
              .alpha(emblemAlpha.value),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f - 4.dp.toPx()
            drawCircle(
              brush =
                Brush.sweepGradient(
                  colors =
                    listOf(
                      primaryColor.copy(alpha = 0.35f),
                      WarmGoldMuted.copy(alpha = 0.90f),
                      primaryColor.copy(alpha = 0.85f),
                      WarmGoldMuted.copy(alpha = 0.35f),
                      primaryColor.copy(alpha = 0.35f),
                    )
                ),
              radius = radius,
              style = Stroke(width = 2.2.dp.toPx()),
            )
          }

          Box(
            modifier =
              Modifier.size(medallionInnerSize)
                .clip(CircleShape)
                .background(Color(0xFF15191C)),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              painter = painterResource(id = R.drawable.ic_launcher_art_tilawah),
              contentDescription = "شعار تلاوة",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().scale(1.26f),
            )
          }
        }

        Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else 26.dp))

        // Animated App Name "تلاوة" with upward glide & shimmering brush sweep
        val shimmerBrush =
          Brush.linearGradient(
            colors = listOf(onBackground, primaryColor, WarmGoldMuted, onBackground),
            start = Offset(x = (shimmerShift - 0.35f) * 380f, y = 0f),
            end = Offset(x = (shimmerShift + 0.35f) * 380f, y = 120f),
          )

        Text(
          text = "تلاوة",
          style =
            MaterialTheme.typography.displaySmall.copy(
              fontFamily = AmiriFontFamily,
              fontSize = titleFontSize,
              lineHeight = titleLineHeight,
              fontWeight = FontWeight.Bold,
              brush = shimmerBrush,
            ),
          textAlign = TextAlign.Center,
          modifier =
            Modifier.testTag("splash_title_text").graphicsLayer {
              alpha = titleProgress.value
              translationY = (1f - titleProgress.value) * 26.dp.toPx()
              val scaleFactor = 0.94f + (0.06f * titleProgress.value)
              scaleX = scaleFactor
              scaleY = scaleFactor
            },
        )

      Spacer(modifier = Modifier.height(10.dp))

      // Symmetrical expanding Islamic ornamental divider with center diamond
      Canvas(
        modifier =
          Modifier.fillMaxWidth(0.56f)
            .height(14.dp)
            .alpha(ornamentProgress.value)
      ) {
        val midX = size.width / 2f
        val midY = size.height / 2f
        val halfSpan = (size.width / 2f - 10.dp.toPx()) * ornamentProgress.value
        val diamondR = 4.5.dp.toPx() * ornamentProgress.value

        if (halfSpan > 6f) {
          drawLine(
            brush =
              Brush.horizontalGradient(
                colors = listOf(Color.Transparent, WarmGoldMuted, primaryColor),
                startX = midX - halfSpan,
                endX = midX - diamondR * 1.8f,
              ),
            start = Offset(midX - halfSpan, midY),
            end = Offset(midX - diamondR * 1.8f, midY),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
          )
          drawLine(
            brush =
              Brush.horizontalGradient(
                colors = listOf(primaryColor, WarmGoldMuted, Color.Transparent),
                startX = midX + diamondR * 1.8f,
                endX = midX + halfSpan,
              ),
            start = Offset(midX + diamondR * 1.8f, midY),
            end = Offset(midX + halfSpan, midY),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
          )
        }

        if (diamondR > 0.5f) {
          val path =
            Path().apply {
              moveTo(midX, midY - diamondR)
              lineTo(midX + diamondR, midY)
              lineTo(midX, midY + diamondR)
              lineTo(midX - diamondR, midY)
              close()
            }
          drawPath(path = path, color = WarmGoldMuted)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "وَرَتِّلِ الْقُرْآنَ تَرْتِيلًا",
        style =
          MaterialTheme.typography.titleLarge.copy(
            fontFamily = AmiriFontFamily,
            fontWeight = FontWeight.Normal,
          ),
        color = primaryColor,
        textAlign = TextAlign.Center,
        modifier =
          Modifier.graphicsLayer {
            alpha = subtitleAlpha.value
            translationY = (1f - subtitleAlpha.value) * 12.dp.toPx()
          },
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "المصحف الصوتي وإذاعة القرآن الكريم من القاهرة",
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = CairoFontFamily),
        color = secondaryText,
        textAlign = TextAlign.Center,
        modifier = Modifier.alpha(subtitleAlpha.value),
      )

      Spacer(modifier = Modifier.weight(1.1f))

      // Subtle Sadaqah Jariyah footer & progress dots
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.alpha(dedicationAlpha.value),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          repeat(3) { idx ->
            val phase = ((shimmerShift + idx * 0.25f) % 1f).coerceIn(0f, 1f)
            val dotAlpha = 0.3f + 0.7f * (1f - kotlin.math.abs(phase - 0.5f) * 2f)
            Box(
              modifier =
                Modifier.size(6.dp)
                  .clip(CircleShape)
                  .background(primaryColor.copy(alpha = dotAlpha))
            )
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "صدقة جارية عن ${QuranCatalog.DEDICATED_PERSON_NAME}",
            style = MaterialTheme.typography.labelMedium,
            color = secondaryText.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
          )
        }
      }
      }
    }
  }
}
