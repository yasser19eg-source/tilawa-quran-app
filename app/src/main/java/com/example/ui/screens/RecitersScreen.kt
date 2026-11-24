package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.QuranCatalog
import com.example.model.Reciter
import com.example.model.ReciterFilter
import com.example.ui.components.ReciterNameWithMic
import com.example.ui.components.ReciterPortrait

@Composable
fun RecitersScreen(
  reciters: List<Reciter>,
  isCatalogSyncing: Boolean,
  searchQuery: String,
  selectedFilter: ReciterFilter,
  favoriteReciterIds: Set<String>,
  recentSearches: List<String>,
  portraits: Map<String, String?>,
  onRequestPortrait: (Reciter) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onSubmitSearch: () -> Unit,
  onSelectFilter: (ReciterFilter) -> Unit,
  onApplyRecentSearch: (String) -> Unit,
  onRemoveRecentSearch: (String) -> Unit,
  onClearRecentSearches: () -> Unit,
  onToggleFavorite: (String) -> Unit,
  onReciterClick: (Reciter) -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  val filteredReciters =
    remember(reciters, searchQuery, selectedFilter, favoriteReciterIds) {
      val normalizedQuery = QuranCatalog.normalizeArabic(searchQuery)
      reciters.filter { reciter ->
        val matchesFilter =
          when (selectedFilter) {
            ReciterFilter.ALL -> true
            ReciterFilter.FAVORITES -> favoriteReciterIds.contains(reciter.id)
            ReciterFilter.HAFS -> reciter.riwayat.any { it.nameAr.contains("حفص") }
            ReciterFilter.WARSH -> reciter.riwayat.any { it.nameAr.contains("ورش") }
            ReciterFilter.QALON_DORI ->
              reciter.riwayat.any {
                it.nameAr.contains("قالون") || it.nameAr.contains("الدوري")
              }
            ReciterFilter.MOJAWWAD ->
              reciter.riwayat.any {
                it.nameAr.contains("مجود") || it.nameAr.contains("المجود")
              }
          }
        val matchesQuery =
          normalizedQuery.isEmpty() ||
            QuranCatalog.normalizeArabic(reciter.nameAr).contains(normalizedQuery) ||
            reciter.riwayat.any {
              QuranCatalog.normalizeArabic(it.nameAr).contains(normalizedQuery)
            }
        matchesFilter && matchesQuery
      }
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 840.dp)) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("ابحث عن قارئ أو رواية...") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = "بحث",
            tint = MaterialTheme.colorScheme.primary,
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { onSearchQueryChange("") },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Rounded.Clear,
                contentDescription = "مسح البحث",
              )
            }
          }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions =
          KeyboardActions(
            onSearch = {
              onSubmitSearch()
              focusManager.clearFocus()
            }
          ),
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("reciters_search_input"),
      )

      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(ReciterFilter.entries) { filter ->
          val isSelected = selectedFilter == filter
          FilterChip(
            selected = isSelected,
            onClick = { onSelectFilter(filter) },
            label = { Text(filter.labelAr) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.primary,
              ),
            modifier = Modifier.testTag("reciter_filter_${filter.name}"),
          )
        }
      }

      if (recentSearches.isNotEmpty() && searchQuery.isEmpty()) {
        RecentSearchesSection(
          recentSearches = recentSearches,
          onApplyRecentSearch = { term ->
            focusManager.clearFocus()
            onApplyRecentSearch(term)
          },
          onRemoveRecentSearch = onRemoveRecentSearch,
          onClearAll = onClearRecentSearches,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "عدد القراء: ${filteredReciters.size}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("reciters_count_label"),
        )
        if (isCatalogSyncing) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(14.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "جاري تحديث المكتبة...",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      if (filteredReciters.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxSize().padding(32.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.MicOff,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(48.dp),
            )
            Text(
              text =
                if (selectedFilter == ReciterFilter.FAVORITES)
                  "لا يوجد قراء في المفضلة بعد"
                else "لم يتم العثور على قارئ مطابق",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(filteredReciters, key = { it.id }) { reciter ->
            LaunchedEffect(reciter.id) { onRequestPortrait(reciter) }
            val isFavorite = favoriteReciterIds.contains(reciter.id)
            ReciterDirectoryCard(
              reciter = reciter,
              portraitUrl = portraits[reciter.id],
              isFavorite = isFavorite,
              onToggleFavorite = { onToggleFavorite(reciter.id) },
              onClick = { onReciterClick(reciter) },
            )
          }
          item { Spacer(modifier = Modifier.height(16.dp)) }
        }
      }
    }
  }
}

@Composable
private fun ReciterDirectoryCard(
  reciter: Reciter,
  portraitUrl: String?,
  isFavorite: Boolean,
  onToggleFavorite: () -> Unit,
  onClick: () -> Unit,
) {
  Card(
    modifier =
      Modifier.fillMaxWidth()
        .testTag("reciter_list_card_${reciter.id}")
        .clickable(onClick = onClick),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border =
      BorderStroke(
        1.dp,
        if (isFavorite) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ReciterPortrait(
        portraitUrl = portraitUrl,
        contentDescription = reciter.nameAr,
        size = 64.dp,
        isCircular = true,
      )
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ReciterNameWithMic(
          name = reciter.nameAr,
          portraitUrl = portraitUrl,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val primaryRiwayah = reciter.riwayat.first()
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = primaryRiwayah.nameAr,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
          Text(
            text =
              if (reciter.riwayat.size > 1) "${reciter.riwayat.size} روايات متاحة"
              else "${primaryRiwayah.availableSurahs?.size ?: 114} سورة",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      IconButton(
        onClick = onToggleFavorite,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("favorite_btn_${reciter.id}"),
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
  }
}

@Composable
private fun RecentSearchesSection(
  recentSearches: List<String>,
  onApplyRecentSearch: (String) -> Unit,
  onRemoveRecentSearch: (String) -> Unit,
  onClearAll: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("recent_searches_section"),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.History,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = "عمليات البحث الأخيرة",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      TextButton(
        onClick = onClearAll,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = Modifier.testTag("clear_recent_searches_button"),
      ) {
        Text(
          text = "مسح الكل",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(recentSearches.size, key = { idx -> recentSearches[idx] }) { index ->
        val term = recentSearches[index]
        InputChip(
          selected = false,
          onClick = { onApplyRecentSearch(term) },
          label = { Text(term, style = MaterialTheme.typography.labelMedium) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.History,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
          },
          trailingIcon = {
            IconButton(
              onClick = { onRemoveRecentSearch(term) },
              modifier = Modifier.size(22.dp).testTag("remove_recent_search_$index"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "حذف $term من البحث الأخير",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
              )
            }
          },
          colors =
            InputChipDefaults.inputChipColors(
              containerColor = MaterialTheme.colorScheme.surface,
              labelColor = MaterialTheme.colorScheme.onSurface,
            ),
          border =
            InputChipDefaults.inputChipBorder(
              enabled = true,
              selected = false,
              borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            ),
          modifier = Modifier.testTag("recent_search_chip_$index"),
        )
      }
    }
  }
}
