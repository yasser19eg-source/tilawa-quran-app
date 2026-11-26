package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LibraryAddCheck
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranCatalog
import com.example.model.MushafAyah
import com.example.model.MushafReaderUiState
import com.example.model.Surah
import com.example.ui.components.AyahImageSharePreviewDialog
import com.example.ui.components.AyahShareCardGenerator
import com.example.ui.components.MushafAyahRosetteBadge
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.MushafGoldBorder
import com.example.ui.theme.MushafParchmentDark
import com.example.ui.theme.MushafParchmentLight
import com.example.ui.theme.MushafPrintedInkDark
import com.example.ui.theme.MushafPrintedInkLight
import com.example.ui.theme.UthmaniQuranFontFamily
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

private enum class ReaderViewMode(val labelAr: String) {
  MUSHAF_PAGE("صفحة المصحف"),
  WITH_TAFSIR("آية وتفسيرها"),
}

private val JUZ_ARABIC_NAMES =
  listOf(
    "الجزء الأول",
    "الجزء الثاني",
    "الجزء الثالث",
    "الجزء الرابع",
    "الجزء الخامس",
    "الجزء السادس",
    "الجزء السابع",
    "الجزء الثامن",
    "الجزء التاسع",
    "الجزء العاشر",
    "الجزء الحادي عشر",
    "الجزء الثاني عشر",
    "الجزء الثالث عشر",
    "الجزء الرابع عشر",
    "الجزء الخامس عشر",
    "الجزء السادس عشر",
    "الجزء السابع عشر",
    "الجزء الثامن عشر",
    "الجزء التاسع عشر",
    "الجزء العشرون",
    "الجزء الحادي والعشرون",
    "الجزء الثاني والعشرون",
    "الجزء الثالث والعشرون",
    "الجزء الرابع والعشرون",
    "الجزء الخامس والعشرون",
    "الجزء السادس والعشرون",
    "الجزء السابع والعشرون",
    "الجزء الثامن والعشرون",
    "الجزء التاسع والعشرون",
    "الجزء الثلاثون",
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahReaderScreen(
  surahNumber: Int,
  readerState: MushafReaderUiState,
  fontScale: Float,
  bookmarkedSurahNumber: Int,
  bookmarkedAyahNumber: Int,
  hasBookmark: Boolean,
  onChangeFontScale: (Float) -> Unit,
  onSaveBookmark: (Int, Int) -> Unit,
  onNavigateSurah: (Int) -> Unit,
  onLoadSurahAyahs: (suspend (Int) -> Result<List<MushafAyah>>)? = null,
  onRetry: () -> Unit,
  onShowFeedback: (String) -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)

  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val surahMeta = remember(surahNumber) { QuranCatalog.findSurah(surahNumber) }
  val nextSurahMeta = remember(surahNumber) { QuranCatalog.findSurah(surahNumber + 1) }

  var viewMode by rememberSaveable { mutableStateOf(ReaderViewMode.MUSHAF_PAGE) }
  var useWarmMushafPaper by rememberSaveable { mutableStateOf(true) }
  var selectedAyahNumberForTafsir by rememberSaveable(surahNumber) { mutableStateOf<Int?>(null) }

  var isSelectionMode by rememberSaveable(surahNumber) { mutableStateOf(false) }
  var selectedAyahNumbersForShare by rememberSaveable(surahNumber) {
    mutableStateOf<List<Int>>(emptyList())
  }
  var imageShareAyahs by remember { mutableStateOf<List<MushafAyah>?>(null) }

  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  val mushafSurfaceColor =
    if (useWarmMushafPaper) {
      if (isDark) MushafParchmentDark else MushafParchmentLight
    } else {
      MaterialTheme.colorScheme.background
    }

  val successContent = (readerState as? MushafReaderUiState.Success)?.content
  val mushafPages =
    remember(successContent) {
      successContent?.ayahs?.groupBy { it.page }?.entries?.sortedBy { it.key }.orEmpty()
    }
  val pagerState =
    rememberPagerState(
      initialPage = 0,
      pageCount = { mushafPages.size.coerceAtLeast(1) },
    )

  val selectedAyahForSheet =
    remember(successContent, selectedAyahNumberForTafsir) {
      val num = selectedAyahNumberForTafsir ?: return@remember null
      successContent?.ayahs?.firstOrNull { it.numberInSurah == num }
    }

  val selectedShareSet = remember(selectedAyahNumbersForShare) { selectedAyahNumbersForShare.toSet() }
  val toggleAyahSelection: (Int) -> Unit = { ayahNum ->
    selectedAyahNumbersForShare =
      if (selectedShareSet.contains(ayahNum)) {
        (selectedShareSet - ayahNum).sorted()
      } else {
        (selectedShareSet + ayahNum).sorted()
      }
  }

  LaunchedEffect(surahNumber, mushafPages, hasBookmark, bookmarkedSurahNumber, bookmarkedAyahNumber) {
    if (mushafPages.isNotEmpty()) {
      val targetPageIdx =
        if (hasBookmark && bookmarkedSurahNumber == surahNumber) {
          val bookmarkedPage =
            successContent?.ayahs?.firstOrNull { it.numberInSurah == bookmarkedAyahNumber }?.page
          if (bookmarkedPage != null) {
            mushafPages.indexOfFirst { it.key == bookmarkedPage }.coerceAtLeast(0)
          } else {
            0
          }
        } else {
          0
        }
      if (targetPageIdx in mushafPages.indices && pagerState.currentPage != targetPageIdx) {
        pagerState.scrollToPage(targetPageIdx)
      }
    }
  }

  val shareOrCopyAyah: (MushafAyah, Surah, Boolean) -> Unit = { ayah, surah, isShare ->
    val formatted =
      "﴿ ${ayah.textAr} ﴾\n[سورة ${surah.nameAr}: ${ayah.numberInSurah}]\n\nالتفسير الميسر:\n${ayah.tafsirMuyassarAr}"
    if (isShare) {
      val intent =
        Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_TEXT, "$formatted\n\n— تطبيق تلاوة")
        }
      context.startActivity(Intent.createChooser(intent, "مشاركة الآية والتفسير"))
    } else {
      clipboardManager.setText(AnnotatedString(formatted))
      onShowFeedback("تم نسخ الآية والتفسير الميسر")
    }
  }

  Box(
    modifier = modifier.fillMaxSize().background(mushafSurfaceColor),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 840.dp)) {
      // Pinned Slim Reader Controls Bar
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onBackClick,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("reader_back_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = "السابق",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
              Text(
                text = "سورة ${surahMeta?.nameAr ?: surahNumber}",
                style =
                  MaterialTheme.typography.titleMedium.copy(
                    fontFamily = UthmaniQuranFontFamily,
                    fontWeight = FontWeight.Normal,
                  ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("surah_reader_title"),
              )
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              IconButton(
                onClick = {
                  selectedAyahNumberForTafsir = null
                  isSelectionMode = false
                  selectedAyahNumbersForShare = emptyList()
                  onNavigateSurah(surahNumber - 1)
                },
                enabled = surahNumber > 1,
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector = Icons.Rounded.ChevronRight,
                  contentDescription = "السورة السابقة",
                )
              }
              IconButton(
                onClick = {
                  selectedAyahNumberForTafsir = null
                  isSelectionMode = false
                  selectedAyahNumbersForShare = emptyList()
                  onNavigateSurah(surahNumber + 1)
                },
                enabled = surahNumber < 114,
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector = Icons.Rounded.ChevronLeft,
                  contentDescription = "السورة التالية",
                )
              }
              IconButton(
                onClick = {
                  isSelectionMode = !isSelectionMode
                  if (!isSelectionMode) selectedAyahNumbersForShare = emptyList()
                },
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("reader_multi_select_toggle"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.LibraryAddCheck,
                  contentDescription = "تحديد آيات للمشاركة كصورة",
                  tint =
                    if (isSelectionMode) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              IconButton(
                onClick = { useWarmMushafPaper = !useWarmMushafPaper },
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("reader_paper_toggle"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Palette,
                  contentDescription = "لون الورق",
                  tint =
                    if (useWarmMushafPaper) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              TextButton(
                onClick = { onChangeFontScale((fontScale - 0.1f).coerceAtLeast(0.8f)) },
                contentPadding = PaddingValues(horizontal = 6.dp),
                modifier = Modifier.testTag("reader_font_decrease"),
              ) {
                Text("أ-", style = MaterialTheme.typography.labelLarge)
              }
              TextButton(
                onClick = { onChangeFontScale((fontScale + 0.1f).coerceAtMost(1.7f)) },
                contentPadding = PaddingValues(horizontal = 6.dp),
                modifier = Modifier.testTag("reader_font_increase"),
              ) {
                Text("أ+", style = MaterialTheme.typography.labelLarge)
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ReaderViewMode.entries.forEach { mode ->
              FilterChip(
                selected = viewMode == mode,
                onClick = { viewMode = mode },
                label = { Text(mode.labelAr, style = MaterialTheme.typography.labelMedium) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  ),
                modifier = Modifier.weight(1f).testTag("reader_mode_${mode.name.lowercase()}"),
              )
            }

            if (readerState is MushafReaderUiState.Success && readerState.isFromCache) {
              Icon(
                imageVector = Icons.Rounded.CloudDone,
                contentDescription = "محفوظة بدون إنترنت",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
            }
          }

          if (isSelectionMode) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "وضع التحديد: اضغط على آية أو أكثر في الصفحة لتحديدها ومشاركتها كصورة",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
          }
        }
      }

      when (readerState) {
        is MushafReaderUiState.Loading -> {
          Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
              Text(
                text = "جاري تحميل صفحات المصحف والتفسير الميسر...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        is MushafReaderUiState.Error -> {
          Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
          ) {
            Card(
              modifier = Modifier.fillMaxWidth().testTag("reader_error_card"),
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.ErrorOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(40.dp),
                )
                Text(
                  text = readerState.messageAr,
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center,
                )
                Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
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
          }
        }

        is MushafReaderUiState.Success -> {
          val content = readerState.content

          if (viewMode == ReaderViewMode.MUSHAF_PAGE) {
            if (mushafPages.isNotEmpty()) {
              HorizontalPager(
                state = pagerState,
                key = { idx -> "mushaf_page_${mushafPages[idx].key}" },
                pageSpacing = 4.dp,
                modifier =
                  Modifier.weight(1f)
                    .fillMaxWidth()
                    .testTag("mushaf_horizontal_pager"),
              ) { pageIndex ->
                val (pageNumber, pageAyahs) = mushafPages[pageIndex]
                val pageOffset =
                  (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                val spineShadowColor =
                  if (isDark) Color.Black.copy(alpha = 0.24f)
                  else MushafGoldBorder.copy(alpha = 0.22f)

                BoxWithConstraints(
                  modifier =
                    Modifier.fillMaxSize()
                      .graphicsLayer {
                        val scale = 1f - (absOffset * 0.03f)
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - (absOffset * 0.16f)
                        rotationY = (-pageOffset * 9f).coerceIn(-11f, 11f)
                        cameraDistance = 18f * density
                      }
                ) {
                  val pageMinHeight = maxHeight
                  Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                      modifier =
                        Modifier.fillMaxSize()
                          .verticalScroll(rememberScrollState())
                          .padding(bottom = if (isSelectionMode) 74.dp else 0.dp)
                    ) {
                      ClassicMushafPage(
                        surah = content.surah,
                        pageNumber = pageNumber,
                        pageAyahs = pageAyahs,
                        isFirstPageOfSurah = pageIndex == 0,
                        pageSurfaceColor = mushafSurfaceColor,
                        fontScale = fontScale,
                        selectedAyahNumberForTafsir = selectedAyahNumberForTafsir,
                        multiSelectedAyahNumbers = selectedShareSet,
                        bookmarkedAyahNumber =
                          if (hasBookmark && bookmarkedSurahNumber == content.surah.number)
                            bookmarkedAyahNumber
                          else null,
                        onAyahTap = { tapped ->
                          if (isSelectionMode) {
                            toggleAyahSelection(tapped.numberInSurah)
                          } else {
                            selectedAyahNumberForTafsir = tapped.numberInSurah
                          }
                        },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = pageMinHeight),
                      )

                      if (pageIndex == mushafPages.lastIndex && nextSurahMeta != null) {
                        OutlinedButton(
                          onClick = {
                            selectedAyahNumberForTafsir = null
                            isSelectionMode = false
                            selectedAyahNumbersForShare = emptyList()
                            onNavigateSurah(nextSurahMeta.number)
                          },
                          shape = RoundedCornerShape(16.dp),
                          border =
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                          modifier =
                            Modifier.fillMaxWidth()
                              .padding(horizontal = 16.dp, vertical = 10.dp)
                              .testTag("reader_continue_next_surah"),
                        ) {
                          Text(
                            text = "السورة التالية: سورة ${nextSurahMeta.nameAr}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                          )
                          Spacer(modifier = Modifier.width(6.dp))
                          Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                          )
                        }
                      }
                    }

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
              }

              // Slim Bottom Mushaf Page Flip Bar
              val currentPageEntry = mushafPages.getOrNull(pagerState.currentPage)
              if (currentPageEntry != null && !isSelectionMode) {
                Surface(
                  color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                  tonalElevation = 1.dp,
                  modifier = Modifier.fillMaxWidth().testTag("mushaf_pager_bar"),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    IconButton(
                      onClick = {
                        val prev = pagerState.currentPage - 1
                        if (prev >= 0) {
                          coroutineScope.launch { pagerState.animateScrollToPage(prev) }
                        }
                      },
                      enabled = pagerState.currentPage > 0,
                      modifier =
                        Modifier.minimumInteractiveComponentSize()
                          .testTag("previous_mushaf_page_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "الصفحة السابقة",
                      )
                    }

                    Text(
                      text =
                        "صفحة ${AyahShareCardGenerator.toArabicIndicDigits(currentPageEntry.key)}  •  ${AyahShareCardGenerator.toArabicIndicDigits(pagerState.currentPage + 1)} من ${AyahShareCardGenerator.toArabicIndicDigits(mushafPages.size)}",
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    IconButton(
                      onClick = {
                        val next = pagerState.currentPage + 1
                        if (next < mushafPages.size) {
                          coroutineScope.launch { pagerState.animateScrollToPage(next) }
                        }
                      },
                      enabled = pagerState.currentPage < mushafPages.lastIndex,
                      modifier =
                        Modifier.minimumInteractiveComponentSize()
                          .testTag("next_mushaf_page_button"),
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
          } else {
            LazyColumn(
              state = listState,
              modifier = Modifier.weight(1f).fillMaxWidth().testTag("surah_reader_list"),
              contentPadding = PaddingValues(top = 8.dp, bottom = if (isSelectionMode) 96.dp else 28.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              if (content.surah.number != 1 && content.surah.number != 9) {
                item(key = "reader_basmalah") {
                  Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    style =
                      MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = UthmaniQuranFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = (24f * fontScale).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = true),
                      ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier =
                      Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                  )
                }
              }

              items(content.ayahs, key = { it.globalNumber }) { ayah ->
                val isBookmarked =
                  hasBookmark &&
                    bookmarkedSurahNumber == content.surah.number &&
                    bookmarkedAyahNumber == ayah.numberInSurah
                val isSelectedForShare = selectedShareSet.contains(ayah.numberInSurah)
                AyahWithTafsirCard(
                  ayah = ayah,
                  fontScale = fontScale,
                  isBookmarked = isBookmarked,
                  isSelectionMode = isSelectionMode,
                  isSelectedForShare = isSelectedForShare,
                  onToggleSelectForShare = {
                    if (!isSelectionMode) isSelectionMode = true
                    toggleAyahSelection(ayah.numberInSurah)
                  },
                  onToggleBookmark = {
                    onSaveBookmark(content.surah.number, ayah.numberInSurah)
                    onShowFeedback(
                      "تم حفظ علامة القراءة عند سورة ${content.surah.nameAr} آية ${ayah.numberInSurah}"
                    )
                  },
                  onShareAsImage = { imageShareAyahs = listOf(ayah) },
                  onCopy = { shareOrCopyAyah(ayah, content.surah, false) },
                  onShare = { shareOrCopyAyah(ayah, content.surah, true) },
                  modifier = Modifier.padding(horizontal = 16.dp),
                )
              }
            }
          }
        }
      }
    }

    // Floating Multi-Ayah Selection Action Bar
    if (isSelectionMode && successContent != null) {
      val selectedAyahs =
        remember(successContent, selectedShareSet) {
          successContent.ayahs.filter { selectedShareSet.contains(it.numberInSurah) }
        }
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier =
          Modifier.align(Alignment.BottomCenter)
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("ayah_selection_bar"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text =
              if (selectedAyahs.isEmpty()) "اختر آية أو أكثر"
              else
                "تم تحديد ${AyahShareCardGenerator.toArabicIndicDigits(selectedAyahs.size)} آية",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TextButton(
              onClick = {
                isSelectionMode = false
                selectedAyahNumbersForShare = emptyList()
              },
              modifier = Modifier.testTag("cancel_ayah_selection_button"),
            ) {
              Text("إلغاء")
            }
            Button(
              onClick = { imageShareAyahs = selectedAyahs },
              enabled = selectedAyahs.isNotEmpty(),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("share_selected_ayahs_image_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("مشاركة كصورة")
            }
          }
        }
      }
    }
  }

  // Tafsir Muyassar Bottom Sheet on single-Ayah tap
  val activeSheetAyah = selectedAyahForSheet
  val activeSurah = successContent?.surah ?: surahMeta
  if (activeSheetAyah != null && activeSurah != null) {
    val totalAyahsInSurah = successContent?.ayahs?.size ?: activeSurah.ayahCount
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { selectedAyahNumberForTafsir = null },
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("ayah_tafsir_bottom_sheet"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text =
              "سورة ${activeSurah.nameAr} • الآية ${AyahShareCardGenerator.toArabicIndicDigits(activeSheetAyah.numberInSurah)} • ص ${AyahShareCardGenerator.toArabicIndicDigits(activeSheetAyah.page)}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = { selectedAyahNumberForTafsir = null },
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("tafsir_sheet_close_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "إغلاق التفسير",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = {
              selectedAyahNumberForTafsir =
                (activeSheetAyah.numberInSurah - 1).coerceAtLeast(1)
            },
            enabled = activeSheetAyah.numberInSurah > 1,
          ) {
            Text("الآية السابقة")
          }
          TextButton(
            onClick = {
              selectedAyahNumberForTafsir =
                (activeSheetAyah.numberInSurah + 1).coerceAtMost(totalAyahsInSurah)
            },
            enabled = activeSheetAyah.numberInSurah < totalAyahsInSurah,
          ) {
            Text("الآية التالية")
          }
        }

        val sheetInlineContent =
          remember(activeSheetAyah.numberInSurah) {
            val digitsCount = if (activeSheetAyah.numberInSurah >= 100) 3 else 2
            mapOf(
              "sheet_ayah_rosette" to
                InlineTextContent(
                  Placeholder(
                    width = (if (digitsCount >= 3) 32f else 28f).sp,
                    height = 24.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                  )
                ) {
                  MushafAyahRosetteBadge(
                    ayahNumber = activeSheetAyah.numberInSurah,
                    fontScale = 1.0f,
                  )
                }
            )
          }
        Text(
          text =
            buildAnnotatedString {
              append(activeSheetAyah.textAr)
              append('\u00A0')
              appendInlineContent(
                "sheet_ayah_rosette",
                "﴿${AyahShareCardGenerator.toArabicIndicDigits(activeSheetAyah.numberInSurah)}﴾",
              )
            },
          inlineContent = sheetInlineContent,
          style =
            MaterialTheme.typography.headlineSmall.copy(
              fontFamily = UthmaniQuranFontFamily,
              fontWeight = FontWeight.Normal,
              lineHeight = 50.sp,
              platformStyle = PlatformTextStyle(includeFontPadding = true),
            ),
          color = if (isDark) MushafPrintedInkDark else MushafPrintedInkLight,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

        Text(
          text = "التفسير الميسر",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = activeSheetAyah.tafsirMuyassarAr,
          style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("tafsir_sheet_body_text"),
        )

        // Share as image & multi-select actions
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = {
              val chosen = activeSheetAyah
              selectedAyahNumberForTafsir = null
              imageShareAyahs = listOf(chosen)
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("tafsir_sheet_share_image_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Image,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("مشاركة كصورة")
          }
          OutlinedButton(
            onClick = {
              val startingNum = activeSheetAyah.numberInSurah
              selectedAyahNumberForTafsir = null
              isSelectionMode = true
              selectedAyahNumbersForShare = (selectedShareSet + startingNum).sorted()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("tafsir_sheet_multi_select_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.LibraryAddCheck,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("تحديد عدة آيات")
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = {
              onSaveBookmark(activeSurah.number, activeSheetAyah.numberInSurah)
              onShowFeedback(
                "تم حفظ موضع القراءة: سورة ${activeSurah.nameAr} آية ${activeSheetAyah.numberInSurah}"
              )
            },
            modifier = Modifier.weight(1f).testTag("tafsir_sheet_bookmark_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Bookmark,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("حفظ الموضع")
          }
          OutlinedButton(
            onClick = { shareOrCopyAyah(activeSheetAyah, activeSurah, false) },
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Rounded.ContentCopy,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("نسخ النص")
          }
        }
      }
    }
  }

  // Share Modal Sheet (Text & Image Tabs + Surah & Range Picker)
  val pendingImageAyahs = imageShareAyahs
  if (!pendingImageAyahs.isNullOrEmpty() && activeSurah != null) {
    AyahImageSharePreviewDialog(
      surah = activeSurah,
      ayahs = pendingImageAyahs,
      initialDarkTheme = isDark,
      loadSurahAyahs = onLoadSurahAyahs,
      onDismiss = { imageShareAyahs = null },
      onShareFailure = { msg -> onShowFeedback(msg) },
      onImageSaved = { msg -> onShowFeedback(msg) },
    )
  }
}

@Composable
private fun ClassicMushafPage(
  surah: Surah,
  pageNumber: Int,
  pageAyahs: List<MushafAyah>,
  isFirstPageOfSurah: Boolean,
  pageSurfaceColor: Color,
  fontScale: Float,
  selectedAyahNumberForTafsir: Int?,
  multiSelectedAyahNumbers: Set<Int>,
  bookmarkedAyahNumber: Int?,
  onAyahTap: (MushafAyah) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  val primaryColor = MaterialTheme.colorScheme.primary
  val goldColor = MushafGoldBorder.copy(alpha = if (isDark) 0.55f else 0.85f)
  val ornamentFillColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
  val onSurfaceColor = if (isDark) MushafPrintedInkDark else MushafPrintedInkLight
  val highlightBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
  val bookmarkBgColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f)

  val firstJuz = (pageAyahs.firstOrNull()?.juz ?: 1).coerceIn(1, 30)
  val juzLabel = JUZ_ARABIC_NAMES[firstJuz - 1]
  val totalChars = remember(pageAyahs) { pageAyahs.sumOf { it.textAr.length } }
  val isOpeningCenteredLayout = isFirstPageOfSurah && totalChars < 360

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .background(pageSurfaceColor)
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .testTag("mushaf_page_card_$pageNumber"),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Classic Edge Header: Juz Ordinal (start) | Surah Name (end)
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = juzLabel,
          style =
            MaterialTheme.typography.titleSmall.copy(
              fontFamily = UthmaniQuranFontFamily,
              fontWeight = FontWeight.Normal,
            ),
          color = onSurfaceColor.copy(alpha = 0.8f),
        )
        Text(
          text = surah.nameAr,
          style =
            MaterialTheme.typography.titleSmall.copy(
              fontFamily = UthmaniQuranFontFamily,
              fontWeight = FontWeight.Normal,
            ),
          color = onSurfaceColor.copy(alpha = 0.8f),
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (isFirstPageOfSurah) {
        SurahOrnamentalBanner(
          surahNameAr = surah.nameAr,
          primaryColor = primaryColor,
          goldColor = goldColor,
          fillColor = ornamentFillColor,
          textColor = onSurfaceColor,
        )

        Spacer(modifier = Modifier.height(if (isOpeningCenteredLayout) 28.dp else 14.dp))

        if (surah.number != 1 && surah.number != 9) {
          Text(
            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            style =
              MaterialTheme.typography.headlineSmall.copy(
                fontFamily = UthmaniQuranFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = (24f * fontScale).sp,
                platformStyle = PlatformTextStyle(includeFontPadding = true),
              ),
            color = onSurfaceColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
          )
        }
      }

      val annotatedPage =
        remember(
          pageAyahs,
          primaryColor,
          onSurfaceColor,
          selectedAyahNumberForTafsir,
          multiSelectedAyahNumbers,
          bookmarkedAyahNumber,
          highlightBgColor,
          bookmarkBgColor,
          pageSurfaceColor,
        ) {
          buildAnnotatedString {
            pageAyahs.forEachIndexed { idx, ayah ->
              val isSelected =
                selectedAyahNumberForTafsir == ayah.numberInSurah ||
                  multiSelectedAyahNumbers.contains(ayah.numberInSurah)
              val isBookmarked = bookmarkedAyahNumber == ayah.numberInSurah
              val spanBackground =
                when {
                  isSelected -> highlightBgColor
                  isBookmarked -> bookmarkBgColor
                  else -> pageSurfaceColor.copy(alpha = 0f)
                }
              withLink(
                LinkAnnotation.Clickable(
                  tag = "ayah_${ayah.numberInSurah}",
                  linkInteractionListener = { onAyahTap(ayah) },
                )
              ) {
                withStyle(
                  SpanStyle(
                    color = onSurfaceColor,
                    background = spanBackground,
                  )
                ) {
                  append(ayah.textAr)
                  append('\u00A0')
                }
                appendInlineContent(
                  id = "ayah_rosette_${ayah.numberInSurah}",
                  alternateText = "﴿${AyahShareCardGenerator.toArabicIndicDigits(ayah.numberInSurah)}﴾",
                )
              }
              if (idx < pageAyahs.lastIndex) {
                append("  ")
              }
            }
          }
        }

      val inlineRosettes =
        remember(
          pageAyahs,
          fontScale,
          selectedAyahNumberForTafsir,
          multiSelectedAyahNumbers,
        ) {
          pageAyahs.associate { ayah ->
            val isHighlighted =
              selectedAyahNumberForTafsir == ayah.numberInSurah ||
                multiSelectedAyahNumbers.contains(ayah.numberInSurah)
            val widthSp = ((if (ayah.numberInSurah >= 100) 32f else 28f) * fontScale).sp
            val heightSp = (24f * fontScale).sp
            "ayah_rosette_${ayah.numberInSurah}" to
              InlineTextContent(
                Placeholder(
                  width = widthSp,
                  height = heightSp,
                  placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                )
              ) {
                MushafAyahRosetteBadge(
                  ayahNumber = ayah.numberInSurah,
                  fontScale = fontScale,
                  isHighlighted = isHighlighted,
                  onClick = { onAyahTap(ayah) },
                )
              }
          }
        }

      Text(
        text = annotatedPage,
        inlineContent = inlineRosettes,
        style =
          MaterialTheme.typography.headlineSmall.copy(
            fontFamily = UthmaniQuranFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (25f * fontScale).sp,
            lineHeight = (52f * fontScale).sp,
            platformStyle = PlatformTextStyle(includeFontPadding = true),
          ),
        textAlign =
          if (isOpeningCenteredLayout || totalChars < 260) TextAlign.Center else TextAlign.Justify,
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 2.dp)
            .testTag("mushaf_page_text_$pageNumber"),
      )
    }

    // Bottom Ornamental Page Number Cartouche alternating edges
    Box(
      modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 4.dp),
      contentAlignment =
        if (pageNumber % 2 == 1) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
      MushafPageNumberCartouche(
        pageNumber = pageNumber,
        primaryColor = primaryColor,
        goldColor = goldColor,
        fillColor = ornamentFillColor,
        textColor = onSurfaceColor,
        isOddPage = pageNumber % 2 == 1,
      )
    }
  }
}

