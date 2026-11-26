package com.example.ui.components

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.MushafAyah
import com.example.model.Surah
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightOliveContainer
import com.example.ui.theme.LightOlivePrimary
import com.example.ui.theme.LightOutline
import com.example.ui.theme.LightSurfaceVariant
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.MushafCardDarkBg
import com.example.ui.theme.MushafCardDarkBorder
import com.example.ui.theme.MushafCardDarkOuter
import com.example.ui.theme.MushafCardDarkPanel
import com.example.ui.theme.MushafCardDarkTextSecondary
import com.example.ui.theme.MushafPrintedInkDark
import com.example.ui.theme.MushafPrintedInkLight
import com.example.ui.theme.OliveAccent
import com.example.ui.theme.OliveDeepContainer
import com.example.ui.theme.WarmGoldDark
import com.example.ui.theme.WarmGoldMuted
import java.io.File
import java.io.FileOutputStream

object AyahShareCardGenerator {

  const val APP_BRAND_TITLE = "تلاوة"
  const val FOOTER_REQUEST_LINE = "نسألكم الدعاء للمغفور له بإذن الله"
  const val FOOTER_NAME_LINE = "الحاج عبد العظيم رجب زكي"
  const val FOOTER_FULL_DUA_REQUEST =
    "نسألكم الدعاء للمغفور له بإذن الله الحاج عبد العظيم رجب زكي"

  fun toArabicIndicDigits(number: Int): String {
    val digits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return number.toString().map { ch ->
      if (ch in '0'..'9') digits[ch - '0'] else ch
    }.joinToString("")
  }

  fun buildAyahRangeLabel(surah: Surah, ayahs: List<MushafAyah>): String {
    if (ayahs.isEmpty()) return "سورة ${surah.nameAr}"
    val sorted = ayahs.sortedBy { it.numberInSurah }
    return "سورة ${surah.nameAr} • ${buildAyahOnlyRangeBadge(sorted)}"
  }

  fun buildAyahOnlyRangeBadge(ayahs: List<MushafAyah>): String {
    if (ayahs.isEmpty()) return "القرآن الكريم"
    val sorted = ayahs.sortedBy { it.numberInSurah }
    return if (sorted.size == 1) {
      "الآية ${toArabicIndicDigits(sorted.first().numberInSurah)}"
    } else {
      val firstNum = toArabicIndicDigits(sorted.first().numberInSurah)
      val lastNum = toArabicIndicDigits(sorted.last().numberInSurah)
      val isContiguous =
        sorted.zipWithNext().all { (a, b) -> b.numberInSurah == a.numberInSurah + 1 }
      if (isContiguous) {
        "الآيات $firstNum - $lastNum"
      } else {
        "${toArabicIndicDigits(sorted.size)} آيات مختارة"
      }
    }
  }

  fun buildShareTextFormatted(
    surah: Surah,
    ayahs: List<MushafAyah>,
    includeTafsir: Boolean = false,
  ): String {
    val sorted = ayahs.sortedBy { it.numberInSurah }
    val versesLine =
      sorted.joinToString(" ") { ayah ->
        "${ayah.textAr} ﴿${toArabicIndicDigits(ayah.numberInSurah)}﴾"
      }
    val header = buildAyahRangeLabel(surah, sorted)
    val tafsirPart =
      if (includeTafsir && sorted.isNotEmpty()) {
        "\n\nالتفسير الميسر:\n" +
          sorted.joinToString("\n") { ayah ->
            "(${toArabicIndicDigits(ayah.numberInSurah)}) ${ayah.tafsirMuyassarAr}"
          }
      } else {
        ""
      }
    return "﴿ $versesLine ﴾\n[$header]$tafsirPart\n\n— تطبيق $APP_BRAND_TITLE\n$FOOTER_FULL_DUA_REQUEST"
  }

  fun buildVerseBlockText(ayahs: List<MushafAyah>): String {
    val sorted = ayahs.sortedBy { it.numberInSurah }
    return sorted.joinToString(" ") { ayah ->
      "${ayah.textAr} ﴿${toArabicIndicDigits(ayah.numberInSurah)}﴾"
    }
  }

