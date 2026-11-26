package com.example.ui.components

import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import android.text.style.ReplacementSpan
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.MushafPrintedInkDark
import com.example.ui.theme.MushafPrintedInkLight
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val ROSETTE_FILL_LIGHT = Color(0xFFE1EEE9)
private val ROSETTE_FILL_DARK = Color(0xFF25483E)
private val ROSETTE_RING_LIGHT = Color(0xFF2E6F5E)
private val ROSETTE_RING_DARK = Color(0xFF6FB09D)
private val ROSETTE_PETAL_LIGHT = Color(0xFFB8924A)
private val ROSETTE_PETAL_DARK = Color(0xFFD4B270)
private val ROSETTE_HIGHLIGHT_FILL = Color(0xFFF4ECDA)

/**
 * Compact ornamental Medina Mushaf Ayah-end rosette medallion with generous horizontal padding
 * so it sits cleanly between Ayahs without ever touching preceding letters or diacritics.
 */
@Composable
fun MushafAyahRosetteBadge(
  ayahNumber: Int,
  fontScale: Float,
  isHighlighted: Boolean = false,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val digits = remember(ayahNumber) { AyahShareCardGenerator.toArabicIndicDigits(ayahNumber) }
  val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

  val fillColor =
    when {
      isHighlighted -> ROSETTE_HIGHLIGHT_FILL.copy(alpha = if (isDark) 0.38f else 0.72f)
      isDark -> ROSETTE_FILL_DARK
      else -> ROSETTE_FILL_LIGHT
    }
  val ringColor = if (isDark) ROSETTE_RING_DARK else ROSETTE_RING_LIGHT
  val petalColor = if (isDark) ROSETTE_PETAL_DARK else ROSETTE_PETAL_LIGHT
  val textColor = if (isDark) MushafPrintedInkDark else MushafPrintedInkLight

  val clickModifier =
    if (onClick != null) {
      val interactionSource = remember { MutableInteractionSource() }
      Modifier.clickable(
        interactionSource = interactionSource,
        indication = null,
        role = Role.Button,
        onClick = onClick,
      )
    } else {
      Modifier
    }

  Box(
    modifier = modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 1.dp).then(clickModifier),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val diameter = min(size.width, size.height)
      val cx = size.width / 2f
      val cy = size.height / 2f
      val outerRadius = diameter * 0.39f
      val innerRadius = diameter * 0.30f

      // 8 delicate outer scalloped petals kept safely inside padded bounds
      for (i in 0 until 8) {
        val angleRad = (i * 45.0) * Math.PI / 180.0
        val px = cx + (outerRadius * 0.88f * cos(angleRad)).toFloat()
        val py = cy + (outerRadius * 0.88f * sin(angleRad)).toFloat()
        val isCardinal = i % 2 == 0
        drawCircle(
          color = ringColor.copy(alpha = 0.78f),
          radius = outerRadius * (if (isCardinal) 0.24f else 0.17f),
          center = Offset(px, py),
        )
        drawCircle(
          color = if (isCardinal) petalColor else fillColor,
          radius = outerRadius * (if (isCardinal) 0.16f else 0.10f),
          center = Offset(px, py),
        )
      }

      drawCircle(
        color = fillColor,
        radius = outerRadius * 0.86f,
        center = Offset(cx, cy),
      )
      drawCircle(
        color = ringColor,
        radius = outerRadius * 0.86f,
        center = Offset(cx, cy),
        style = Stroke(width = (diameter * 0.05f).coerceAtLeast(1.0f)),
      )
      drawCircle(
        color = ringColor.copy(alpha = 0.52f),
        radius = innerRadius * 0.82f,
        center = Offset(cx, cy),
        style = Stroke(width = (diameter * 0.028f).coerceAtLeast(0.8f)),
      )
    }

    val digitFontSize =
      when {
        digits.length >= 3 -> (8.0f * fontScale).sp
        digits.length == 2 -> (9.0f * fontScale).sp
        else -> (9.8f * fontScale).sp
      }

    Text(
      text = digits,
      style =
        MaterialTheme.typography.labelSmall.copy(
          fontFamily = AmiriFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = digitFontSize,
          lineHeight = (digitFontSize.value + 1f).sp,
        ),
      color = textColor,
      textAlign = TextAlign.Center,
      maxLines = 1,
    )
  }
}

