package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val CairoFontFamily = FontFamily(
  Font(R.font.cairo, FontWeight.Normal),
  Font(R.font.cairo, FontWeight.Medium),
  Font(R.font.cairo, FontWeight.SemiBold),
  Font(R.font.cairo, FontWeight.Bold),
)

val AmiriFontFamily = FontFamily(
  Font(R.font.amiri, FontWeight.Normal),
  Font(R.font.amiri, FontWeight.Bold),
)

fun createAppTypography(scaleFactor: Float): Typography =
  Typography(
    displaySmall =
      TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (32f * scaleFactor).sp,
        lineHeight = (44f * scaleFactor).sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (26f * scaleFactor).sp,
        lineHeight = (36f * scaleFactor).sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (21f * scaleFactor).sp,
        lineHeight = (30f * scaleFactor).sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (19f * scaleFactor).sp,
        lineHeight = (27f * scaleFactor).sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (16f * scaleFactor).sp,
        lineHeight = (24f * scaleFactor).sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (14f * scaleFactor).sp,
        lineHeight = (20f * scaleFactor).sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (16f * scaleFactor).sp,
        lineHeight = (25f * scaleFactor).sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (14f * scaleFactor).sp,
        lineHeight = (22f * scaleFactor).sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (12f * scaleFactor).sp,
        lineHeight = (18f * scaleFactor).sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (14f * scaleFactor).sp,
        lineHeight = (20f * scaleFactor).sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (12f * scaleFactor).sp,
        lineHeight = (16f * scaleFactor).sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (11f * scaleFactor).sp,
        lineHeight = (15f * scaleFactor).sp,
      ),
  )
