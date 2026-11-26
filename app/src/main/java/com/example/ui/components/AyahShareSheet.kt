package com.example.ui.components

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.QuranCatalog
import com.example.model.MushafAyah
import com.example.model.Surah
import com.example.ui.theme.AmiriFontFamily
import kotlinx.coroutines.launch

enum class AyahShareTab(val labelAr: String) {
  IMAGE("صورة"),
  TEXT("نص"),
}

@Composable
fun AyahImageSharePreviewDialog(
  surah: Surah,
  ayahs: List<MushafAyah>,
  initialDarkTheme: Boolean,
  loadSurahAyahs: (suspend (Int) -> Result<List<MushafAyah>>)? = null,
  onDismiss: () -> Unit,
  onShareFailure: (String) -> Unit,
  onImageSaved: (String) -> Unit = {},
) {
  AyahShareModalSheet(
    surah = surah,
    ayahs = ayahs,
    initialDarkTheme = initialDarkTheme,
    initialTab = AyahShareTab.IMAGE,
    loadSurahAyahs = loadSurahAyahs,
    onDismiss = onDismiss,
    onShareFailure = onShareFailure,
    onFeedback = onImageSaved,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahShareModalSheet(
  surah: Surah,
  ayahs: List<MushafAyah>,
  initialDarkTheme: Boolean,
  initialTab: AyahShareTab = AyahShareTab.IMAGE,
  loadSurahAyahs: (suspend (Int) -> Result<List<MushafAyah>>)? = null,
  onDismiss: () -> Unit,
  onShareFailure: (String) -> Unit,
  onFeedback: (String) -> Unit = {},
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val scope = rememberCoroutineScope()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val initialSorted = remember(ayahs) { ayahs.sortedBy { it.numberInSurah } }
  val initialFrom = initialSorted.firstOrNull()?.numberInSurah ?: 1
  val initialTo = initialSorted.lastOrNull()?.numberInSurah ?: initialFrom

  var activeTab by remember { mutableStateOf(initialTab) }
  var selectedSurah by remember(surah.number) { mutableStateOf(surah) }
  var fromAyah by remember(surah.number, initialFrom) { mutableIntStateOf(initialFrom) }
  var toAyah by remember(surah.number, initialTo) { mutableIntStateOf(initialTo) }

  var includeTafsir by remember { mutableStateOf(false) }
  var useDarkCard by remember { mutableStateOf(initialDarkTheme) }
  var showSurahPicker by remember { mutableStateOf(false) }
  var editingAyahField by remember { mutableStateOf<String?>(null) } // "from" or "to"
  var showFullscreenImage by remember { mutableStateOf(false) }

  val seededAyahsBySurah = remember(surah.number, initialSorted) {
    mutableStateMapOf<Int, List<MushafAyah>>().apply {
      if (initialSorted.isNotEmpty()) {
        put(surah.number, initialSorted)
      }
    }
  }
  val fullSurahCache = remember { mutableStateMapOf<Int, List<MushafAyah>>() }
  var loadingSurahNum by remember { mutableStateOf<Int?>(null) }
  var loadErrorForSurah by remember { mutableStateOf<String?>(null) }
  var reloadTrigger by remember { mutableIntStateOf(0) }

  val availableAyahsForActiveSurah =
    fullSurahCache[selectedSurah.number] ?: seededAyahsBySurah[selectedSurah.number].orEmpty()

  val activeRangeAyahs =
    remember(availableAyahsForActiveSurah, fromAyah, toAyah) {
      val lo = minOf(fromAyah, toAyah)
      val hi = maxOf(fromAyah, toAyah)
      availableAyahsForActiveSurah
        .filter { it.numberInSurah in lo..hi }
        .sortedBy { it.numberInSurah }
    }

  val needsFullSurahFetch =
    remember(selectedSurah.number, fromAyah, toAyah, availableAyahsForActiveSurah, fullSurahCache.size) {
      if (fullSurahCache.containsKey(selectedSurah.number)) {
        false
      } else {
        val lo = minOf(fromAyah, toAyah)
        val hi = maxOf(fromAyah, toAyah)
        val expectedCount = (hi - lo + 1).coerceAtLeast(1)
        activeRangeAyahs.size < expectedCount
      }
    }

  LaunchedEffect(selectedSurah.number, needsFullSurahFetch, reloadTrigger) {
    if (!needsFullSurahFetch) {
      loadErrorForSurah = null
      return@LaunchedEffect
    }
    val loader = loadSurahAyahs
    if (loader == null) {
      loadErrorForSurah = null
      return@LaunchedEffect
    }
    loadingSurahNum = selectedSurah.number
    loadErrorForSurah = null
    val result = loader(selectedSurah.number)
    loadingSurahNum = null
    result.fold(
      onSuccess = { loaded -> fullSurahCache[selectedSurah.number] = loaded },
      onFailure = {
        loadErrorForSurah =
          "تعذر تحميل آيات سورة ${selectedSurah.nameAr}. تأكد من الاتصال بالإنترنت."
      },
    )
  }

  val previewBitmap =
    remember(selectedSurah.number, activeRangeAyahs, includeTafsir, useDarkCard) {
      if (activeRangeAyahs.isEmpty()) null
      else
        AyahShareCardGenerator.renderAyahShareBitmap(
          context = context,
          surah = selectedSurah,
          ayahs = activeRangeAyahs,
          includeTafsir = includeTafsir,
          useDarkTheme = useDarkCard,
        )
    }

  val legacyCreateDocumentLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
      val bmp = previewBitmap
      if (uri != null && bmp != null) {
        try {
          context.contentResolver.openOutputStream(uri)?.use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
          }
          onFeedback("تم حفظ الصورة في الهاتف بنجاح")
          onDismiss()
        } catch (_: Exception) {
          onShareFailure("تعذر حفظ الصورة في الهاتف")
        }
      }
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.background,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .navigationBarsPadding()
          .padding(horizontal = 18.dp)
          .padding(bottom = 20.dp)
          .testTag("ayah_share_image_dialog"),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Sheet Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "مشاركة الآيات",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text =
              if (activeRangeAyahs.isNotEmpty()) {
                AyahShareCardGenerator.buildAyahRangeLabel(selectedSurah, activeRangeAyahs)
              } else {
                "سورة ${selectedSurah.nameAr} • الآيات ${AyahShareCardGenerator.toArabicIndicDigits(fromAyah)} - ${AyahShareCardGenerator.toArabicIndicDigits(toAyah)}"
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier =
            Modifier.minimumInteractiveComponentSize().testTag("share_image_dialog_close"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "إغلاق نافذة المشاركة",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Segmented Mode Tab Switcher: "صورة" vs "نص"
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ShareModeTabButton(
            title = AyahShareTab.IMAGE.labelAr,
            icon = Icons.Rounded.Image,
            selected = activeTab == AyahShareTab.IMAGE,
            onClick = { activeTab = AyahShareTab.IMAGE },
            modifier = Modifier.weight(1f).testTag("choose_share_as_image"),
          )
          ShareModeTabButton(
            title = AyahShareTab.TEXT.labelAr,
            icon = Icons.Rounded.TextFields,
            selected = activeTab == AyahShareTab.TEXT,
            onClick = { activeTab = AyahShareTab.TEXT },
            modifier = Modifier.weight(1f).testTag("choose_share_as_text"),
          )
        }
      }

      // Surah & Ayah Range Picker Card
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          // Surah Selector
          Surface(
            onClick = { showSurahPicker = true },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().testTag("share_surah_selector"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp),
                )
                Column {
                  Text(
                    text = "السورة المختارة",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text =
                      "سورة ${selectedSurah.nameAr} (${AyahShareCardGenerator.toArabicIndicDigits(selectedSurah.ayahCount)} آية)",
                    style =
                      MaterialTheme.typography.titleMedium.copy(
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.Bold,
                      ),
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
              Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = "تغيير السورة",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }

          // "من آية" -> "إلى آية" Range Controls
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            AyahStepperControl(
              label = "من آية",
              value = fromAyah,
              canDecrement = fromAyah > 1,
              canIncrement = fromAyah < selectedSurah.ayahCount,
              onDecrement = {
                val next = (fromAyah - 1).coerceAtLeast(1)
                fromAyah = next
                if (toAyah < next) toAyah = next
              },
              onIncrement = {
                val next = (fromAyah + 1).coerceAtMost(selectedSurah.ayahCount)
                fromAyah = next
                if (toAyah < next) toAyah = next
              },
              onValueClick = { editingAyahField = "from" },
              decTag = "share_from_ayah_dec",
              valueTag = "share_from_ayah_value",
              incTag = "share_from_ayah_inc",
              modifier = Modifier.weight(1f),
            )

            AyahStepperControl(
              label = "إلى آية",
              value = toAyah,
              canDecrement = toAyah > fromAyah,
              canIncrement = toAyah < selectedSurah.ayahCount,
              onDecrement = {
                toAyah = (toAyah - 1).coerceAtLeast(fromAyah)
              },
              onIncrement = {
                toAyah = (toAyah + 1).coerceAtMost(selectedSurah.ayahCount)
              },
              onValueClick = { editingAyahField = "to" },
              decTag = "share_to_ayah_dec",
              valueTag = "share_to_ayah_value",
              incTag = "share_to_ayah_inc",
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // Loading or Error feedback if loading new Surah/Ayah range
      if (loadingSurahNum == selectedSurah.number) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              strokeWidth = 2.5.dp,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "جارٍ تجهيز آيات سورة ${selectedSurah.nameAr}...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else if (loadErrorForSurah != null && activeRangeAyahs.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = loadErrorForSurah ?: "",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center,
            )
            OutlinedButton(
              onClick = { reloadTrigger++ },
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
      }

      // Tab Content
      if (activeTab == AyahShareTab.IMAGE) {
        // Card Bitmap Preview
        if (previewBitmap != null) {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Image(
              bitmap = previewBitmap.asImageBitmap(),
              contentDescription = "معاينة بطاقة الآية للمشاركة",
              contentScale = ContentScale.FillWidth,
              modifier =
                Modifier.fillMaxWidth()
                  .heightIn(max = 360.dp)
                  .clip(RoundedCornerShape(18.dp))
                  .clickable { showFullscreenImage = true }
                  .testTag("ayah_share_preview_image"),
            )
          }
        }

        // Card Background Switcher (Light Mushaf Paper / Dark Calm Night)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FilterChip(
            selected = !useDarkCard,
            onClick = { useDarkCard = false },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.LightMode,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
            },
            label = { Text("خلفية فاتحة") },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
              ),
            modifier = Modifier.weight(1f).testTag("share_theme_light_chip"),
          )
          FilterChip(
            selected = useDarkCard,
            onClick = { useDarkCard = true },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.DarkMode,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
            },
            label = { Text("خلفية داكنة") },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
              ),
            modifier = Modifier.weight(1f).testTag("share_theme_dark_chip"),
          )
        }

        // Tafsir Toggle Card
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = "تضمين التفسير الميسر",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "إظهار شرح الآيات المحددة داخل البطاقة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = includeTafsir,
              onCheckedChange = { includeTafsir = it },
              modifier = Modifier.testTag("share_image_include_tafsir_switch"),
            )
          }
        }

        // Save to Phone + Share Image Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = {
              if (activeRangeAyahs.isEmpty()) return@OutlinedButton
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val saved =
                  AyahShareCardGenerator.saveAyahImageToGalleryApi29Plus(
                    context = context,
                    surah = selectedSurah,
                    ayahs = activeRangeAyahs,
                    includeTafsir = includeTafsir,
                    useDarkTheme = useDarkCard,
                  )
                if (saved) {
                  onFeedback("تم حفظ الصورة في معرض الصور")
                  onDismiss()
                } else {
                  onShareFailure("تعذر حفظ الصورة في الهاتف")
                }
              } else {
                val firstAyahNum = activeRangeAyahs.minOfOrNull { it.numberInSurah } ?: 1
                legacyCreateDocumentLauncher.launch(
                  "tilawah_${selectedSurah.number}_$firstAyahNum.png"
                )
              }
            },
            enabled = activeRangeAyahs.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(48.dp).testTag("save_ayah_image_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Download,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("حفظ في الهاتف")
          }

          Button(
            onClick = {
              if (activeRangeAyahs.isEmpty()) return@Button
              val shared =
                AyahShareCardGenerator.shareAyahImage(
                  context = context,
                  surah = selectedSurah,
                  ayahs = activeRangeAyahs,
                  includeTafsir = includeTafsir,
                  useDarkTheme = useDarkCard,
                )
              if (shared) {
                onDismiss()
              } else {
                onShareFailure("تعذر تجهيز الصورة للمشاركة")
              }
            },
            enabled = activeRangeAyahs.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.weight(1f).height(48.dp).testTag("confirm_share_ayah_image_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Share,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("مشاركة الصورة")
          }
        }
      } else {
        // TEXT TAB: Formatted text preview + Tafsir toggle + Copy / Share Text buttons
        val formattedText =
          remember(selectedSurah.number, activeRangeAyahs, includeTafsir) {
            if (activeRangeAyahs.isEmpty()) ""
            else
              AyahShareCardGenerator.buildShareTextFormatted(
                surah = selectedSurah,
                ayahs = activeRangeAyahs,
                includeTafsir = includeTafsir,
              )
          }

        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
          modifier = Modifier.fillMaxWidth().testTag("ayah_share_text_preview"),
        ) {
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text =
                if (formattedText.isNotEmpty()) formattedText
                else "جارٍ تجهيز نص الآيات...",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = "تضمين التفسير الميسر",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "إرفاق التفسير الميسر أسفل نص الآيات",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = includeTafsir,
              onCheckedChange = { includeTafsir = it },
              modifier = Modifier.testTag("share_image_include_tafsir_switch"),
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = {
              if (formattedText.isNotEmpty()) {
                clipboardManager.setText(AnnotatedString(formattedText))
                onFeedback("تم نسخ نص الآيات بنجاح")
                onDismiss()
              }
            },
            enabled = formattedText.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(48.dp).testTag("copy_ayah_text_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.ContentCopy,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("نسخ النص")
          }

          Button(
            onClick = {
              if (activeRangeAyahs.isEmpty()) return@Button
              val shared =
                AyahShareCardGenerator.shareAyahText(
                  context = context,
                  surah = selectedSurah,
                  ayahs = activeRangeAyahs,
                  includeTafsir = includeTafsir,
                )
              if (shared) {
                onDismiss()
              } else {
                onShareFailure("تعذر مشاركة النص")
              }
            },
            enabled = activeRangeAyahs.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.weight(1f).height(48.dp).testTag("confirm_share_ayah_text_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Share,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("مشاركة النص")
          }
        }
      }
    }
  }

  // Surah Picker Dialog
  if (showSurahPicker) {
    ShareSurahPickerDialog(
      currentSurahNumber = selectedSurah.number,
      onSelectSurah = { chosen ->
        showSurahPicker = false
        if (chosen.number != selectedSurah.number) {
          selectedSurah = chosen
          fromAyah = 1
          toAyah = 1
        }
      },
      onDismiss = { showSurahPicker = false },
    )
  }

  // Direct Ayah Number Entry Dialog (helpful for long Surahs)
  val activeEdit = editingAyahField
  if (activeEdit != null) {
    val isFrom = activeEdit == "from"
    AyahNumberInputDialog(
      title = if (isFrom) "تحديد بداية النطاق (من آية)" else "تحديد نهاية النطاق (إلى آية)",
      initialValue = if (isFrom) fromAyah else toAyah,
      minAllowed = if (isFrom) 1 else fromAyah,
      maxAllowed = selectedSurah.ayahCount,
      onConfirm = { chosen ->
        if (isFrom) {
          fromAyah = chosen
          if (toAyah < chosen) toAyah = chosen
        } else {
          toAyah = chosen.coerceAtLeast(fromAyah)
        }
        editingAyahField = null
      },
      onDismiss = { editingAyahField = null },
    )
  }

  // Enlarged Fullscreen Card Preview on Image Tap
  if (showFullscreenImage && previewBitmap != null) {
    Dialog(
      onDismissRequest = { showFullscreenImage = false },
      properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
      Surface(
        color = Color.Black.copy(alpha = 0.88f),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(22.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
          ) {
            IconButton(
              onClick = { showFullscreenImage = false },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "إغلاق التكبير",
                tint = Color.White,
              )
            }
          }
          Image(
            bitmap = previewBitmap.asImageBitmap(),
            contentDescription = "عرض البطاقة بالحجم الكامل",
            contentScale = ContentScale.FillWidth,
            modifier =
              Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(16.dp)),
          )
        }
      }
    }
  }
}

