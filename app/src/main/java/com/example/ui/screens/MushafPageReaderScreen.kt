package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.MushafPageRepository
import com.example.data.QuranCatalog
import com.example.model.AyahLineBound
import com.example.model.MushafAyah
import com.example.model.MushafPageMeta
import com.example.model.SelectedAyahKey
import com.example.model.Surah
import com.example.ui.components.AyahShareCardGenerator
import com.example.ui.components.AyahShareModalSheet
import com.example.ui.components.AyahShareTab
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.MushafGoldBorder
import com.example.ui.theme.MushafParchmentDark
import com.example.ui.theme.MushafParchmentLight
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val LIGHT_PRINTED_INK_COLOR_MATRIX =
  ColorMatrix(
    floatArrayOf(
      0.86f, 0f, 0f, 0f, 35f,
      0f, 0.85f, 0f, 0f, 38f,
      0f, 0f, 0.86f, 0f, 36f,
      0f, 0f, 0f, 1f, 0f,
    )
  )

private val NIGHT_INK_COLOR_MATRIX =
  ColorMatrix(
    floatArrayOf(
      -0.84f, 0f, 0f, 0f, 237f,
      0f, -0.82f, 0f, 0f, 232f,
      0f, 0f, -0.78f, 0f, 220f,
      0f, 0f, 0f, 1f, 0f,
    )
  )