  fun renderAyahShareBitmap(
    context: Context,
    surah: Surah,
    ayahs: List<MushafAyah>,
    includeTafsir: Boolean,
    useDarkTheme: Boolean,
  ): Bitmap {
    val sortedAyahs = ayahs.sortedBy { it.numberInSurah }
    val widthPx = 1080
    val centerX = widthPx / 2f

    val uthmaniTypeface =
      try {
        ResourcesCompat.getFont(context, R.font.amiri_quran) ?: Typeface.SERIF
      } catch (_: Exception) {
        Typeface.SERIF
      }
    val uiTypeface =
      try {
        ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.SANS_SERIF
      } catch (_: Exception) {
        Typeface.SANS_SERIF
      }
    val uiBoldTypeface =
      try {
        ResourcesCompat.getFont(context, R.font.cairo_bold)
          ?: Typeface.create(uiTypeface, Typeface.BOLD)
      } catch (_: Exception) {
        Typeface.create(uiTypeface, Typeface.BOLD)
      }
    val amiriBoldTypeface =
      try {
        ResourcesCompat.getFont(context, R.font.amiri_bold)
          ?: Typeface.create(uthmaniTypeface, Typeface.BOLD)
      } catch (_: Exception) {
        Typeface.create(uthmaniTypeface, Typeface.BOLD)
      }

    val bgColor =
      if (useDarkTheme) MushafCardDarkOuter.toArgb() else LightBackground.toArgb()
    val cardColor =
      if (useDarkTheme) MushafCardDarkBg.toArgb() else 0xFFFBF9F4.toInt()
    val panelColor =
      if (useDarkTheme) MushafCardDarkPanel.toArgb() else LightSurfaceVariant.toArgb()
    val accentColor = if (useDarkTheme) OliveAccent.toArgb() else LightOlivePrimary.toArgb()
    val pillBgColor =
      if (useDarkTheme) OliveDeepContainer.toArgb() else LightOliveContainer.toArgb()
    val goldColor = if (useDarkTheme) WarmGoldDark.toArgb() else WarmGoldMuted.toArgb()
    val verseInkColor =
      if (useDarkTheme) MushafPrintedInkDark.toArgb() else MushafPrintedInkLight.toArgb()
    val primaryTextColor =
      if (useDarkTheme) MushafPrintedInkDark.toArgb() else LightTextPrimary.toArgb()
    val secondaryTextColor =
      if (useDarkTheme) MushafCardDarkTextSecondary.toArgb() else LightTextSecondary.toArgb()
    val outlineColor =
      if (useDarkTheme) MushafCardDarkBorder.toArgb() else LightOutline.toArgb()

    // 1. Measure Quranic verse block with generous spacing so tashkeel never overlaps
    val verseHorizontalMargin = 86f
    val verseLayoutWidth = (widthPx - verseHorizontalMargin * 2f).toInt()
    val totalCharCount = sortedAyahs.sumOf { it.textAr.length }
    val verseFontSize =
      when {
        totalCharCount > 900 -> 34f
        totalCharCount > 600 -> 39f
        totalCharCount > 320 -> 44f
        else -> 50f
      }

    val versePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = verseInkColor
        textSize = verseFontSize
        typeface = uthmaniTypeface
      }

    val verseSpannable =
      buildStyledVerseSpannable(
        ayahs = sortedAyahs,
        useDarkTheme = useDarkTheme,
        digitTypeface = uiBoldTypeface,
      )
    val verseLayout =
      StaticLayout.Builder.obtain(
        verseSpannable,
        0,
        verseSpannable.length,
        versePaint,
        verseLayoutWidth,
      )
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setLineSpacing(16f, 1.60f)
        .setIncludePad(true)
        .setTextDirection(TextDirectionHeuristics.RTL)
        .build()

    // 2. Measure optional Tafsir Muyassar block
    val tafsirBoxMarginX = 72f
    val tafsirText =
      if (includeTafsir && sortedAyahs.isNotEmpty()) {
        sortedAyahs.joinToString("\n") { ayah ->
          "(${toArabicIndicDigits(ayah.numberInSurah)}) ${ayah.tafsirMuyassarAr}"
        }
      } else {
        ""
      }

