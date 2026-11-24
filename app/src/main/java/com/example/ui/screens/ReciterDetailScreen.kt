package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.QuranCatalog
import com.example.model.ActiveAudioTarget
import com.example.model.PlaybackUiState
import com.example.model.Reciter
import com.example.model.Riwayah
import com.example.model.Surah
import com.example.ui.components.QuranMedia3PlayerCard
import com.example.ui.components.ReciterNameWithMic
import com.example.ui.components.ReciterPortrait
import com.example.ui.theme.AmiriFontFamily

@Composable
fun ReciterDetailScreen(
  reciter: Reciter,
  portraitUrl: String?,
  portraits: Map<String, String?>,
  isFavorite: Boolean,
  selectedRiwayahIndex: Int,
  surahSearchQuery: String,
  playbackState: PlaybackUiState,
  onRequestPortrait: (Reciter) -> Unit,
  onBackClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onSelectRiwayah: (Int) -> Unit,
  onSurahSearchChange: (String) -> Unit,
  onPlaySurah: (Riwayah, Surah) -> Unit,
  onTogglePlayPause: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onSeekBy: (Long) -> Unit,
  onPreviousSurah: () -> Unit,
  onNextSurah: () -> Unit,
  onExpandPlayer: () -> Unit,
  onStopPlayback: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)
  LaunchedEffect(reciter.id) { onRequestPortrait(reciter) }

  val activeRiwayah = reciter.riwayat.getOrElse(selectedRiwayahIndex) { reciter.riwayat.first() }
  val availableSurahs =
    remember(activeRiwayah) {
      QuranCatalog.surahs.filter { activeRiwayah.isSurahListed(it.number) }
    }
  val filteredSurahs =
    remember(surahSearchQuery, availableSurahs) {
      val normalized = QuranCatalog.normalizeArabic(surahSearchQuery)
      if (normalized.isEmpty()) {
        availableSurahs
      } else {
        availableSurahs.filter { surah ->
          QuranCatalog.normalizeArabic(surah.nameAr).contains(normalized) ||
            surah.number.toString().contains(normalized) ||
            surah.paddedNumber.contains(normalized)
        }
      }
    }

  val activeSurahTarget = playbackState.activeTarget as? ActiveAudioTarget.SurahTrack

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
      contentPadding = PaddingValues(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Top navigation & Reciter Header Card
      item(key = "reciter_header") {
        Card(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("reciter_detail_previous_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = "السابق",
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("السابق")
              }

              IconButton(
                onClick = onToggleFavorite,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("detail_favorite_button"),
              ) {
                Icon(
                  imageVector =
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                  contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                  tint =
                    if (isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ReciterPortrait(
                portraitUrl = portraitUrl,
                contentDescription = reciter.nameAr,
                size = 84.dp,
                isCircular = true,
              )
              Spacer(modifier = Modifier.width(16.dp))
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                ReciterNameWithMic(
                  name = reciter.nameAr,
                  portraitUrl = portraitUrl,
                  style = MaterialTheme.typography.headlineSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 2,
                )
                Text(
                  text = "${activeRiwayah.nameAr} • ${availableSurahs.size} سورة",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "الرواية المتاحة",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(reciter.riwayat.indices.toList()) { index ->
                val riwayah = reciter.riwayat[index]
                val selected = index == selectedRiwayahIndex
                FilterChip(
                  selected = selected,
                  onClick = { onSelectRiwayah(index) },
                  label = { Text(riwayah.nameAr) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                      selectedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                  modifier = Modifier.testTag("riwayah_chip_$index"),
                )
              }
            }
          }
        }
      }

      // Inline Media3 Audio Player Component when a Surah is active
      if (activeSurahTarget != null) {
        item(key = "inline_media3_player") {
          QuranMedia3PlayerCard(
            playbackState = playbackState,
            portraits = portraits,
            onTogglePlayPause = onTogglePlayPause,
            onSeekTo = onSeekTo,
            onSeekBy = onSeekBy,
            onPreviousSurah = onPreviousSurah,
            onNextSurah = onNextSurah,
            onExpandPlayer = onExpandPlayer,
            onStop = onStopPlayback,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
          )
        }
      }

      // Surah Search Input
      item(key = "surah_search") {
        OutlinedTextField(
          value = surahSearchQuery,
          onValueChange = onSurahSearchChange,
          placeholder = { Text("ابحث باسم السورة أو رقمها (مثلاً: الكهف أو 18)...") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = "بحث عن سورة",
              tint = MaterialTheme.colorScheme.primary,
            )
          },
          trailingIcon = {
            if (surahSearchQuery.isNotEmpty()) {
              IconButton(
                onClick = { onSurahSearchChange("") },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(imageVector = Icons.Rounded.Clear, contentDescription = "مسح")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 2.dp)
              .testTag("surah_search_input"),
        )
      }

      // 114 Surahs List
      items(filteredSurahs, key = { it.number }) { surah ->
        val trackKey = "${reciter.id}|${activeRiwayah.baseUrl}|${surah.number}"
        val isUnavailable = playbackState.unavailableTrackKeys.contains(trackKey)
        val isCurrentSurah =
          activeSurahTarget?.reciter?.id == reciter.id &&
            activeSurahTarget.riwayah.baseUrl == activeRiwayah.baseUrl &&
            activeSurahTarget.surah.number == surah.number

        SurahRowCard(
          surah = surah,
          isCurrentTrack = isCurrentSurah,
          isPlaying = isCurrentSurah && playbackState.isPlaying,
          isBuffering = isCurrentSurah && playbackState.isBuffering,
          isUnavailable = isUnavailable,
          onClick = {
            if (isCurrentSurah) {
              onTogglePlayPause()
            } else {
              onPlaySurah(activeRiwayah, surah)
            }
          },
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
    }
  }
}

@Composable
private fun SurahRowCard(
  surah: Surah,
  isCurrentTrack: Boolean,
  isPlaying: Boolean,
  isBuffering: Boolean,
  isUnavailable: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor =
    when {
      isUnavailable -> MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
      isCurrentTrack -> MaterialTheme.colorScheme.primary
      else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("surah_row_${surah.number}")
        .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isCurrentTrack) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
          else MaterialTheme.colorScheme.surface
      ),
    border = BorderStroke(1.dp, borderColor),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
              if (isCurrentTrack) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = surah.paddedNumber,
          style = MaterialTheme.typography.labelLarge,
          color =
            if (isCurrentTrack) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "سورة ${surah.nameAr}",
          style = MaterialTheme.typography.titleMedium.copy(fontFamily = AmiriFontFamily),
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (isUnavailable) {
          Text(
            text = "غير متوفر حالياً على الخادم",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        } else {
          Text(
            text = "${surah.revelationTypeAr} • ${surah.ayahCount} آية",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Box(
        modifier =
          Modifier.size(44.dp)
            .clip(CircleShape)
            .background(
              if (isUnavailable) MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
              else MaterialTheme.colorScheme.primaryContainer
            ),
        contentAlignment = Alignment.Center,
      ) {
        when {
          isUnavailable -> {
            Icon(
              imageVector = Icons.Rounded.ErrorOutline,
              contentDescription = "غير متوفر",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(22.dp),
            )
          }
          isBuffering -> {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.5.dp,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          isPlaying -> {
            Icon(
              imageVector = Icons.Rounded.Pause,
              contentDescription = "إيقاف مؤقت",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp),
            )
          }
          isCurrentTrack -> {
            Icon(
              imageVector = Icons.Rounded.GraphicEq,
              contentDescription = "محدد حالياً",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp),
            )
          }
          else -> {
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = "تشغيل سورة ${surah.nameAr}",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp),
            )
          }
        }
      }
    }
  }
}
