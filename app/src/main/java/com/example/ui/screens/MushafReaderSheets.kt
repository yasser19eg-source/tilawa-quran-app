package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MushafPageRepository
import com.example.data.QuranCatalog
import com.example.model.MushafAyah
import com.example.model.Surah
import com.example.ui.components.AyahShareCardGenerator
import com.example.ui.components.MushafAyahRosetteBadge
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.MushafPrintedInkDark
import com.example.ui.theme.MushafPrintedInkLight
import com.example.ui.theme.UthmaniQuranFontFamily

enum class MushafJumpTab(val labelAr: String) {
  SURAH("السور"),
  JUZ("الأجزاء"),
  PAGE("رقم الصفحة"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafQuickJumpSheet(
  currentPage: Int,
  onJumpToPage: (Int) -> Unit,
  onOpenFullIndex: (() -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedTab by rememberSaveable { mutableStateOf(MushafJumpTab.SURAH) }
  var surahQuery by rememberSaveable { mutableStateOf("") }
  var pageInput by rememberSaveable { mutableStateOf(currentPage.toString()) }
  var pageError by remember { mutableStateOf<String?>(null) }

  val filteredSurahs =
    remember(surahQuery) {
      val q = QuranCatalog.normalizeArabic(surahQuery)
      if (q.isEmpty()) QuranCatalog.surahs
      else
        QuranCatalog.surahs.filter { surah ->
          QuranCatalog.normalizeArabic(surah.nameAr).contains(q) ||
            surah.number.toString().contains(q)
        }
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .testTag("mushaf_quick_jump_sheet"),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "الانتقال السريع في المصحف",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
        if (onOpenFullIndex != null) {
          OutlinedButton(
            onClick = {
              onDismiss()
              onOpenFullIndex()
            },
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.padding(end = 6.dp).testTag("quick_jump_open_full_index"),
          ) {
            Text("الفهرس الكامل", style = MaterialTheme.typography.labelMedium)
          }
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("quick_jump_close"),
        ) {
          Icon(imageVector = Icons.Rounded.Close, contentDescription = "إغلاق")
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        MushafJumpTab.entries.forEach { tab ->
          FilterChip(
            selected = selectedTab == tab,
            onClick = { selectedTab = tab },
            label = { Text(tab.labelAr) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            modifier = Modifier.weight(1f).testTag("jump_tab_${tab.name.lowercase()}"),
          )
        }
      }

      when (selectedTab) {
        MushafJumpTab.SURAH -> {
          OutlinedTextField(
            value = surahQuery,
            onValueChange = { surahQuery = it },
            placeholder = { Text("ابحث باسم السورة أو رقمها...") },
            leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("quick_jump_surah_search"),
          )

          LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
          ) {
            items(filteredSurahs, key = { it.number }) { surah ->
              val startPage = MushafPageRepository.startPageForSurah(surah.number)
              val endPage = MushafPageRepository.endPageForSurah(surah.number)
              val juzRange = MushafPageRepository.juzRangeForSurah(surah.number)
              val juzText =
                if (juzRange.first == juzRange.last)
                  "الجزء ${AyahShareCardGenerator.toArabicIndicDigits(juzRange.first)}"
                else
                  "الأجزاء ${AyahShareCardGenerator.toArabicIndicDigits(juzRange.first)}-${AyahShareCardGenerator.toArabicIndicDigits(juzRange.last)}"
              Card(
                onClick = {
                  onJumpToPage(startPage)
                  onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors =
                  CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                  ),
                modifier = Modifier.fillMaxWidth().testTag("jump_surah_item_${surah.number}"),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                      modifier =
                        Modifier.size(36.dp)
                          .clip(CircleShape)
                          .background(MaterialTheme.colorScheme.primaryContainer),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = AyahShareCardGenerator.toArabicIndicDigits(surah.number),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                      Text(
                        text = "سورة ${surah.nameAr}",
                        style =
                          MaterialTheme.typography.titleMedium.copy(
                            fontFamily = AmiriFontFamily,
                            fontWeight = FontWeight.Bold,
                          ),
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Text(
                        text = "${surah.revelationTypeAr} • ${AyahShareCardGenerator.toArabicIndicDigits(surah.ayahCount)} آية • $juzText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  Text(
                    text =
                      if (endPage > startPage)
                        "ص ${AyahShareCardGenerator.toArabicIndicDigits(startPage)} - ${AyahShareCardGenerator.toArabicIndicDigits(endPage)}"
                      else "ص ${AyahShareCardGenerator.toArabicIndicDigits(startPage)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }

        MushafJumpTab.JUZ -> {
          LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
          ) {
            items((1..30).toList(), key = { it }) { juzNum ->
              val startPage = MushafPageRepository.startPageForJuz(juzNum)
              val endPage = MushafPageRepository.endPageForJuz(juzNum)
              val juzName = MushafPageRepository.JUZ_ARABIC_NAMES[juzNum - 1]
              val juzSurahs = remember(juzNum) { MushafPageRepository.surahsInJuz(juzNum) }
              val surahHint =
                if (juzSurahs.size <= 2) juzSurahs.joinToString(" • ") { it.nameAr }
                else "${juzSurahs.first().nameAr} ← ${juzSurahs.last().nameAr}"
              Card(
                onClick = {
                  onJumpToPage(startPage)
                  onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors =
                  CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                  ),
                modifier = Modifier.fillMaxWidth().testTag("jump_juz_item_$juzNum"),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                      modifier =
                        Modifier.size(36.dp)
                          .clip(CircleShape)
                          .background(MaterialTheme.colorScheme.primaryContainer),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = AyahShareCardGenerator.toArabicIndicDigits(juzNum),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                      Text(
                        text = juzName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Text(
                        text = surahHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  Text(
                    text =
                      "ص ${AyahShareCardGenerator.toArabicIndicDigits(startPage)} - ${AyahShareCardGenerator.toArabicIndicDigits(endPage)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }

        MushafJumpTab.PAGE -> {
          val trySubmitPage: () -> Unit = {
            val parsed = pageInput.trim().toIntOrNull()
            if (parsed != null && parsed in 1..MushafPageRepository.TOTAL_MUSHAF_PAGES) {
              pageError = null
              onJumpToPage(parsed)
              onDismiss()
            } else {
              pageError = "أدخل رقم صفحة صحيح من 1 إلى 604"
            }
          }

          Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedTextField(
              value = pageInput,
              onValueChange = {
                pageInput = it.filter { ch -> ch.isDigit() }.take(3)
                pageError = null
              },
              label = { Text("رقم الصفحة (1 - 604)") },
              isError = pageError != null,
              supportingText = pageError?.let { err -> { Text(err) } },
              singleLine = true,
              keyboardOptions =
                KeyboardOptions(
                  keyboardType = KeyboardType.Number,
                  imeAction = ImeAction.Go,
                ),
              keyboardActions = KeyboardActions(onGo = { trySubmitPage() }),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("quick_jump_page_input"),
            )

            Button(
              onClick = trySubmitPage,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("quick_jump_page_confirm"),
            ) {
              Text("الانتقال إلى الصفحة")
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectedAyahsTafsirSheet(
  surah: Surah,
  ayahs: List<MushafAyah>,
  onCopyAll: () -> Unit,
  onShareAll: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp)
          .testTag("mushaf_selected_tafsir_sheet"),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = AyahShareCardGenerator.buildAyahRangeLabel(surah, ayahs),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f),
        )
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("selected_tafsir_close"),
        ) {
          Icon(imageVector = Icons.Rounded.Close, contentDescription = "إغلاق التفسير")
        }
      }

      ayahs.forEachIndexed { idx, ayah ->
        val inlineContent =
          remember(ayah.numberInSurah) {
            val digitsCount = if (ayah.numberInSurah >= 100) 3 else 2
            mapOf(
              "tafsir_ayah_rosette" to
                InlineTextContent(
                  Placeholder(
                    width = (if (digitsCount >= 3) 32f else 28f).sp,
                    height = 24.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                  )
                ) {
                  MushafAyahRosetteBadge(ayahNumber = ayah.numberInSurah, fontScale = 1.0f)
                }
            )
          }

        Text(
          text =
            buildAnnotatedString {
              append(ayah.textAr)
              append('\u00A0')
              appendInlineContent(
                "tafsir_ayah_rosette",
                "﴿${AyahShareCardGenerator.toArabicIndicDigits(ayah.numberInSurah)}﴾",
              )
            },
          inlineContent = inlineContent,
          style =
            MaterialTheme.typography.headlineSmall.copy(
              fontFamily = UthmaniQuranFontFamily,
              fontWeight = FontWeight.Normal,
              lineHeight = 48.sp,
              platformStyle = PlatformTextStyle(includeFontPadding = true),
            ),
          color =
            if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MushafPrintedInkDark
            else MushafPrintedInkLight,
        )

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text =
                "التفسير الميسر • الآية ${AyahShareCardGenerator.toArabicIndicDigits(ayah.numberInSurah)}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = ayah.tafsirMuyassarAr,
              style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 27.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (idx < ayahs.lastIndex) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onCopyAll,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("نسخ")
        }
        Button(
          onClick = {
            onDismiss()
            onShareAll()
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Rounded.Image,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("مشاركة")
        }
      }
    }
  }
}

@Composable
fun AyahShareModeDialog(
  surah: Surah,
  ayahs: List<MushafAyah>,
  onShareAsText: () -> Unit,
  onShareAsImage: () -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
      modifier = Modifier.fillMaxWidth().testTag("ayah_share_mode_dialog"),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "طريقة المشاركة",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = AyahShareCardGenerator.buildAyahRangeLabel(surah, ayahs),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = "إغلاق")
          }
        }

        Card(
          onClick = {
            onDismiss()
            onShareAsImage()
          },
          shape = RoundedCornerShape(16.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth().testTag("choose_share_as_image"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Image,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "مشاركة كصورة",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                text = "توليد بطاقة مصممة باسم التطبيق وطلب الدعاء مع إمكانية الحفظ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
              )
            }
          }
        }

        Card(
          onClick = {
            onDismiss()
            onShareAsText()
          },
          shape = RoundedCornerShape(16.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth().testTag("choose_share_as_text"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.TextFields,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "مشاركة كنص",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "إرسال نص الآيات بالتشكيل مباشرة لتطبيقات التواصل",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = AyahShareCardGenerator.FOOTER_FULL_DUA_REQUEST,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}
