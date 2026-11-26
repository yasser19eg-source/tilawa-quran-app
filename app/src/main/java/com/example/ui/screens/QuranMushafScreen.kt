package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MushafPageRepository
import com.example.data.QuranCatalog
import com.example.data.UserPreferencesState
import com.example.model.Surah
import com.example.ui.components.AyahShareCardGenerator
import com.example.ui.theme.AmiriFontFamily
import kotlinx.coroutines.launch

private enum class MushafIndexMode(val labelAr: String) {
  SURAHS("فهرس السور (١١٤)"),
  JUZ("الأجزاء (٣٠)"),
}

private enum class SurahRevelationFilter(val labelAr: String) {
  ALL("جميع السور"),
  MECCAN("مكية"),
  MEDINAN("مدنية"),
}

@Composable
fun QuranMushafScreen(
  preferences: UserPreferencesState,
  cachedSurahNumbers: Set<Int>,
  onOpenSurah: (Int) -> Unit,
  onOpenMushafPage: (Int) -> Unit = { surahNum -> onOpenSurah(surahNum) },
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)

  var indexMode by rememberSaveable { mutableStateOf(MushafIndexMode.SURAHS) }
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var selectedFilter by rememberSaveable { mutableStateOf(SurahRevelationFilter.ALL) }
  var selectedJuzNumber by rememberSaveable { mutableStateOf<Int?>(null) }
  var quickPageText by rememberSaveable { mutableStateOf("") }

  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  val filteredSurahs =
    remember(searchQuery, selectedFilter, selectedJuzNumber) {
      val normalizedQuery = QuranCatalog.normalizeArabic(searchQuery)
      QuranCatalog.surahs.filter { surah ->
        val matchesType =
          when (selectedFilter) {
            SurahRevelationFilter.ALL -> true
            SurahRevelationFilter.MECCAN -> surah.revelationTypeAr == "مكية"
            SurahRevelationFilter.MEDINAN -> surah.revelationTypeAr == "مدنية"
          }
        val matchesJuz =
          selectedJuzNumber?.let { juz -> MushafPageRepository.surahBelongsToJuz(surah.number, juz) }
            ?: true
        val matchesQuery =
          if (normalizedQuery.isEmpty()) true
          else {
            QuranCatalog.normalizeArabic(surah.nameAr).contains(normalizedQuery) ||
              surah.number.toString().contains(normalizedQuery) ||
              surah.paddedNumber.contains(normalizedQuery) ||
              MushafPageRepository.startPageForSurah(surah.number).toString() == normalizedQuery
          }
        matchesType && matchesJuz && matchesQuery
      }
    }

  val bookmarkedSurah =
    remember(preferences.hasMushafBookmark, preferences.lastReadSurahNumber) {
      if (preferences.hasMushafBookmark) QuranCatalog.findSurah(preferences.lastReadSurahNumber)
      else null
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("quran_mushaf_surah_list"),
      contentPadding = PaddingValues(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Header Card
      item(key = "mushaf_header") {
        Card(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("mushaf_back_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = "السابق",
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("السابق")
              }

              Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "مصحف المدينة • ٦٠٤ صفحات",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "فهرس السور والأجزاء",
              style =
                MaterialTheme.typography.headlineSmall.copy(
                  fontFamily = AmiriFontFamily,
                  fontWeight = FontWeight.Bold,
                ),
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text =
                "تنقّل سريعاً بين سور القرآن الكريم وأجزائه الثلاثين مع عرض رقم الصفحة المقابل لكل سورة في مصحف المدينة.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Open Full 604-Page Mushaf / Resume Bookmark Card
      item(key = "mushaf_bookmark") {
        val targetPage =
          if (preferences.hasMushafBookmark)
            preferences.lastReadMushafPage.coerceIn(1, MushafPageRepository.TOTAL_MUSHAF_PAGES)
          else 1
        val targetSurah = bookmarkedSurah ?: QuranCatalog.surahs.first()
        Card(
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp)
              .testTag("mushaf_resume_bookmark_card"),
          shape = RoundedCornerShape(18.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text =
                    if (preferences.hasMushafBookmark) "متابعة القراءة" else "تصفح المصحف الشريف",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text =
                    if (preferences.hasMushafBookmark)
                      "صفحة ${AyahShareCardGenerator.toArabicIndicDigits(targetPage)} • سورة ${targetSurah.nameAr}"
                    else "البدء من سورة الفاتحة (صفحة ١ من ٦٠٤)",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
            Button(
              onClick = { onOpenMushafPage(targetPage) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("mushaf_resume_bookmark_button"),
            ) {
              Text("فتح")
            }
          }
        }
      }

      // Direct Page Number Jump Row (1 - 604)
      item(key = "mushaf_direct_page_jump") {
        val jumpToEnteredPage: () -> Unit = {
          val page = quickPageText.trim().toIntOrNull()
          if (page != null && page in 1..MushafPageRepository.TOTAL_MUSHAF_PAGES) {
            onOpenMushafPage(page)
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = quickPageText,
            onValueChange = { quickPageText = it.filter { ch -> ch.isDigit() }.take(3) },
            placeholder = { Text("الانتقال السريع إلى صفحة (1 - 604)...") },
            leadingIcon = {
              Icon(imageVector = Icons.Rounded.FindInPage, contentDescription = null)
            },
            singleLine = true,
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Go,
              ),
            keyboardActions = KeyboardActions(onGo = { jumpToEnteredPage() }),
            shape = RoundedCornerShape(16.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
              ),
            modifier = Modifier.weight(1f).testTag("mushaf_page_number_input"),
          )
          val parsedPage = quickPageText.trim().toIntOrNull()
          Button(
            onClick = jumpToEnteredPage,
            enabled = parsedPage != null && parsedPage in 1..MushafPageRepository.TOTAL_MUSHAF_PAGES,
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("mushaf_page_jump_button"),
          ) {
            Text("انتقال")
          }
        }
      }

      // Index Switcher: Surahs (114) vs Juz (30)
      item(key = "mushaf_mode_tabs") {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          MushafIndexMode.entries.forEach { mode ->
            FilterChip(
              selected = indexMode == mode,
              onClick = { indexMode = mode },
              label = { Text(mode.labelAr) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
              modifier = Modifier.weight(1f).testTag("mushaf_index_tab_${mode.name.lowercase()}"),
            )
          }
        }
      }

      if (indexMode == MushafIndexMode.SURAHS) {
        // Search Input, Revelation Filters & Quick 30-Juz Navigation Strip
        item(key = "mushaf_search_and_juz_strip") {
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("ابحث باسم السورة، رقمها، أو رقم الصفحة...") },
              leadingIcon = {
                Icon(imageVector = Icons.Rounded.Search, contentDescription = "بحث")
              },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(
                    onClick = { searchQuery = "" },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Icon(imageVector = Icons.Rounded.Clear, contentDescription = "مسح البحث")
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(16.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                ),
              modifier = Modifier.fillMaxWidth().testTag("mushaf_surah_search_input"),
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(SurahRevelationFilter.entries) { filter ->
                FilterChip(
                  selected = filter == selectedFilter,
                  onClick = { selectedFilter = filter },
                  label = { Text(filter.labelAr) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                      selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
              }
            }

            // Quick Horizontal 30-Juz Navigation Strip inside the Surah Index
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "التنقل السريع بالأجزاء",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (selectedJuzNumber != null) {
                  TextButton(
                    onClick = { selectedJuzNumber = null },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("clear_selected_juz_button"),
                  ) {
                    Text("عرض كل السور", style = MaterialTheme.typography.labelMedium)
                  }
                }
              }

              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("mushaf_juz_quick_bar"),
              ) {
                item(key = "juz_all") {
                  FilterChip(
                    selected = selectedJuzNumber == null,
                    onClick = { selectedJuzNumber = null },
                    label = { Text("كل الأجزاء") },
                    colors =
                      FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                      ),
                    modifier = Modifier.testTag("juz_filter_chip_all"),
                  )
                }

                items((1..30).toList(), key = { "juz_chip_$it" }) { juzNumber ->
                  val isSelected = selectedJuzNumber == juzNumber
                  FilterChip(
                    selected = isSelected,
                    onClick = {
                      selectedJuzNumber = if (isSelected) null else juzNumber
                    },
                    label = {
                      Text(
                        "الجزء ${AyahShareCardGenerator.toArabicIndicDigits(juzNumber)}"
                      )
                    },
                    colors =
                      FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                      ),
                    modifier = Modifier.testTag("juz_filter_chip_$juzNumber"),
                  )
                }
              }
            }
          }
        }

        // Selected Juz Summary Banner with direct "Open Juz" action
        val activeJuz = selectedJuzNumber
        if (activeJuz != null) {
          item(key = "selected_juz_banner") {
            val juzName = MushafPageRepository.JUZ_ARABIC_NAMES[activeJuz - 1]
            val juzStart = MushafPageRepository.startPageForJuz(activeJuz)
            val juzEnd = MushafPageRepository.endPageForJuz(activeJuz)
            val surahCountInJuz = MushafPageRepository.surahsInJuz(activeJuz).size

            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
              modifier =
                Modifier.fillMaxWidth()
                  .padding(horizontal = 16.dp)
                  .testTag("selected_juz_banner_$activeJuz"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = juzName,
                    style =
                      MaterialTheme.typography.titleMedium.copy(
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.Bold,
                      ),
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text =
                      "صفحات ${AyahShareCardGenerator.toArabicIndicDigits(juzStart)} - ${AyahShareCardGenerator.toArabicIndicDigits(juzEnd)} • ${AyahShareCardGenerator.toArabicIndicDigits(surahCountInJuz)} سورة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                Button(
                  onClick = { onOpenMushafPage(juzStart) },
                  shape = RoundedCornerShape(12.dp),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                  modifier = Modifier.testTag("open_selected_juz_button"),
                ) {
                  Text(
                    "فتح ص ${AyahShareCardGenerator.toArabicIndicDigits(juzStart)}",
                    style = MaterialTheme.typography.labelLarge,
                  )
                }
              }
            }
          }
        }

        // Empty search state
        if (filteredSurahs.isEmpty()) {
          item(key = "mushaf_empty_search") {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = "لا توجد سور مطابقة للبحث",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(
                  onClick = {
                    searchQuery = ""
                    selectedFilter = SurahRevelationFilter.ALL
                    selectedJuzNumber = null
                  }
                ) {
                  Text("إعادة ضبط الفلاتر")
                }
              }
            }
          }
        }

        // 114 Surah Cards -> display page numbers & open exact Medina Mushaf page
        items(filteredSurahs, key = { it.number }) { surah ->
          val startPage = MushafPageRepository.startPageForSurah(surah.number)
          val endPage = MushafPageRepository.endPageForSurah(surah.number)
          val openPage =
            if (activeJuz != null) MushafPageRepository.surahOpenPageInJuz(surah.number, activeJuz)
            else startPage
          MushafSurahIndexCard(
            surah = surah,
            startPage = startPage,
            endPage = endPage,
            activeJuzFilter = activeJuz,
            openPageInContext = openPage,
            isCachedOffline = cachedSurahNumbers.contains(surah.number),
            onClick = { onOpenMushafPage(openPage) },
            modifier = Modifier.padding(horizontal = 16.dp),
          )
        }
      } else {
        // 30 Juz Cards -> display Surahs inside each Juz, page span, and direct open / browse Surahs
        items((1..30).toList(), key = { "juz_$it" }) { juzNumber ->
          val startPage = MushafPageRepository.startPageForJuz(juzNumber)
          val endPage = MushafPageRepository.endPageForJuz(juzNumber)
          val juzName = MushafPageRepository.JUZ_ARABIC_NAMES[juzNumber - 1]
          val juzSurahs = remember(juzNumber) { MushafPageRepository.surahsInJuz(juzNumber) }
          val surahsSummary =
            remember(juzSurahs) {
              when {
                juzSurahs.size <= 3 -> juzSurahs.joinToString(" • ") { "سورة ${it.nameAr}" }
                else ->
                  "من سورة ${juzSurahs.first().nameAr} إلى ${juzSurahs.last().nameAr} (${AyahShareCardGenerator.toArabicIndicDigits(juzSurahs.size)} سورة)"
              }
            }

          Card(
            onClick = { onOpenMushafPage(startPage) },
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("mushaf_juz_card_$juzNumber"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier =
                  Modifier.size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = AyahShareCardGenerator.toArabicIndicDigits(juzNumber),
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = juzName,
                  style =
                    MaterialTheme.typography.titleLarge.copy(
                      fontFamily = AmiriFontFamily,
                      fontWeight = FontWeight.Bold,
                    ),
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = surahsSummary,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text =
                    "صفحات ${AyahShareCardGenerator.toArabicIndicDigits(startPage)} - ${AyahShareCardGenerator.toArabicIndicDigits(endPage)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(50),
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                ) {
                  Text(
                    text = "ص ${AyahShareCardGenerator.toArabicIndicDigits(startPage)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }

                OutlinedButton(
                  onClick = {
                    selectedJuzNumber = juzNumber
                    indexMode = MushafIndexMode.SURAHS
                    coroutineScope.launch { listState.animateScrollToItem(0) }
                  },
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                  border =
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                  modifier =
                    Modifier.height(30.dp).testTag("juz_show_surahs_button_$juzNumber"),
                ) {
                  Text("عرض السور", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MushafSurahIndexCard(
  surah: Surah,
  startPage: Int,
  endPage: Int,
  activeJuzFilter: Int?,
  openPageInContext: Int,
  isCachedOffline: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val juzRange = remember(surah.number) { MushafPageRepository.juzRangeForSurah(surah.number) }
  val juzLabel =
    remember(juzRange) {
      if (juzRange.first == juzRange.last) {
        "الجزء ${AyahShareCardGenerator.toArabicIndicDigits(juzRange.first)}"
      } else {
        "الأجزاء ${AyahShareCardGenerator.toArabicIndicDigits(juzRange.first)} - ${AyahShareCardGenerator.toArabicIndicDigits(juzRange.last)}"
      }
    }

  Card(
    onClick = onClick,
    modifier = modifier.fillMaxWidth().testTag("mushaf_surah_card_${surah.number}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = AyahShareCardGenerator.toArabicIndicDigits(surah.number),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "سورة ${surah.nameAr}",
            style =
              MaterialTheme.typography.titleLarge.copy(
                fontFamily = AmiriFontFamily,
                fontWeight = FontWeight.Bold,
              ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (isCachedOffline) {
            Icon(
              imageVector = Icons.Rounded.CloudDone,
              contentDescription = "محفوظة بدون إنترنت",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(15.dp),
            )
          }
        }
        Text(
          text =
            "${surah.revelationTypeAr} • ${AyahShareCardGenerator.toArabicIndicDigits(surah.ayahCount)} آية • $juzLabel",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (activeJuzFilter != null && openPageInContext != startPage) {
          Text(
            text =
              "تبدأ في هذا الجزء من ص ${AyahShareCardGenerator.toArabicIndicDigits(openPageInContext)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Always display corresponding Mushaf page number(s) prominently
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        modifier = Modifier.testTag("mushaf_surah_page_badge_${surah.number}"),
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = "صفحة ${AyahShareCardGenerator.toArabicIndicDigits(startPage)}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          if (endPage > startPage) {
            Text(
              text = "إلى ${AyahShareCardGenerator.toArabicIndicDigits(endPage)}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
          }
        }
      }
    }
  }
}