@Composable
private fun SurahOrnamentalBanner(
  surahNameAr: String,
  primaryColor: Color,
  goldColor: Color,
  fillColor: Color,
  textColor: Color,
) {
  Box(
    modifier = Modifier.fillMaxWidth().height(54.dp),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val cy = h / 2f

      drawRoundRect(
        color = fillColor.copy(alpha = 0.35f),
        topLeft = Offset(2f, 4f),
        size = Size(w - 4f, h - 8f),
        cornerRadius = CornerRadius(6f, 6f),
      )
      drawRoundRect(
        color = primaryColor.copy(alpha = 0.75f),
        topLeft = Offset(2f, 4f),
        size = Size(w - 4f, h - 8f),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 2.2f),
      )
      drawRoundRect(
        color = goldColor,
        topLeft = Offset(7f, 9f),
        size = Size(w - 14f, h - 18f),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(width = 1.3f),
      )

      val wingCenterLeft = w * 0.14f
      val wingCenterRight = w * 0.86f
      listOf(wingCenterLeft, wingCenterRight).forEach { cx ->
        val outerR = h * 0.27f
        for (i in 0 until 8) {
          val angle = (i * 45.0) * Math.PI / 180.0
          val px = cx + (outerR * 0.65f * cos(angle)).toFloat()
          val py = cy + (outerR * 0.65f * sin(angle)).toFloat()
          drawCircle(
            color = primaryColor.copy(alpha = 0.55f),
            radius = outerR * 0.32f,
            center = Offset(px, py),
          )
        }
        drawCircle(
          color = goldColor,
          radius = outerR * 0.36f,
          center = Offset(cx, cy),
        )
        drawCircle(
          color = primaryColor,
          radius = outerR * 0.18f,
          center = Offset(cx, cy),
        )
      }

      val cartHalfW = w * 0.25f
      val tip = h * 0.24f
      val path =
        Path().apply {
          moveTo(w / 2f - cartHalfW, cy)
          lineTo(w / 2f - cartHalfW + tip, 8f)
          lineTo(w / 2f + cartHalfW - tip, 8f)
          lineTo(w / 2f + cartHalfW, cy)
          lineTo(w / 2f + cartHalfW - tip, h - 8f)
          lineTo(w / 2f - cartHalfW + tip, h - 8f)
          close()
        }
      drawPath(path = path, color = fillColor)
      drawPath(path = path, color = primaryColor, style = Stroke(width = 2.2f))
    }

    Text(
      text = "سُورَةُ $surahNameAr",
      style =
        MaterialTheme.typography.titleLarge.copy(
          fontFamily = UthmaniQuranFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 21.sp,
        ),
      color = textColor,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun MushafPageNumberCartouche(
  pageNumber: Int,
  primaryColor: Color,
  goldColor: Color,
  fillColor: Color,
  textColor: Color,
  isOddPage: Boolean,
) {
  Box(
    modifier = Modifier.width(104.dp).height(30.dp),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val boxW = w * 0.72f
      val boxLeft = if (isOddPage) w - boxW - 2f else 2f

      drawRoundRect(
        color = fillColor.copy(alpha = 0.55f),
        topLeft = Offset(boxLeft, 3f),
        size = Size(boxW, h - 6f),
        cornerRadius = CornerRadius(6f, 6f),
      )
      drawRoundRect(
        color = primaryColor.copy(alpha = 0.8f),
        topLeft = Offset(boxLeft, 3f),
        size = Size(boxW, h - 6f),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 1.8f),
      )

      val medCx = if (isOddPage) 14f else w - 14f
      val medCy = h / 2f
      drawCircle(color = primaryColor.copy(alpha = 0.45f), radius = 10f, center = Offset(medCx, medCy))
      drawCircle(color = goldColor, radius = 6f, center = Offset(medCx, medCy))
      drawCircle(color = primaryColor, radius = 3f, center = Offset(medCx, medCy))
    }

    Text(
      text = AyahShareCardGenerator.toArabicIndicDigits(pageNumber),
      style =
        MaterialTheme.typography.labelLarge.copy(
          fontFamily = AmiriFontFamily,
          fontWeight = FontWeight.Bold,
        ),
      color = textColor,
      modifier =
        Modifier.padding(
          start = if (isOddPage) 22.dp else 0.dp,
          end = if (isOddPage) 0.dp else 22.dp,
        ),
    )
  }
}

