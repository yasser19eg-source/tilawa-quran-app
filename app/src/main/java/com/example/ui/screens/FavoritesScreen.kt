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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.QuranCatalog
import com.example.model.Reciter
import com.example.ui.components.ReciterNameWithMic
import com.example.ui.components.ReciterPortrait
import com.example.ui.theme.AmiriFontFamily

@Composable
fun FavoritesScreen(
  reciters: List<Reciter>,
  favoriteReciterIds: Set<String>,
  portraits: Map<String, String?>,
  onRequestPortrait: (Reciter) -> Unit,
  onBackClick: () -> Unit,
  onToggleFavorite: (String) -> Unit,
  onReciterClick: (Reciter) -> Unit,
  onBrowseRecitersClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)

  var localSearchQuery by rememberSaveable { mutableStateOf("") }

  val favoriteReciters =
    remember(reciters, favoriteReciterIds) {
      reciters.filter { favoriteReciterIds.contains(it.id) }
    }

  val filteredFavorites =
    remember(favoriteReciters, localSearchQuery) {
      val normalized = QuranCatalog.normalizeArabic(localSearchQuery)
      if (normalized.isEmpty()) {
        favoriteReciters
      } else {
        favoriteReciters.filter { reciter ->
          QuranCatalog.normalizeArabic(reciter.nameAr).contains(normalized) ||
            reciter.riwayat.any { QuranCatalog.normalizeArabic(it.nameAr).contains(normalized) }
        }
      }
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 840.dp)) {
      // Top Hero Summary Card
      Card(
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("favorites_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                Brush.horizontalGradient(
                  colors =
                    listOf(
                      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                      MaterialTheme.colorScheme.surface,
                    )
                )
              )
              .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
          ) {
            Box(
              modifier =
                Modifier.size(46.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
              )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "قرائي المفضلون",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = AmiriFontFamily),
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  if (favoriteReciters.isEmpty()) "احفظ قراءك المفضلين للوصول السريع"
                  else "${favoriteReciters.size} قارئ محفوظ على جهازك",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          OutlinedButton(
            onClick = onBackClick,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("favorites_previous_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "السابق",
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("السابق", style = MaterialTheme.typography.labelLarge)
          }
        }
      }

      if (favoriteReciters.size > 3) {
        OutlinedTextField(
          value = localSearchQuery,
          onValueChange = { localSearchQuery = it },
          placeholder = { Text("ابحث في القراء المفضلين...") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = "بحث",
              tint = MaterialTheme.colorScheme.primary,
            )
          },
          trailingIcon = {
            if (localSearchQuery.isNotEmpty()) {
              IconButton(
                onClick = { localSearchQuery = "" },
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
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 4.dp)
              .testTag("favorites_search_input"),
        )
      }

      if (favoriteReciters.isEmpty()) {
        FavoritesEmptyState(
          onBrowseRecitersClick = onBrowseRecitersClick,
          modifier = Modifier.fillMaxSize().padding(24.dp),
        )
      } else if (filteredFavorites.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxSize().padding(32.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "لا يوجد قارئ مفضل مطابق لبحثك",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(filteredFavorites, key = { it.id }) { reciter ->
            LaunchedEffect(reciter.id) { onRequestPortrait(reciter) }
            FavoriteReciterListCard(
              reciter = reciter,
              portraitUrl = portraits[reciter.id],
              onToggleFavorite = { onToggleFavorite(reciter.id) },
              onClick = { onReciterClick(reciter) },
              modifier = Modifier.animateItem(),
            )
          }
          item { Spacer(modifier = Modifier.height(16.dp)) }
        }
      }
    }
  }
}

@Composable
private fun FavoriteReciterListCard(
  reciter: Reciter,
  portraitUrl: String?,
  onToggleFavorite: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("favorite_reciter_card_${reciter.id}")
        .clickable(onClick = onClick),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
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
        modifier =
          Modifier.minimumInteractiveComponentSize().testTag("favorites_screen_toggle_${reciter.id}"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Favorite,
          contentDescription = "إزالة من المفضلة",
          tint = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun FavoritesEmptyState(
  onBrowseRecitersClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
      modifier = Modifier.fillMaxWidth().testTag("favorites_empty_state"),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Box(
          modifier =
            Modifier.size(72.dp)
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  colors =
                    listOf(
                      MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                      MaterialTheme.colorScheme.primaryContainer,
                    )
                )
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.FavoriteBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(34.dp),
          )
        }
        Text(
          text = "قائمة المفضلة فارغة",
          style = MaterialTheme.typography.titleLarge.copy(fontFamily = AmiriFontFamily),
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
        )
        Text(
          text =
            "اضغط على أيقونة القلب في بطاقة أي قارئ لإضافته إلى مفضلتك والوصول إليه بسرعة في أي وقت.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        Button(
          onClick = onBrowseRecitersClick,
          shape = RoundedCornerShape(50),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier = Modifier.padding(top = 4.dp).testTag("favorites_browse_reciters_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.People,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("تصفح القراء", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
