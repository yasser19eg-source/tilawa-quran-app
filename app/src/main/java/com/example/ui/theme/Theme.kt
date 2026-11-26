package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SanctuaryDarkColorScheme =
  darkColorScheme(
    primary = OliveAccent,
    onPrimary = DarkBackground,
    primaryContainer = OliveDeepContainer,
    onPrimaryContainer = OliveAccentSoft,
    secondary = WarmGoldDark,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    error = SoftCoralError,
    onError = DarkBackground,
  )

private val SanctuaryLightColorScheme =
  lightColorScheme(
    primary = LightOlivePrimary,
    onPrimary = Color.White,
    primaryContainer = LightOliveContainer,
    onPrimaryContainer = LightOliveOnContainer,
    secondary = WarmGoldMuted,
    onSecondary = LightTextPrimary,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightTextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    error = LightError,
    onError = Color.White,
  )

private val AppShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun MushafAlTilawahTheme(
  darkTheme: Boolean = false,
  fontScaleFactor: Float = 1.0f,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) SanctuaryDarkColorScheme else SanctuaryLightColorScheme
  val typography = remember(fontScaleFactor) { createAppTypography(fontScaleFactor) }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = typography,
    shapes = AppShapes,
    content = content,
  )
}