@Composable
private fun AyahWithTafsirCard(
  ayah: MushafAyah,
  fontScale: Float,
  isBookmarked: Boolean,
  isSelectionMode: Boolean,
  isSelectedForShare: Boolean,
  onToggleSelectForShare: () -> Unit,
  onToggleBookmark: () -> Unit,
  onShareAsImage: () -> Unit,
  onCopy: () -> Unit,
  onShare: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth().testTag("ayah_tafsir_card_${ayah.numberInSurah}"),
    shape = RoundedCornerShape(20.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isSelectedForShare) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
          else MaterialTheme.colorScheme.surface
      ),
    border =
      BorderStroke(
        width = if (isBookmarked || isSelectedForShare) 1.5.dp else 1.dp,
        color =
          if (isSelectedForShare || isBookmarked) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
      ),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        ) {
          Text(
            text =
              "الآية ${AyahShareCardGenerator.toArabicIndicDigits(ayah.numberInSurah)} • الجزء ${AyahShareCardGenerator.toArabicIndicDigits(ayah.juz)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isSelectionMode) {
            IconButton(
              onClick = onToggleSelectForShare,
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector =
                  if (isSelectedForShare) Icons.Rounded.CheckCircle
                  else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = "تحديد الآية للمشاركة",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
          IconButton(
            onClick = onShareAsImage,
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector = Icons.Rounded.Image,
              contentDescription = "مشاركة الآية كصورة",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
          IconButton(
            onClick = onToggleBookmark,
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("ayah_bookmark_${ayah.numberInSurah}"),
          ) {
            Icon(
              imageVector =
                if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
              contentDescription = "حفظ علامة القراءة",
              tint =
                if (isBookmarked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(
            onClick = onCopy,
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector = Icons.Rounded.ContentCopy,
              contentDescription = "نسخ الآية والتفسير",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
          }
          IconButton(
            onClick = onShare,
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector = Icons.Rounded.Share,
              contentDescription = "مشاركة نص الآية",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      val cardInlineRosette =
        remember(ayah.numberInSurah, fontScale, isSelectedForShare) {
          val widthSp = ((if (ayah.numberInSurah >= 100) 32f else 28f) * fontScale).sp
          val heightSp = (24f * fontScale).sp
          mapOf(
            "card_ayah_rosette" to
              InlineTextContent(
                Placeholder(
                  width = widthSp,
                  height = heightSp,
                  placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                )
              ) {
                MushafAyahRosetteBadge(
                  ayahNumber = ayah.numberInSurah,
                  fontScale = fontScale,
                  isHighlighted = isSelectedForShare,
                )
              }
          )
        }
      Text(
        text =
          buildAnnotatedString {
            append(ayah.textAr)
            append('\u00A0')
            appendInlineContent(
              "card_ayah_rosette",
              "﴿${AyahShareCardGenerator.toArabicIndicDigits(ayah.numberInSurah)}﴾",
            )
          },
        inlineContent = cardInlineRosette,
        style =
          MaterialTheme.typography.headlineSmall.copy(
            fontFamily = UthmaniQuranFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (25f * fontScale).sp,
            lineHeight = (52f * fontScale).sp,
            platformStyle = PlatformTextStyle(includeFontPadding = true),
          ),
        color =
          if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MushafPrintedInkDark
          else MushafPrintedInkLight,
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(12.dp))

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
          Text(
            text = "التفسير الميسر",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = ayah.tafsirMuyassarAr,
            style =
              MaterialTheme.typography.bodyMedium.copy(
                fontSize = (15f * fontScale).sp,
                lineHeight = (25f * fontScale).sp,
              ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