@Composable
fun MushafPageReaderScreen(
  initialPage: Int,
  bookmarkedPage: Int,
  hasBookmark: Boolean,
  getPageMeta: (Int) -> MushafPageMeta,
  getPageBounds: (Int) -> List<AyahLineBound>,
  onPageChanged: (Int) -> Unit,
  onResolveSelectedAyahs: suspend (Int, List<SelectedAyahKey>) -> Result<List<MushafAyah>>,
  onLoadSurahAyahs: (suspend (Int) -> Result<List<MushafAyah>>)? = null,
  onSavePageBookmark: (pageNumber: Int, surahNumber: Int, ayahNumber: Int) -> Unit,
  onShowFeedback: (String) -> Unit,
  onOpenFullIndex: (() -> Unit)? = null,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val systemDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  var isNightPaper by rememberSaveable(systemDark) { mutableStateOf(systemDark) }

  val safeInitialIndex = (initialPage.coerceIn(1, MushafPageRepository.TOTAL_MUSHAF_PAGES) - 1)
  val pagerState =
    rememberPagerState(
      initialPage = safeInitialIndex,
      pageCount = { MushafPageRepository.TOTAL_MUSHAF_PAGES },
    )
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val haptics = LocalHapticFeedback.current

  val currentPageNumber = pagerState.currentPage + 1
  val currentMeta = remember(currentPageNumber) { getPageMeta(currentPageNumber) }
  val primarySurah =
    remember(currentMeta.firstSurahNumber) {
      QuranCatalog.findSurah(currentMeta.firstSurahNumber) ?: QuranCatalog.surahs.first()
    }
  val juzName =
    remember(currentMeta.juzNumber) {
      MushafPageRepository.JUZ_ARABIC_NAMES.getOrElse(currentMeta.juzNumber - 1) {
        "الجزء ${currentMeta.juzNumber}"
      }
    }

  var anchorAyahKey by remember { mutableStateOf<SelectedAyahKey?>(null) }
  var selectedAyahKeys by remember { mutableStateOf<List<SelectedAyahKey>>(emptyList()) }
  var resolvedSelectionAyahs by remember { mutableStateOf<List<MushafAyah>?>(null) }
  var isResolvingSelection by remember { mutableStateOf(false) }

  var showQuickJumpSheet by remember { mutableStateOf(false) }
  var showTafsirSheet by remember { mutableStateOf(false) }
  var shareModeTarget by remember { mutableStateOf<Pair<Surah, List<MushafAyah>>?>(null) }
  var imagePreviewTarget by remember { mutableStateOf<Pair<Surah, List<MushafAyah>>?>(null) }

  BackHandler {
    if (selectedAyahKeys.isNotEmpty()) {
      anchorAyahKey = null
      selectedAyahKeys = emptyList()
      resolvedSelectionAyahs = null
    } else {
      onBackClick()
    }
  }

  LaunchedEffect(currentPageNumber) {
    onPageChanged(currentPageNumber)
  }

  LaunchedEffect(selectedAyahKeys, currentPageNumber) {
    if (selectedAyahKeys.isEmpty()) {
      resolvedSelectionAyahs = null
      isResolvingSelection = false
    } else {
      isResolvingSelection = true
      val result = onResolveSelectedAyahs(currentPageNumber, selectedAyahKeys)
      isResolvingSelection = false
      resolvedSelectionAyahs = result.getOrNull()
    }
  }

  val paperColor = if (isNightPaper) MushafParchmentDark else MushafParchmentLight
  val surfaceBarColor = if (isNightPaper) Color(0xFF1F2823) else Color(0xFFEFECE3)
  val borderStrokeColor =
    MushafGoldBorder.copy(alpha = if (isNightPaper) 0.42f else 0.78f)

  val activeSelectionSurah: Surah? =
    remember(selectedAyahKeys, primarySurah) {
      val sNum = selectedAyahKeys.firstOrNull()?.surahNumber ?: return@remember null
      QuranCatalog.findSurah(sNum) ?: primarySurah
    }

  val selectionSummaryLabel: String =
    remember(selectedAyahKeys, activeSelectionSurah) {
      val surah = activeSelectionSurah ?: return@remember ""
      if (selectedAyahKeys.isEmpty()) return@remember ""
      val firstNum = selectedAyahKeys.minOf { it.ayahNumber }
      val lastNum = selectedAyahKeys.maxOf { it.ayahNumber }
      if (firstNum == lastNum) {
        "سورة ${surah.nameAr} • الآية ${AyahShareCardGenerator.toArabicIndicDigits(firstNum)}"
      } else {
        "سورة ${surah.nameAr} • الآيات ${AyahShareCardGenerator.toArabicIndicDigits(firstNum)} - ${AyahShareCardGenerator.toArabicIndicDigits(lastNum)}"
      }
    }

  val ensureSelectionTextThen: ((Surah, List<MushafAyah>) -> Unit) -> Unit = { action ->
    val surah = activeSelectionSurah ?: primarySurah
    val cached = resolvedSelectionAyahs
    if (!cached.isNullOrEmpty()) {
      action(surah, cached)
    } else {
      coroutineScope.launch {
        isResolvingSelection = true
        val res = onResolveSelectedAyahs(currentPageNumber, selectedAyahKeys)
        isResolvingSelection = false
        val ayahs = res.getOrNull()
        if (!ayahs.isNullOrEmpty()) {
          resolvedSelectionAyahs = ayahs
          action(surah, ayahs)
        } else {
          onShowFeedback("تعذر تحميل نص الآية والتفسير. تأكد من الاتصال بالإنترنت ثم أعد المحاولة.")
        }
      }
    }
  }

  Box(
    modifier = modifier.fillMaxSize().background(paperColor).testTag("mushaf_604_reader_root"),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 880.dp)) {
      // Clean Top Navigation Bar
      Surface(
        color = surfaceBarColor,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().testTag("mushaf_604_top_bar"),
      ) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              IconButton(
                onClick = onBackClick,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("mushaf_604_back_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = "السابق",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier =
                  Modifier.clip(RoundedCornerShape(12.dp))
                    .clickable { showQuickJumpSheet = true }
                    .testTag("mushaf_604_header_jump_pill"),
              ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                  Text(
                    text = "سورة ${primarySurah.nameAr}",
                    style =
                      MaterialTheme.typography.titleMedium.copy(
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.Bold,
                      ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text =
                      "$juzName • ص ${AyahShareCardGenerator.toArabicIndicDigits(currentPageNumber)} / ${AyahShareCardGenerator.toArabicIndicDigits(MushafPageRepository.TOTAL_MUSHAF_PAGES)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                  )
                }
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              IconButton(
                onClick = { showQuickJumpSheet = true },
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("mushaf_604_quick_jump_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Tune,
                  contentDescription = "الانتقال السريع لسورة أو جزء أو صفحة",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }

              val isBookmarkedHere = hasBookmark && bookmarkedPage == currentPageNumber
              IconButton(
                onClick = {
                  onSavePageBookmark(
                    currentPageNumber,
                    currentMeta.firstSurahNumber,
                    currentMeta.firstAyahNumber,
                  )
                  onShowFeedback(
                    "تم حفظ موضع القراءة: صفحة ${AyahShareCardGenerator.toArabicIndicDigits(currentPageNumber)} (سورة ${primarySurah.nameAr})"
                  )
                },
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("mushaf_604_bookmark_button"),
              ) {
                Icon(
                  imageVector =
                    if (isBookmarkedHere) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                  contentDescription = "حفظ علامة الصفحة",
                  tint =
                    if (isBookmarkedHere) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              IconButton(
                onClick = { isNightPaper = !isNightPaper },
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("mushaf_604_theme_toggle"),
              ) {
                Icon(
                  imageVector =
                    if (isNightPaper) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                  contentDescription = if (isNightPaper) "الوضع النهاري الهادئ" else "الوضع الليلي",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(1.dp)
                .background(borderStrokeColor.copy(alpha = 0.45f))
          )
        }
      }

      // Main 604-Page Horizontal Mushaf Pager (Swipe right-to-left naturally in RTL)
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        HorizontalPager(
          state = pagerState,
          key = { idx -> "mushaf_medina_page_${idx + 1}" },
          pageSpacing = 4.dp,
          modifier = Modifier.fillMaxSize().testTag("mushaf_604_horizontal_pager"),
        ) { pageIndex ->
          val pageNumber = pageIndex + 1
          val pageBounds = remember(pageNumber) { getPageBounds(pageNumber) }
          val pageOffset =
            (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
          val absOffset = abs(pageOffset).coerceIn(0f, 1f)
          val spineShadowColor =
            if (isNightPaper) Color.Black.copy(alpha = 0.30f)
            else MushafGoldBorder.copy(alpha = 0.20f)

          Box(
            modifier =
              Modifier.fillMaxSize()
                .graphicsLayer {
                  val scale = 1f - (absOffset * 0.025f)
                  scaleX = scale
                  scaleY = scale
                  alpha = 1f - (absOffset * 0.14f)
                  rotationY = (-pageOffset * 8f).coerceIn(-10f, 10f)
                  cameraDistance = 18f * density
                }
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .testTag("mushaf_page_canvas_$pageNumber"),
            contentAlignment = Alignment.Center,
          ) {
            MedinaMushafPageSurface(
              pageNumber = pageNumber,
              pageBounds = pageBounds,
              selectedAyahKeys =
                if (pageNumber == currentPageNumber) selectedAyahKeys else emptyList(),
              isNightPaper = isNightPaper,
              paperColor = paperColor,
              borderColor = borderStrokeColor,
              onAyahLongPress = { hitKey ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                anchorAyahKey = hitKey
                selectedAyahKeys = listOf(hitKey)
              },
              onAyahTap = { hitKey ->
                if (selectedAyahKeys.isNotEmpty()) {
                  if (hitKey == null) {
                    anchorAyahKey = null
                    selectedAyahKeys = emptyList()
                  } else {
                    val anchor = anchorAyahKey ?: hitKey
                    if (anchor.surahNumber == hitKey.surahNumber) {
                      selectedAyahKeys =
                        MushafPageRepository.computeConsecutiveRange(anchor, hitKey)
                    } else {
                      anchorAyahKey = hitKey
                      selectedAyahKeys = listOf(hitKey)
                    }
                  }
                }
              },
            )

            // Subtle book-binding spine edge shadow on the inner margin
            Box(
              modifier =
                Modifier.fillMaxHeight()
                  .width(10.dp)
                  .align(
                    if (pageNumber % 2 == 1) Alignment.CenterStart else Alignment.CenterEnd
                  )
                  .background(
                    Brush.horizontalGradient(
                      colors =
                        if (pageNumber % 2 == 1) listOf(spineShadowColor, Color.Transparent)
                        else listOf(Color.Transparent, spineShadowColor)
                    )
                  )
            )
          }
        }

        // Floating Selection Action Toolbar when 1+ Ayahs are selected
        androidx.compose.animation.AnimatedVisibility(
          visible = selectedAyahKeys.isNotEmpty(),
          enter = slideInVertically { it } + fadeIn(),
          exit = slideOutVertically { it } + fadeOut(),
          modifier = Modifier.align(Alignment.BottomCenter),
        ) {
          val activeSurah = activeSelectionSurah ?: primarySurah
          val minSelected = selectedAyahKeys.minOfOrNull { it.ayahNumber } ?: 1
          val maxSelected = selectedAyahKeys.maxOfOrNull { it.ayahNumber } ?: 1
          val canExtendNext = maxSelected < activeSurah.ayahCount
          val canShrinkSelection = selectedAyahKeys.size > 1

          Surface(
            shape = RoundedCornerShape(22.dp),
            color = surfaceBarColor,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier =
              Modifier.widthIn(max = 620.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("mushaf_604_selection_toolbar"),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = selectionSummaryLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "اضغط على أي آية أخرى لتوسيع التحديد أو استخدم + التالية",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                  )
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  if (canShrinkSelection) {
                    IconButton(
                      onClick = {
                        selectedAyahKeys =
                          (minSelected until maxSelected).map {
                            SelectedAyahKey(activeSurah.number, it)
                          }
                      },
                      modifier =
                        Modifier.size(40.dp).testTag("selection_shrink_ayah_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "تقليص التحديد",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  if (canExtendNext) {
                    Surface(
                      onClick = {
                        val nextAyah = maxSelected + 1
                        selectedAyahKeys =
                          (minSelected..nextAyah).map {
                            SelectedAyahKey(activeSurah.number, it)
                          }
                      },
                      shape = RoundedCornerShape(50),
                      color = MaterialTheme.colorScheme.primaryContainer,
                      modifier = Modifier.testTag("selection_extend_next_ayah_button"),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.Add,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.onPrimaryContainer,
                          modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                          text = "التالية",
                          style = MaterialTheme.typography.labelMedium,
                          color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                      }
                    }
                  }
                  IconButton(
                    onClick = {
                      anchorAyahKey = null
                      selectedAyahKeys = emptyList()
                    },
                    modifier =
                      Modifier.size(40.dp).testTag("selection_clear_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Close,
                      contentDescription = "إلغاء التحديد",
                    )
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = {
                    ensureSelectionTextThen { _, _ -> showTafsirSheet = true }
                  },
                  enabled = !isResolvingSelection,
                  shape = RoundedCornerShape(14.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("selection_action_tafsir"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text("التفسير", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                  onClick = {
                    ensureSelectionTextThen { surah, ayahs ->
                      val text =
                        AyahShareCardGenerator.buildShareTextFormatted(
                          surah = surah,
                          ayahs = ayahs,
                          includeTafsir = false,
                        )
                      clipboardManager.setText(AnnotatedString(text))
                      onShowFeedback("تم نسخ الآيات المحددة")
                    }
                  },
                  enabled = !isResolvingSelection,
                  shape = RoundedCornerShape(14.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("selection_action_copy"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text("نسخ", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                  onClick = {
                    ensureSelectionTextThen { surah, ayahs ->
                      shareModeTarget = surah to ayahs
                    }
                  },
                  enabled = !isResolvingSelection,
                  shape = RoundedCornerShape(14.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary
                    ),
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("selection_action_share"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text("مشاركة", style = MaterialTheme.typography.labelLarge)
                }
              }
            }
          }
        }
      }

      // Clean Bottom Navigation Bar (when no selection bar is covering it)
      if (selectedAyahKeys.isEmpty()) {
        Surface(
          color = surfaceBarColor,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth().testTag("mushaf_604_bottom_bar"),
        ) {
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 4.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              IconButton(
                onClick = {
                  if (pagerState.currentPage > 0) {
                    coroutineScope.launch {
                      pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                  }
                },
                enabled = pagerState.currentPage > 0,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("mushaf_604_prev_page"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.ChevronRight,
                  contentDescription = "الصفحة السابقة",
                )
              }

              Column(
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Slider(
                  value = currentPageNumber.toFloat(),
                  onValueChange = { value ->
                    val targetIdx =
                      (value.roundToInt().coerceIn(1, MushafPageRepository.TOTAL_MUSHAF_PAGES) - 1)
                    if (targetIdx != pagerState.currentPage) {
                      coroutineScope.launch { pagerState.scrollToPage(targetIdx) }
                    }
                  },
                  valueRange = 1f..MushafPageRepository.TOTAL_MUSHAF_PAGES.toFloat(),
                  colors =
                    SliderDefaults.colors(
                      thumbColor = MaterialTheme.colorScheme.primary,
                      activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                  modifier = Modifier.fillMaxWidth().height(26.dp).testTag("mushaf_604_page_slider"),
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text =
                      "صفحة ${AyahShareCardGenerator.toArabicIndicDigits(currentPageNumber)} من ${AyahShareCardGenerator.toArabicIndicDigits(MushafPageRepository.TOTAL_MUSHAF_PAGES)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("mushaf_604_page_indicator"),
                  )
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Rounded.TouchApp,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = "اضغط مطولاً على آية للتحديد",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }

              IconButton(
                onClick = {
                  if (pagerState.currentPage < MushafPageRepository.TOTAL_MUSHAF_PAGES - 1) {
                    coroutineScope.launch {
                      pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                  }
                },
                enabled = pagerState.currentPage < MushafPageRepository.TOTAL_MUSHAF_PAGES - 1,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("mushaf_604_next_page"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.ChevronLeft,
                  contentDescription = "الصفحة التالية",
                )
              }
            }
          }
        }
      }
    }
  }

  if (showQuickJumpSheet) {
    MushafQuickJumpSheet(
      currentPage = currentPageNumber,
      onJumpToPage = { targetPage ->
        anchorAyahKey = null
        selectedAyahKeys = emptyList()
        val idx = (targetPage.coerceIn(1, MushafPageRepository.TOTAL_MUSHAF_PAGES) - 1)
        coroutineScope.launch { pagerState.scrollToPage(idx) }
      },
      onOpenFullIndex = onOpenFullIndex,
      onDismiss = { showQuickJumpSheet = false },
    )
  }

  if (showTafsirSheet) {
    val ayahs = resolvedSelectionAyahs.orEmpty()
    val surah = activeSelectionSurah ?: primarySurah
    if (ayahs.isNotEmpty()) {
      SelectedAyahsTafsirSheet(
        surah = surah,
        ayahs = ayahs,
        onCopyAll = {
          val text =
            AyahShareCardGenerator.buildShareTextFormatted(
              surah = surah,
              ayahs = ayahs,
              includeTafsir = true,
            )
          clipboardManager.setText(AnnotatedString(text))
          onShowFeedback("تم نسخ الآيات والتفسير الميسر")
        },
        onShareAll = {
          shareModeTarget = surah to ayahs
        },
        onDismiss = { showTafsirSheet = false },
      )
    }
  }

  val activeShareMode = shareModeTarget
  if (activeShareMode != null) {
    val (surah, ayahs) = activeShareMode
    AyahShareModalSheet(
      surah = surah,
      ayahs = ayahs,
      initialDarkTheme = isNightPaper,
      initialTab = AyahShareTab.IMAGE,
      loadSurahAyahs = onLoadSurahAyahs,
      onDismiss = { shareModeTarget = null },
      onShareFailure = onShowFeedback,
      onFeedback = onShowFeedback,
    )
  }

  val activeImagePreview = imagePreviewTarget
  if (activeImagePreview != null) {
    val (surah, ayahs) = activeImagePreview
    AyahShareModalSheet(
      surah = surah,
      ayahs = ayahs,
      initialDarkTheme = isNightPaper,
      initialTab = AyahShareTab.IMAGE,
      loadSurahAyahs = onLoadSurahAyahs,
      onDismiss = { imagePreviewTarget = null },
      onShareFailure = onShowFeedback,
      onFeedback = onShowFeedback,
    )
  }
}

@Composable
private fun MedinaMushafPageSurface(
  pageNumber: Int,
  pageBounds: List<AyahLineBound>,
  selectedAyahKeys: List<SelectedAyahKey>,
  isNightPaper: Boolean,
  paperColor: Color,
  borderColor: Color,
  onAyahLongPress: (SelectedAyahKey) -> Unit,
  onAyahTap: (SelectedAyahKey?) -> Unit,
) {
  val context = LocalContext.current
  val candidateUrls = remember(pageNumber) { MushafPageRepository.candidatePageImageUrls(pageNumber) }
  var mirrorIndex by remember(pageNumber) { mutableIntStateOf(0) }
  var reloadSeed by remember(pageNumber) { mutableIntStateOf(0) }
  val activeUrl = candidateUrls[mirrorIndex.coerceIn(0, candidateUrls.lastIndex)]

  val selectedSet = remember(selectedAyahKeys) { selectedAyahKeys.toSet() }
  val highlightFillColor =
    if (isNightPaper) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
  val highlightStrokeColor =
    if (isNightPaper) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

  val aspectRatio =
    MushafPageRepository.MUSHAF_IMAGE_WIDTH / MushafPageRepository.MUSHAF_IMAGE_HEIGHT

  BoxWithConstraints(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    val fittedWidth = min(maxWidth, maxHeight * aspectRatio)
    val fittedHeight = fittedWidth / aspectRatio

    Surface(
      shape = RoundedCornerShape(14.dp),
      color = paperColor,
      border = BorderStroke(1.dp, borderColor),
      shadowElevation = 2.dp,
      modifier = Modifier.size(width = fittedWidth, height = fittedHeight),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag("mushaf_page_interactive_surface_$pageNumber")
            .pointerInput(pageNumber, pageBounds) {
              detectTapGestures(
                onLongPress = { offset ->
                  val w = size.width.toFloat().coerceAtLeast(1f)
                  val h = size.height.toFloat().coerceAtLeast(1f)
                  val hit =
                    MushafPageRepository.findAyahAtNormalizedPoint(
                      bounds = pageBounds,
                      normX = offset.x / w,
                      normY = offset.y / h,
                    )
                  if (hit != null) {
                    onAyahLongPress(hit)
                  }
                },
                onTap = { offset ->
                  val w = size.width.toFloat().coerceAtLeast(1f)
                  val h = size.height.toFloat().coerceAtLeast(1f)
                  val hit =
                    MushafPageRepository.findAyahAtNormalizedPoint(
                      bounds = pageBounds,
                      normX = offset.x / w,
                      normY = offset.y / h,
                    )
                  onAyahTap(hit)
                },
              )
            }
      ) {
        // Selected Ayah Line Highlights Drawn Behind Crisp Transparent Calligraphy
        if (selectedSet.isNotEmpty() && pageBounds.isNotEmpty()) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            pageBounds.forEach { b ->
              val key = SelectedAyahKey(b.surahNumber, b.ayahNumber)
              if (selectedSet.contains(key)) {
                val left = (b.minX / MushafPageRepository.MUSHAF_IMAGE_WIDTH) * w
                val top = (b.minY / MushafPageRepository.MUSHAF_IMAGE_HEIGHT) * h
                val right = (b.maxX / MushafPageRepository.MUSHAF_IMAGE_WIDTH) * w
                val bottom = (b.maxY / MushafPageRepository.MUSHAF_IMAGE_HEIGHT) * h
                val padX = 4f
                val padY = 2f
                drawRoundRect(
                  color = highlightFillColor,
                  topLeft = Offset((left - padX).coerceAtLeast(0f), (top - padY).coerceAtLeast(0f)),
                  size =
                    Size(
                      width = ((right - left) + padX * 2f).coerceAtLeast(4f),
                      height = ((bottom - top) + padY * 2f).coerceAtLeast(4f),
                    ),
                  cornerRadius = CornerRadius(10f, 10f),
                )
                drawRoundRect(
                  color = highlightStrokeColor,
                  topLeft = Offset((left - padX).coerceAtLeast(0f), (top - padY).coerceAtLeast(0f)),
                  size =
                    Size(
                      width = ((right - left) + padX * 2f).coerceAtLeast(4f),
                      height = ((bottom - top) + padY * 2f).coerceAtLeast(4f),
                    ),
                  cornerRadius = CornerRadius(10f, 10f),
                  style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f),
                )
              }
            }
          }
        }

        // High-Resolution Medina Mushaf Page Image with Printed Ink Softening (#232624 / #EDE8DC)
        val isTransparentCalligraphyLayer = mirrorIndex < 3
        val pageInkColorFilter =
          if (isTransparentCalligraphyLayer) {
            ColorFilter.colorMatrix(
              if (isNightPaper) NIGHT_INK_COLOR_MATRIX else LIGHT_PRINTED_INK_COLOR_MATRIX
            )
          } else {
            null
          }
        SubcomposeAsyncImage(
          model =
            ImageRequest.Builder(context)
              .data("$activeUrl?retry=$reloadSeed")
              .diskCacheKey("medina_page_${pageNumber}_m$mirrorIndex")
              .memoryCacheKey("medina_page_${pageNumber}_m$mirrorIndex")
              .build(),
          contentDescription = "صفحة المصحف الشريف رقم $pageNumber",
          contentScale = ContentScale.FillBounds,
          colorFilter = pageInkColorFilter,
          modifier = Modifier.fillMaxSize().aspectRatio(aspectRatio, matchHeightConstraintsFirst = true),
          loading = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                CircularProgressIndicator(
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(32.dp),
                )
                Text(
                  text =
                    "جاري تحميل صفحة ${AyahShareCardGenerator.toArabicIndicDigits(pageNumber)}...",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          },
          error = {
            if (mirrorIndex < candidateUrls.lastIndex) {
              LaunchedEffect(pageNumber, mirrorIndex) { mirrorIndex += 1 }
            }
            Box(
              modifier = Modifier.fillMaxSize().padding(16.dp),
              contentAlignment = Alignment.Center,
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text =
                    "تعذر تحميل صورة الصفحة ${AyahShareCardGenerator.toArabicIndicDigits(pageNumber)}",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center,
                )
                Text(
                  text = "تحقق من اتصالك بالإنترنت لعرض صفحات المصحف وحفظها محلياً",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                )
                Button(
                  onClick = {
                    mirrorIndex = 0
                    reloadSeed += 1
                  },
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("إعادة المحاولة")
                }
              }
            }
          },
        )
      }
    }
  }
}
