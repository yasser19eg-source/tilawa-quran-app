package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdhkarCatalog
import com.example.model.DhikrCategory
import com.example.model.DhikrItem
import com.example.ui.theme.AmiriFontFamily

@Composable
fun AdhkarScreen(
  adhkarCounts: Map<String, Int>,
  onIncrementDhikr: (DhikrItem) -> Unit,
  onResetCategory: (DhikrCategory) -> Unit,
  onShowFeedback: (String) -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)

  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val haptic = LocalHapticFeedback.current

  var selectedCategory by rememberSaveable { mutableStateOf(DhikrCategory.MORNING) }
  val categoryItems =
    remember(selectedCategory) { AdhkarCatalog.itemsForCategory(selectedCategory) }
  val completedCount =
    remember(categoryItems, adhkarCounts) {
      categoryItems.count { (adhkarCounts[it.id] ?: 0) >= it.targetCount }
    }
  val progress =
    if (categoryItems.isNotEmpty()) completedCount.toFloat() / categoryItems.size.toFloat()
    else 0f

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("adhkar_screen_list"),
      contentPadding = PaddingValues(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Header Summary Card
      item(key = "adhkar_header") {
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
                modifier = Modifier.testTag("adhkar_back_button"),
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
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "حصن المسلم اليومي",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = selectedCategory.titleAr,
              style =
                MaterialTheme.typography.headlineSmall.copy(
                  fontFamily = AmiriFontFamily,
                  fontWeight = FontWeight.Bold,
                ),
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = selectedCategory.subtitleAr,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "المنجز: $completedCount من ${categoryItems.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("adhkar_progress_label"),
              )
              TextButton(
                onClick = { onResetCategory(selectedCategory) },
                modifier = Modifier.testTag("adhkar_reset_category_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Refresh,
                  contentDescription = "إعادة العداد",
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("إعادة العد")
              }
            }

            LinearProgressIndicator(
              progress = { progress },
              modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
          }
        }
      }

      // Categories Selector Row
      item(key = "adhkar_categories") {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(DhikrCategory.entries, key = { it.id }) { category ->
            val selected = category == selectedCategory
            FilterChip(
              selected = selected,
              onClick = { selectedCategory = category },
              label = { Text(category.titleAr, style = MaterialTheme.typography.labelLarge) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
              modifier = Modifier.testTag("adhkar_category_${category.id}"),
            )
          }
        }
      }

      // Adhkar Cards
      items(categoryItems, key = { it.id }) { dhikr ->
        val currentCount = adhkarCounts[dhikr.id] ?: 0
        DhikrCard(
          dhikr = dhikr,
          currentCount = currentCount,
          onIncrement = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onIncrementDhikr(dhikr)
          },
          onCopy = {
            val formatted = "${dhikr.titleAr}\n${dhikr.textAr}\n(${dhikr.sourceAr})"
            clipboardManager.setText(AnnotatedString(formatted))
            onShowFeedback("تم نسخ الذكر إلى الحافظة")
          },
          onShare = {
            val shareText =
              "${dhikr.titleAr}\n\n${dhikr.textAr}\n\nالمصدر: ${dhikr.sourceAr}\n— تطبيق تلاوة"
            val sendIntent =
              Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
              }
            context.startActivity(Intent.createChooser(sendIntent, "مشاركة الذكر"))
          },
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
    }
  }
}

@Composable
private fun DhikrCard(
  dhikr: DhikrItem,
  currentCount: Int,
  onIncrement: () -> Unit,
  onCopy: () -> Unit,
  onShare: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDone = currentCount >= dhikr.targetCount
  val remaining = (dhikr.targetCount - currentCount).coerceAtLeast(0)
  val borderColor by
    animateColorAsState(
      targetValue =
        if (isDone) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
      label = "dhikr_card_border",
    )
  val containerColor by
    animateColorAsState(
      targetValue =
        if (isDone) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.surface,
      label = "dhikr_card_bg",
    )

  Card(
    modifier = modifier.fillMaxWidth().testTag("dhikr_card_${dhikr.id}"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(1.dp, borderColor),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = dhikr.titleAr,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = dhikr.sourceAr,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onCopy,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("dhikr_copy_${dhikr.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.ContentCopy,
              contentDescription = "نسخ الذكر",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
          }
          IconButton(
            onClick = onShare,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("dhikr_share_${dhikr.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Share,
              contentDescription = "مشاركة الذكر",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = dhikr.textAr,
        style =
          MaterialTheme.typography.bodyLarge.copy(
            fontFamily = AmiriFontFamily,
            fontSize = 21.sp,
            lineHeight = 36.sp,
          ),
        color = MaterialTheme.colorScheme.onSurface,
      )

      if (!dhikr.virtueAr.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = dhikr.virtueAr,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        ) {
          Text(
            text = "التكرار المطلوب: ${dhikr.targetCount}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }

        Button(
          onClick = onIncrement,
          enabled = !isDone,
          shape = RoundedCornerShape(14.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
              disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          modifier = Modifier.testTag("dhikr_counter_${dhikr.id}"),
        ) {
          Icon(
            imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.TouchApp,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text =
              if (isDone) "تم الذكر ✓ ($currentCount/${dhikr.targetCount})"
              else "$currentCount / ${dhikr.targetCount} (متبقي $remaining)",
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}