    val tafsirPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 25f
        typeface = uiTypeface
        textAlign = Paint.Align.LEFT
      }
    val tafsirTitlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = 25f
        typeface = uiBoldTypeface
        textAlign = Paint.Align.RIGHT
      }

    val tafsirLayoutWidth = (widthPx - tafsirBoxMarginX * 2f - 68f).toInt()
    val tafsirLayout =
      if (tafsirText.isNotEmpty()) {
        StaticLayout.Builder.obtain(
          tafsirText,
          0,
          tafsirText.length,
          tafsirPaint,
          tafsirLayoutWidth,
        )
          .setAlignment(Layout.Alignment.ALIGN_NORMAL)
          .setLineSpacing(6f, 1.32f)
          .setIncludePad(true)
          .setTextDirection(TextDirectionHeuristics.RTL)
          .build()
      } else {
        null
      }

    // 3. Measure Footer Dua layout cleanly (IMPORTANT: Keep TextPaint.Align.LEFT for StaticLayout!)
    val footerMarginX = 68f
    val footerBoxWidth = widthPx - footerMarginX * 2f
    val duaMaxWidth = (footerBoxWidth - 64f).toInt().coerceAtLeast(320)
    val duaTextPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 26f
        typeface = uiBoldTypeface
        textAlign = Paint.Align.LEFT
      }
    val duaLayout =
      StaticLayout.Builder.obtain(
        FOOTER_FULL_DUA_REQUEST,
        0,
        FOOTER_FULL_DUA_REQUEST.length,
        duaTextPaint,
        duaMaxWidth,
      )
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setLineSpacing(6f, 1.24f)
        .setIncludePad(true)
        .setTextDirection(TextDirectionHeuristics.RTL)
        .build()

    // 4. Compute exact vertical layout metrics so nothing collides or clips near bottom edges
    val topBarTop = 66f
    val topBarHeight = 96f
    val dividerSpacing = 26f
    val verseTopPadding = 40f
    val verseBottomGap = 44f
    val tafsirBoxPaddingTop = 60f
    val tafsirBoxPaddingBottom = 26f
    val tafsirSectionHeight =
      if (tafsirLayout != null) {
        tafsirLayout.height + tafsirBoxPaddingTop + tafsirBoxPaddingBottom + 34f
      } else {
        0f
      }

    val footerIconSize = 54f
    val footerInnerTopPad = 24f
    val footerBrandToLineGap = 16f
    val footerLineToDuaGap = 16f
    val footerInnerBottomPad = 26f
    val footerBarHeight =
      footerInnerTopPad +
        footerIconSize +
        footerBrandToLineGap +
        footerLineToDuaGap +
        duaLayout.height +
        footerInnerBottomPad
    val footerBottomOuterMargin = 68f

    val contentEndBottom =
      topBarTop +
        topBarHeight +
        dividerSpacing +
        verseTopPadding +
        verseLayout.height +
        verseBottomGap +
        tafsirSectionHeight

    val minRequiredHeight =
      (contentEndBottom + 24f + footerBarHeight + footerBottomOuterMargin).toInt()
    val totalHeight = minRequiredHeight.coerceAtLeast(860)

    val bitmap = Bitmap.createBitmap(widthPx, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(bgColor)

    // Outer double ornamental frame
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardColor }
    val outerRect = RectF(32f, 32f, widthPx - 32f, totalHeight - 32f)
    canvas.drawRoundRect(outerRect, 34f, 34f, fillPaint)

    val goldStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = goldColor
        style = Paint.Style.STROKE
        strokeWidth = 3f
      }
    canvas.drawRoundRect(outerRect, 34f, 34f, goldStroke)

    val innerRect = RectF(46f, 46f, widthPx - 46f, totalHeight - 46f)
    val innerStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = outlineColor
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
      }
    canvas.drawRoundRect(innerRect, 26f, 26f, innerStroke)

    // Subtle corner gold accents inside inner frame
    drawCornerAccents(canvas, innerRect, goldColor)

    // 5. Elegant Top Header Bar: Surah name on right (RTL start), Ayah range pill on left (RTL end)
    val headerBox = RectF(68f, topBarTop, widthPx - 68f, topBarTop + topBarHeight)
    val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = panelColor }
    canvas.drawRoundRect(headerBox, 22f, 22f, panelPaint)
    val panelStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = outlineColor
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
      }
    canvas.drawRoundRect(headerBox, 22f, 22f, panelStroke)

    val surahTitleText = "سورة ${surah.nameAr}"
    val ayahBadgeText = buildAyahOnlyRangeBadge(sortedAyahs)

    val headerSurahPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 34f
        typeface = amiriBoldTypeface
        textAlign = Paint.Align.RIGHT
      }
    val headerBadgePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = 24f
        typeface = uiBoldTypeface
        textAlign = Paint.Align.CENTER
      }

    // Shrink badge font gently if Surah name + badge need extra horizontal room
    var badgeTextW = headerBadgePaint.measureText(ayahBadgeText)
    val surahTextW = headerSurahPaint.measureText(surahTitleText)
    if (badgeTextW + surahTextW + 140f > headerBox.width()) {
      headerBadgePaint.textSize = 21f
      headerSurahPaint.textSize = 30f
      badgeTextW = headerBadgePaint.measureText(ayahBadgeText)
    }

    val headerMidY = headerBox.centerY()
    // Right side (RTL start): small gold diamond + Surah title
    val diamondX = headerBox.right - 34f
    drawOrnamentalDiamond(canvas, diamondX, headerMidY, 7f, goldColor)
    val surahFm = headerSurahPaint.fontMetrics
    val surahBaselineY = headerMidY - (surahFm.ascent + surahFm.descent) / 2f
    canvas.drawText(surahTitleText, diamondX - 18f, surahBaselineY, headerSurahPaint)

    // Left side (RTL end): Ayah range pill badge
    val badgePillW = (badgeTextW + 50f).coerceAtMost(headerBox.width() * 0.48f)
    val badgeRect =
      RectF(
        headerBox.left + 22f,
        headerMidY - 26f,
        headerBox.left + 22f + badgePillW,
        headerMidY + 26f,
      )
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pillBgColor }
    canvas.drawRoundRect(badgeRect, 26f, 26f, pillPaint)
    val badgeStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = goldColor
        alpha = 165
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
      }
    canvas.drawRoundRect(badgeRect, 26f, 26f, badgeStroke)
    val badgeFm = headerBadgePaint.fontMetrics
    val badgeBaselineY = headerMidY - (badgeFm.ascent + badgeFm.descent) / 2f
    canvas.drawText(ayahBadgeText, badgeRect.centerX(), badgeBaselineY, headerBadgePaint)

    // Ornamental divider below Top Header Bar
    val dividerY = headerBox.bottom + dividerSpacing
    val dividerPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = goldColor
        alpha = 140
        strokeWidth = 1.8f
      }
    canvas.drawLine(96f, dividerY, centerX - 24f, dividerY, dividerPaint)
    canvas.drawLine(centerX + 24f, dividerY, widthPx - 96f, dividerY, dividerPaint)
    drawOrnamentalDiamond(canvas, centerX, dividerY, 7.5f, goldColor)

    // 6. Center Quranic Verse(s) block vertically in remaining space above Tafsir / Footer
    val footerTop = (totalHeight - footerBottomOuterMargin - footerBarHeight).coerceAtLeast(contentEndBottom + 24f)
    val availableAboveFooter =
      footerTop - (dividerY + verseTopPadding) - tafsirSectionHeight - verseBottomGap
    val extraVerticalCentering =
      if (tafsirLayout == null && availableAboveFooter > verseLayout.height) {
        (availableAboveFooter - verseLayout.height) * 0.42f
      } else {
        0f
      }

    var cursorY = dividerY + verseTopPadding + extraVerticalCentering
    canvas.save()
    canvas.translate((widthPx - verseLayout.width) / 2f, cursorY)
    verseLayout.draw(canvas)
    canvas.restore()
    cursorY += verseLayout.height + verseBottomGap

    // 7. Optional Tafsir Muyassar Card
    if (tafsirLayout != null) {
      val tafsirBox =
        RectF(
          tafsirBoxMarginX,
          cursorY,
          widthPx - tafsirBoxMarginX,
          cursorY + tafsirLayout.height + tafsirBoxPaddingTop + tafsirBoxPaddingBottom,
        )
      canvas.drawRoundRect(tafsirBox, 22f, 22f, panelPaint)
      canvas.drawRoundRect(tafsirBox, 22f, 22f, panelStroke)

      // Calm Sage right-edge accent stripe
      val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
      val stripeRect =
        RectF(
          tafsirBox.right - 14f,
          tafsirBox.top + 22f,
          tafsirBox.right - 8f,
          tafsirBox.bottom - 22f,
        )
      canvas.drawRoundRect(stripeRect, 3f, 3f, stripePaint)

      canvas.drawText(
        "التفسير الميسر",
        tafsirBox.right - 30f,
        tafsirBox.top + 40f,
        tafsirTitlePaint,
      )

      canvas.save()
      canvas.translate(tafsirBox.left + 26f, tafsirBox.top + tafsirBoxPaddingTop)
      tafsirLayout.draw(canvas)
      canvas.restore()
    }

    // 8. Dedicated Visually Separated Bottom Footer (comfortably inset from bottom edges)
    val footerBox =
      RectF(
        footerMarginX,
        footerTop,
        widthPx - footerMarginX,
        footerTop + footerBarHeight,
      )
    canvas.drawRoundRect(footerBox, 24f, 24f, panelPaint)
    val footerBorderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = goldColor
        alpha = 145
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
      }
    canvas.drawRoundRect(footerBox, 24f, 24f, footerBorderPaint)

    // Top Row inside Footer: App Icon + "تلاوة" centered horizontally
    val appTitlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 35f
        typeface = amiriBoldTypeface
        textAlign = Paint.Align.RIGHT
      }
    val appTitleW = appTitlePaint.measureText(APP_BRAND_TITLE)
    val brandGap = 14f
    val brandBlockW = footerIconSize + brandGap + appTitleW
    val brandStartLeft = centerX - brandBlockW / 2f
    val brandRowCenterY = footerBox.top + footerInnerTopPad + footerIconSize / 2f

    val iconRight = brandStartLeft + brandBlockW
    val iconLeft = iconRight - footerIconSize
    val iconTop = brandRowCenterY - footerIconSize / 2f

    drawCircularAppIcon(
      context = context,
      canvas = canvas,
      destRect = RectF(iconLeft, iconTop, iconRight, iconTop + footerIconSize),
      borderColor = goldColor,
    )
    val titleFm = appTitlePaint.fontMetrics
    val titleBaselineY = brandRowCenterY - (titleFm.ascent + titleFm.descent) / 2f
    canvas.drawText(APP_BRAND_TITLE, iconLeft - brandGap, titleBaselineY, appTitlePaint)

    // Thin subtle gold divider between app brand and prayer request
    val footerDividerY = footerBox.top + footerInnerTopPad + footerIconSize + footerBrandToLineGap
    val footerDividerPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = goldColor
        alpha = 95
        strokeWidth = 1.2f
      }
    canvas.drawLine(
      footerBox.left + 84f,
      footerDividerY,
      footerBox.right - 84f,
      footerDividerY,
      footerDividerPaint,
    )

    // Centered Dua line comfortably padded above bottom edge
    val duaTopY = footerDividerY + footerLineToDuaGap
    canvas.save()
    canvas.translate(footerBox.left + (footerBox.width() - duaLayout.width) / 2f, duaTopY)
    duaLayout.draw(canvas)
    canvas.restore()

    return bitmap
  }

  private fun drawOrnamentalDiamond(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    radius: Float,
    color: Int,
  ) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val path =
      Path().apply {
        moveTo(cx, cy - radius)
        lineTo(cx + radius, cy)
        lineTo(cx, cy + radius)
        lineTo(cx - radius, cy)
        close()
      }
    canvas.drawPath(path, paint)
  }

  private fun drawCornerAccents(canvas: Canvas, rect: RectF, goldColor: Int) {
    val paint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = goldColor
        alpha = 120
        style = Paint.Style.FILL
      }
    val offset = 16f
    val r = 3.5f
    canvas.drawCircle(rect.left + offset, rect.top + offset, r, paint)
    canvas.drawCircle(rect.right - offset, rect.top + offset, r, paint)
    canvas.drawCircle(rect.left + offset, rect.bottom - offset, r, paint)
    canvas.drawCircle(rect.right - offset, rect.bottom - offset, r, paint)
  }

  private fun buildStyledVerseSpannable(
    ayahs: List<MushafAyah>,
    useDarkTheme: Boolean,
    digitTypeface: Typeface,
  ): SpannableStringBuilder {
    val builder = SpannableStringBuilder()
    ayahs.forEachIndexed { idx, ayah ->
      builder.append(ayah.textAr)
      builder.append('\u00A0')
      val markerStart = builder.length
      builder.append("﴿${toArabicIndicDigits(ayah.numberInSurah)}﴾")
      val markerEnd = builder.length
      builder.setSpan(
        MushafAyahRosetteSpan(
          ayahNumber = ayah.numberInSurah,
          useDarkTheme = useDarkTheme,
          digitTypeface = digitTypeface,
        ),
        markerStart,
        markerEnd,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
      )
      if (idx < ayahs.lastIndex) {
        builder.append("  ")
      }
    }
    return builder
  }

  private fun drawCircularAppIcon(
    context: Context,
    canvas: Canvas,
    destRect: RectF,
    borderColor: Int,
  ) {
    val iconBitmap =
      try {
        BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_art_tilawah)
      } catch (_: Exception) {
        null
      }

    canvas.save()
    val clipPath = Path().apply { addOval(destRect, Path.Direction.CW) }
    canvas.clipPath(clipPath)
    if (iconBitmap != null) {
      canvas.drawBitmap(iconBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
    } else {
      canvas.drawColor(borderColor)
    }
    canvas.restore()

    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderColor
        style = Paint.Style.STROKE
        strokeWidth = 2.4f
      }
    canvas.drawOval(destRect, borderPaint)
  }

  fun shareAyahText(
    context: Context,
    surah: Surah,
    ayahs: List<MushafAyah>,
    includeTafsir: Boolean = false,
  ): Boolean {
    return try {
      val formatted = buildShareTextFormatted(surah, ayahs, includeTafsir)
      val intent =
        Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_TEXT, formatted)
        }
      context.startActivity(Intent.createChooser(intent, "مشاركة الآيات كنص"))
      true
    } catch (_: Exception) {
      false
    }
  }

  fun shareAyahImage(
    context: Context,
    surah: Surah,
    ayahs: List<MushafAyah>,
    includeTafsir: Boolean,
    useDarkTheme: Boolean,
  ): Boolean {
    return try {
      val bitmap = renderAyahShareBitmap(context, surah, ayahs, includeTafsir, useDarkTheme)
      val shareDir = File(context.cacheDir, "shared_ayahs").apply { mkdirs() }
      val firstAyahNum = ayahs.minOfOrNull { it.numberInSurah } ?: 1
      val outFile = File(shareDir, "tilawah_ayah_${surah.number}_$firstAyahNum.png")
      FileOutputStream(outFile).use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
      }

      val uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
      val intent =
        Intent(Intent.ACTION_SEND).apply {
          type = "image/png"
          putExtra(Intent.EXTRA_STREAM, uri)
          putExtra(
            Intent.EXTRA_TEXT,
            "${buildAyahRangeLabel(surah, ayahs)} — تطبيق $APP_BRAND_TITLE\n$FOOTER_FULL_DUA_REQUEST",
          )
          clipData = ClipData.newRawUri(APP_BRAND_TITLE, uri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      context.startActivity(Intent.createChooser(intent, "مشاركة الآيات كصورة"))
      true
    } catch (_: Exception) {
      false
    }
  }

  fun saveAyahImageToGalleryApi29Plus(
    context: Context,
    surah: Surah,
    ayahs: List<MushafAyah>,
    includeTafsir: Boolean,
    useDarkTheme: Boolean,
  ): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    return try {
      val bitmap = renderAyahShareBitmap(context, surah, ayahs, includeTafsir, useDarkTheme)
      val firstAyahNum = ayahs.minOfOrNull { it.numberInSurah } ?: 1
      val fileName = "tilawah_${surah.number}_${firstAyahNum}_${System.currentTimeMillis()}.png"
      val values =
        ContentValues().apply {
          put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
          put(MediaStore.Images.Media.MIME_TYPE, "image/png")
          put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Tilawah")
          put(MediaStore.Images.Media.IS_PENDING, 1)
        }
      val resolver = context.contentResolver
      val uri =
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
      try {
        resolver.openOutputStream(uri)?.use { stream ->
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        } ?: return false
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        true
      } catch (e: Exception) {
        resolver.delete(uri, null, null)
        false
      }
    } catch (_: Exception) {
      false
    }
  }
}
