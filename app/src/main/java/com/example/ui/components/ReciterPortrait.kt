package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

@Composable
fun ReciterPortrait(
  portraitUrl: String?,
  contentDescription: String,
  size: Dp,
  isCircular: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val shape: Shape = if (isCircular) CircleShape else RoundedCornerShape(18.dp)
  val olivePrimary = MaterialTheme.colorScheme.primary
  val auraColor = olivePrimary.copy(alpha = 0.28f)
  val surfaceContainer = MaterialTheme.colorScheme.surfaceVariant
  val bwMatrix = ColorMatrix().apply { setToSaturation(0f) }

  Box(
    modifier =
      modifier
        .size(size)
        .drawBehind {
          val glowBrush =
            Brush.radialGradient(
              colors = listOf(auraColor, Color.Transparent),
              center = center,
              radius = this.size.maxDimension * 0.64f,
            )
          drawCircle(brush = glowBrush, radius = this.size.maxDimension * 0.62f)
        }
        .clip(shape)
        .background(surfaceContainer)
        .border(BorderStroke(1.5.dp, olivePrimary.copy(alpha = 0.65f)), shape),
    contentAlignment = Alignment.Center,
  ) {
    if (portraitUrl.isNullOrBlank()) {
      StudioMicFallback(size = size)
    } else {
      SubcomposeAsyncImage(
        model =
          ImageRequest.Builder(LocalContext.current)
            .data(portraitUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        colorFilter = ColorFilter.colorMatrix(bwMatrix),
        modifier = Modifier.fillMaxSize().clip(shape),
        loading = { StudioMicFallback(size = size) },
        error = { StudioMicFallback(size = size) },
      )
      // Subtle olive atmospheric tint overlay over monochrome portrait
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors =
                  listOf(
                    Color.Transparent,
                    olivePrimary.copy(alpha = 0.16f),
                  )
              )
            )
      )
    }
  }
}

@Composable
private fun StudioMicFallback(size: Dp) {
  val olivePrimary = MaterialTheme.colorScheme.primary
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.radialGradient(
            colors =
              listOf(
                olivePrimary.copy(alpha = 0.22f),
                MaterialTheme.colorScheme.surfaceVariant,
              )
          )
        ),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = Icons.Rounded.Mic,
      contentDescription = null,
      tint = olivePrimary,
      modifier = Modifier.size((size * 0.42f).coerceIn(20.dp, 54.dp)),
    )
  }
}

@Composable
fun ReciterNameWithMic(
  name: String,
  modifier: Modifier = Modifier,
  portraitUrl: String? = null,
  style: TextStyle = MaterialTheme.typography.titleSmall,
  color: Color = MaterialTheme.colorScheme.onSurface,
  maxLines: Int = 1,
  centered: Boolean = false,
) {
  val olivePrimary = MaterialTheme.colorScheme.primary
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement =
      if (centered) Arrangement.Center else Arrangement.Start,
  ) {
    if (!portraitUrl.isNullOrBlank()) {
      SubcomposeAsyncImage(
        model =
          ImageRequest.Builder(LocalContext.current)
            .data(portraitUrl)
            .crossfade(true)
            .build(),
        contentDescription = name,
        contentScale = ContentScale.Crop,
        modifier =
          Modifier.padding(end = 6.dp)
            .size(24.dp)
            .clip(CircleShape)
            .border(BorderStroke(1.dp, olivePrimary.copy(alpha = 0.75f)), CircleShape),
        loading = {
          Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = "قارئ",
            tint = olivePrimary,
            modifier = Modifier.size(16.dp),
          )
        },
        error = {
          Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = "قارئ",
            tint = olivePrimary,
            modifier = Modifier.size(16.dp),
          )
        },
      )
    }
    Icon(
      imageVector = Icons.Rounded.Mic,
      contentDescription = "قارئ",
      tint = olivePrimary,
      modifier = Modifier.padding(end = 4.dp).size(15.dp),
    )
    Text(
      text = name,
      style = style,
      color = color,
      maxLines = maxLines,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

fun formatMillis(ms: Long): String {
  val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
  val hours = totalSeconds / 3600L
  val minutes = (totalSeconds % 3600L) / 60L
  val seconds = totalSeconds % 60L
  return if (hours > 0L) {
    String.format("%d:%02d:%02d", hours, minutes, seconds)
  } else {
    String.format("%02d:%02d", minutes, seconds)
  }
}