/**
 * Inline Android Canvas [ReplacementSpan] rendering the Medina Mushaf Ayah rosette inside StaticLayout
 * with generous symmetric horizontal padding so it never touches adjacent Ayah letters or diacritics.
 */
class MushafAyahRosetteSpan(
  ayahNumber: Int,
  private val useDarkTheme: Boolean,
  private val digitTypeface: Typeface,
) : ReplacementSpan() {

  private val digits = AyahShareCardGenerator.toArabicIndicDigits(ayahNumber)

  override fun getSize(
    paint: AndroidPaint,
    text: CharSequence?,
    start: Int,
    end: Int,
    fm: AndroidPaint.FontMetricsInt?,
  ): Int {
    val diameter = paint.textSize * 0.68f
    val horizontalPad = paint.textSize * 0.28f
    return ceil((diameter + horizontalPad * 2f).toDouble()).toInt()
  }

  override fun draw(
    canvas: AndroidCanvas,
    text: CharSequence?,
    start: Int,
    end: Int,
    x: Float,
    top: Int,
    y: Int,
    bottom: Int,
    paint: AndroidPaint,
  ) {
    val diameter = paint.textSize * 0.68f
    val horizontalPad = paint.textSize * 0.28f
    val cx = x + horizontalPad + diameter / 2f
    val cy = y - paint.textSize * 0.25f

    val outerRadius = diameter * 0.40f
    val innerRadius = diameter * 0.30f

    val fillInt = if (useDarkTheme) 0xFF25483E.toInt() else 0xFFE1EEE9.toInt()
    val ringInt = if (useDarkTheme) 0xFF6FB09D.toInt() else 0xFF2E6F5E.toInt()
    val petalInt = if (useDarkTheme) 0xFFD4B270.toInt() else 0xFFB8924A.toInt()
    val digitColor =
      if (useDarkTheme) MushafPrintedInkDark.toArgb() else MushafPrintedInkLight.toArgb()

    val fillPaint =
      AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = fillInt
        style = AndroidPaint.Style.FILL
      }
    val ringFillPaint =
      AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = ringInt
        alpha = 200
        style = AndroidPaint.Style.FILL
      }
    val petalPaint =
      AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = petalInt
        style = AndroidPaint.Style.FILL
      }
    val strokePaint =
      AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = ringInt
        style = AndroidPaint.Style.STROKE
        strokeWidth = (diameter * 0.052f).coerceAtLeast(1.3f)
      }
    val innerStrokePaint =
      AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = ringInt
        alpha = 135
        style = AndroidPaint.Style.STROKE
        strokeWidth = (diameter * 0.03f).coerceAtLeast(0.9f)
      }

    for (i in 0 until 8) {
      val angleRad = (i * 45.0) * Math.PI / 180.0
      val px = cx + (outerRadius * 0.88f * cos(angleRad)).toFloat()
      val py = cy + (outerRadius * 0.88f * sin(angleRad)).toFloat()
      val isCardinal = i % 2 == 0
      canvas.drawCircle(px, py, outerRadius * (if (isCardinal) 0.24f else 0.17f), ringFillPaint)
      canvas.drawCircle(
        px,
        py,
        outerRadius * (if (isCardinal) 0.16f else 0.10f),
        if (isCardinal) petalPaint else fillPaint,
      )
    }

    canvas.drawCircle(cx, cy, outerRadius * 0.86f, fillPaint)
    canvas.drawCircle(cx, cy, outerRadius * 0.86f, strokePaint)
    canvas.drawCircle(cx, cy, innerRadius * 0.82f, innerStrokePaint)

    val digitSize =
      when {
        digits.length >= 3 -> diameter * 0.34f
        digits.length == 2 -> diameter * 0.39f
        else -> diameter * 0.43f
      }
    val digitPaint =
      AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = digitColor
        textSize = digitSize
        typeface = Typeface.create(digitTypeface, Typeface.BOLD)
        textAlign = AndroidPaint.Align.CENTER
      }
    val fm = digitPaint.fontMetrics
    val baselineY = cy - (fm.ascent + fm.descent) / 2f
    canvas.drawText(digits, cx, baselineY, digitPaint)
  }
}