@Composable
private fun ShareModeTabButton(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
    modifier = modifier.height(44.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint =
          if (selected) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color =
          if (selected) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun AyahStepperControl(
  label: String,
  value: Int,
  canDecrement: Boolean,
  canIncrement: Boolean,
  onDecrement: () -> Unit,
  onIncrement: () -> Unit,
  onValueClick: () -> Unit,
  decTag: String,
  valueTag: String,
  incTag: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onDecrement,
          enabled = canDecrement,
          modifier = Modifier.minimumInteractiveComponentSize().testTag(decTag),
        ) {
          Icon(
            imageVector = Icons.Rounded.Remove,
            contentDescription = "إنقاص $label",
            tint =
              if (canDecrement) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
          )
        }

        Surface(
          onClick = onValueClick,
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
          modifier = Modifier.testTag(valueTag),
        ) {
          Text(
            text = AyahShareCardGenerator.toArabicIndicDigits(value),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }

        IconButton(
          onClick = onIncrement,
          enabled = canIncrement,
          modifier = Modifier.minimumInteractiveComponentSize().testTag(incTag),
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "زيادة $label",
            tint =
              if (canIncrement) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
          )
        }
      }
    }
  }
}

@Composable
private fun ShareSurahPickerDialog(
  currentSurahNumber: Int,
  onSelectSurah: (Surah) -> Unit,
  onDismiss: () -> Unit,
) {
  var query by remember { mutableStateOf("") }
  val filtered =
    remember(query) {
      val norm = QuranCatalog.normalizeArabic(query)
      if (norm.isEmpty()) QuranCatalog.surahs
      else
        QuranCatalog.surahs.filter {
          QuranCatalog.normalizeArabic(it.nameAr).contains(norm) ||
            it.number.toString().contains(norm)
        }
    }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.background,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
      modifier = Modifier.fillMaxWidth().testTag("share_surah_picker_dialog"),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "اختر السورة",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = "إغلاق")
          }
        }

        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          placeholder = { Text("ابحث باسم السورة أو رقمها...") },
          leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("share_surah_search_input"),
        )

        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          contentPadding = PaddingValues(bottom = 8.dp),
        ) {
          items(filtered, key = { it.number }) { item ->
            val isSelected = item.number == currentSurahNumber
            Card(
              onClick = { onSelectSurah(item) },
              shape = RoundedCornerShape(14.dp),
              colors =
                CardDefaults.cardColors(
                  containerColor =
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
              border =
                BorderStroke(
                  width = if (isSelected) 1.4.dp else 1.dp,
                  color =
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                ),
              modifier = Modifier.fillMaxWidth().testTag("share_pick_surah_${item.number}"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = AyahShareCardGenerator.toArabicIndicDigits(item.number),
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                  Text(
                    text = "سورة ${item.nameAr}",
                    style =
                      MaterialTheme.typography.titleMedium.copy(
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.Bold,
                      ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Text(
                  text = "${AyahShareCardGenerator.toArabicIndicDigits(item.ayahCount)} آية",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AyahNumberInputDialog(
  title: String,
  initialValue: Int,
  minAllowed: Int,
  maxAllowed: Int,
  onConfirm: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  var textValue by remember { mutableStateOf(initialValue.toString()) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  val trySubmit: () -> Unit = {
    val parsed = textValue.trim().toIntOrNull()
    if (parsed != null && parsed in minAllowed..maxAllowed) {
      onConfirm(parsed)
    } else {
      errorMsg = "أدخل رقماً من $minAllowed إلى $maxAllowed"
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
          value = textValue,
          onValueChange = {
            textValue = it.filter { ch -> ch.isDigit() }.take(3)
            errorMsg = null
          },
          label = { Text("رقم الآية ($minAllowed - $maxAllowed)") },
          isError = errorMsg != null,
          supportingText = errorMsg?.let { msg -> { Text(msg) } },
          singleLine = true,
          keyboardOptions =
            KeyboardOptions(
              keyboardType = KeyboardType.Number,
              imeAction = ImeAction.Done,
            ),
          keyboardActions = KeyboardActions(onDone = { trySubmit() }),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) { Text("إلغاء") }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = trySubmit,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          ) {
            Text("تأكيد")
          }
        }
      }
    }
  }
}
