package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.LastPlayedRecord
import com.example.data.QuranCatalog
import com.example.data.UserPreferencesState
import com.example.model.DailyWirdGoal
import com.example.model.Reciter
import com.example.notifications.DailyWirdReminderScheduler
import com.example.ui.components.ReciterNameWithMic
import com.example.ui.components.ReciterPortrait
import com.example.ui.components.formatMillis
import com.example.ui.theme.AmiriFontFamily

@Composable
fun HomeScreen(
  reciters: List<Reciter>,
  portraits: Map<String, String?>,
  preferences: UserPreferencesState,
  onRequestPortrait: (Reciter) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onOpenFavorites: () -> Unit,
  onToggleWirdCompleted: () -> Unit,
  onOpenWirdSettings: () -> Unit,
  onPlayTodayRecitation: () -> Unit,
  onResumeLastPlayed: () -> Unit,
  onReciterClick: (Reciter) -> Unit,
  onSeeAllReciters: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val favoriteReciterIds = preferences.favoriteReciterIds
  val lastPlayed = preferences.lastPlayed
  val today = remember { QuranCatalog.todayRecitation() }
  val todayReciter =
    remember(today, reciters) {
      reciters.firstOrNull { it.id == today.reciterId }
        ?: reciters.firstOrNull()
        ?: QuranCatalog.reciters.first()
    }
  val todayRiwayah =
    remember(today, todayReciter) {
      todayReciter.riwayat.getOrElse(today.riwayahIndex) { todayReciter.riwayat.first() }
    }
  val todaySurah = remember(today) { QuranCatalog.findSurah(today.surahNumber) ?: QuranCatalog.surahs.first() }

  val favoriteReciters =
    remember(reciters, favoriteReciterIds) {
      reciters.filter { favoriteReciterIds.contains(it.id) }
    }
  val mostListened = remember(reciters) { reciters.filter { it.isMostListened } }
  val hafsReciters =
    remember(reciters) {
      reciters.filter { r -> r.riwayat.any { it.nameAr.contains("حفص") } }.take(30)
    }
  val warshReciters =
    remember(reciters) {
      reciters.filter { r -> r.riwayat.any { it.nameAr.contains("ورش") } }
    }
  val qalonAndDoriReciters =
    remember(reciters) {
      reciters.filter { r ->
        r.riwayat.any { it.nameAr.contains("قالون") || it.nameAr.contains("الدوري") }
      }
    }
  val mojawwadReciters =
    remember(reciters) {
      reciters.filter { r ->
        r.riwayat.any { it.nameAr.contains("مجود") || it.nameAr.contains("المجود") }
      }
    }

  LaunchedEffect(todayReciter.id) { onRequestPortrait(todayReciter) }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
      contentPadding = PaddingValues(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      // 1. Hero Card: تلاوة اليوم
      item {
        TodayRecitationHeroCard(
          reciter = todayReciter,
          surahName = todaySurah.nameAr,
          riwayahName = todayRiwayah.nameAr,
          highlightNote = today.highlightNoteAr,
          portraitUrl = portraits[todayReciter.id],
          onPlayClick = onPlayTodayRecitation,
          onReciterClick = { onReciterClick(todayReciter) },
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
      }

      // 2. Daily Quran Portion (الورد اليومي) Card
      item {
        DailyWirdHomeCard(
          goal = preferences.wirdGoal,
          isCompletedToday = preferences.isTodayWirdCompleted(),
          streakDays = preferences.effectiveWirdStreakDays(),
          reminderEnabled = preferences.wirdReminderEnabled,
          reminderHour = preferences.wirdReminderHour,
          reminderMinute = preferences.wirdReminderMinute,
          onToggleCompleted = onToggleWirdCompleted,
          onOpenReminderSettings = onOpenWirdSettings,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }

      // 3. Continue Listening Banner (if local history exists)
      if (lastPlayed != null) {
        val lastReciter =
          reciters.firstOrNull { it.id == lastPlayed.reciterId }
            ?: QuranCatalog.findReciter(lastPlayed.reciterId)
        val lastSurah = QuranCatalog.findSurah(lastPlayed.surahNumber)
        if (lastReciter != null && lastSurah != null) {
          item {
            LaunchedEffect(lastReciter.id) { onRequestPortrait(lastReciter) }
            ContinueListeningCard(
              reciter = lastReciter,
              surahName = lastSurah.nameAr,
              riwayahName = lastPlayed.riwayahName,
              positionMs = lastPlayed.positionMs,
              portraitUrl = portraits[lastReciter.id],
              onResumeClick = onResumeLastPlayed,
              modifier = Modifier.padding(horizontal = 16.dp),
            )
          }
        }
      }

      // 4. Dedicated Favorites Section
      item {
        if (favoriteReciters.isNotEmpty()) {
          ReciterHorizontalSection(
            title = "القراء المفضلون",
            reciters = favoriteReciters,
            portraits = portraits,
            favoriteReciterIds = favoriteReciterIds,
            useSquareCards = true,
            onRequestPortrait = onRequestPortrait,
            onToggleFavorite = onToggleFavorite,
            onReciterClick = onReciterClick,
            onSeeAllClick = onOpenFavorites,
            seeAllLabel = "عرض المفضلة (${favoriteReciters.size})",
          )
        } else {
          HomeFavoritesPromptCard(
            onOpenFavorites = onOpenFavorites,
            modifier = Modifier.padding(horizontal = 16.dp),
          )
        }
      }

      // 4. Most Listened Reciters (Square Cards)
      item {
        ReciterHorizontalSection(
          title = "القراء الأكثر استماعاً",
          reciters = mostListened,
          portraits = portraits,
          favoriteReciterIds = favoriteReciterIds,
          useSquareCards = true,
          onRequestPortrait = onRequestPortrait,
          onToggleFavorite = onToggleFavorite,
          onReciterClick = onReciterClick,
          onSeeAllClick = onSeeAllReciters,
        )
      }

      // 5. Hafs 'an 'Asim Reciters (Circular Cards)
      if (hafsReciters.isNotEmpty()) {
        item {
          ReciterHorizontalSection(
            title = "تلاوات برواية حفص عن عاصم",
            reciters = hafsReciters,
            portraits = portraits,
            favoriteReciterIds = favoriteReciterIds,
            useSquareCards = false,
            onRequestPortrait = onRequestPortrait,
            onToggleFavorite = onToggleFavorite,
            onReciterClick = onReciterClick,
            onSeeAllClick = onSeeAllReciters,
          )
        }
      }

      // 6. Warsh 'an Nafi' Reciters (Circular Cards)
      if (warshReciters.isNotEmpty()) {
        item {
          ReciterHorizontalSection(
            title = "تلاوات برواية ورش عن نافع",
            reciters = warshReciters,
            portraits = portraits,
            favoriteReciterIds = favoriteReciterIds,
            useSquareCards = false,
            onRequestPortrait = onRequestPortrait,
            onToggleFavorite = onToggleFavorite,
            onReciterClick = onReciterClick,
          )
        }
      }

      // 7. Qalon & Al-Douri Reciters (Circular Cards)
      if (qalonAndDoriReciters.isNotEmpty()) {
        item {
          ReciterHorizontalSection(
            title = "تلاوات برواية قالون والدوري",
            reciters = qalonAndDoriReciters,
            portraits = portraits,
            favoriteReciterIds = favoriteReciterIds,
            useSquareCards = false,
            onRequestPortrait = onRequestPortrait,
            onToggleFavorite = onToggleFavorite,
            onReciterClick = onReciterClick,
          )
        }
      }

      // 8. Al-Mushaf Al-Mujawwad (Circular Cards)
      if (mojawwadReciters.isNotEmpty()) {
        item {
          ReciterHorizontalSection(
            title = "المصحف المجود",
            reciters = mojawwadReciters,
            portraits = portraits,
            favoriteReciterIds = favoriteReciterIds,
            useSquareCards = false,
            onRequestPortrait = onRequestPortrait,
            onToggleFavorite = onToggleFavorite,
            onReciterClick = onReciterClick,
          )
        }
      }
    }
  }
}

@Composable
private fun TodayRecitationHeroCard(
  reciter: Reciter,
  surahName: String,
  riwayahName: String,
  highlightNote: String,
  portraitUrl: String?,
  onPlayClick: () -> Unit,
  onReciterClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val bwMatrix = remember { ColorMatrix().apply { setToSaturation(0f) } }
  val oliveDark = Color(0xFF1F2B23)
  val oliveAccent = MaterialTheme.colorScheme.primary

  Card(
    modifier = modifier.fillMaxWidth().testTag("today_recitation_hero_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = oliveDark),
    border = BorderStroke(1.dp, oliveAccent.copy(alpha = 0.45f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
  ) {
    Box(modifier = Modifier.fillMaxWidth().height(226.dp)) {
      // Background reciter portrait in black & white
      if (!portraitUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
          model =
            ImageRequest.Builder(LocalContext.current).data(portraitUrl).crossfade(true).build(),
          contentDescription = reciter.nameAr,
          contentScale = ContentScale.Crop,
          colorFilter = ColorFilter.colorMatrix(bwMatrix),
          modifier = Modifier.fillMaxSize(),
          error = { HeroStudioBackdrop() },
        )
      } else {
        HeroStudioBackdrop()
      }

      // Dark olive-green gradient overlay ensuring crisp contrast
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.horizontalGradient(
                colors =
                  listOf(
                    Color(0xF0121413),
                    Color(0xD91A251E),
                    Color(0xA6293B2F),
                  )
              )
            )
      )

      Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = oliveAccent.copy(alpha = 0.22f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, oliveAccent.copy(alpha = 0.5f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF9CB8A3),
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = "تلاوة اليوم",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFEDF1EE),
              )
            }
          }

          Text(
            text = riwayahName,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFCBB88B),
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "سورة $surahName",
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = AmiriFontFamily),
            color = Color(0xFFEDF1EE),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = highlightNote,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFC2CEC5),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            onClick = onReciterClick,
            color = Color(0x33FFFFFF),
            shape = RoundedCornerShape(50),
          ) {
            ReciterNameWithMic(
              name = reciter.nameAr,
              portraitUrl = portraitUrl,
              style = MaterialTheme.typography.labelLarge,
              color = Color(0xFFEDF1EE),
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
          }

          Button(
            onClick = onPlayClick,
            shape = RoundedCornerShape(50),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7A9A82),
                contentColor = Color(0xFF121413),
              ),
            modifier = Modifier.testTag("play_today_recitation_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("استماع الآن", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun HeroStudioBackdrop() {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.radialGradient(
            colors = listOf(Color(0xFF2B3B30), Color(0xFF121413))
          )
        ),
    contentAlignment = Alignment.CenterEnd,
  ) {
    Icon(
      imageVector = Icons.Rounded.Mic,
      contentDescription = null,
      tint = Color(0x287A9A82),
      modifier = Modifier.padding(end = 24.dp).size(120.dp),
    )
  }
}

@Composable
private fun ContinueListeningCard(
  reciter: Reciter,
  surahName: String,
  riwayahName: String,
  positionMs: Long,
  portraitUrl: String?,
  onResumeClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("continue_listening_card")
        .clickable(onClick = onResumeClick),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ReciterPortrait(
        portraitUrl = portraitUrl,
        contentDescription = reciter.nameAr,
        size = 50.dp,
        isCircular = true,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = "متابعة الاستماع",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Text(
          text = "سورة $surahName",
          style = MaterialTheme.typography.titleMedium.copy(fontFamily = AmiriFontFamily),
          color = MaterialTheme.colorScheme.onSurface,
        )
        ReciterNameWithMic(
          name = "${reciter.nameAr} • $riwayahName (${formatMillis(positionMs)})",
          portraitUrl = portraitUrl,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Box(
        modifier =
          Modifier.size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.PlayArrow,
          contentDescription = "استئناف التلاوة",
          tint = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun DailyWirdHomeCard(
  goal: DailyWirdGoal,
  isCompletedToday: Boolean,
  streakDays: Int,
  reminderEnabled: Boolean,
  reminderHour: Int,
  reminderMinute: Int,
  onToggleCompleted: () -> Unit,
  onOpenReminderSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val formattedTime =
    remember(reminderHour, reminderMinute) {
      DailyWirdReminderScheduler.formatArabicTime(reminderHour, reminderMinute)
    }

  Card(
    modifier = modifier.fillMaxWidth().testTag("home_daily_wird_card"),
    shape = RoundedCornerShape(20.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isCompletedToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
          else MaterialTheme.colorScheme.surface
      ),
    border =
      BorderStroke(
        1.dp,
        if (isCompletedToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.MenuBook,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(19.dp),
            )
          }
          Column {
            Text(
              text = "الورد اليومي",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "${goal.labelAr} • ${goal.descriptionAr}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        if (streakDays > 0) {
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
          ) {
            Text(
              text = if (streakDays == 1) "يوم واحد" else "$streakDays أيام متتالية",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(
          onClick = onToggleCompleted,
          shape = RoundedCornerShape(50),
          colors =
            ButtonDefaults.buttonColors(
              containerColor =
                if (isCompletedToday) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primaryContainer,
              contentColor =
                if (isCompletedToday) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.primary,
            ),
          modifier = Modifier.weight(1f).testTag("home_wird_complete_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isCompletedToday) "تم إنجاز ورد اليوم" else "إتمام ورد اليوم",
            fontWeight = FontWeight.SemiBold,
          )
        }

        OutlinedButton(
          onClick = onOpenReminderSettings,
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          modifier = Modifier.testTag("home_wird_settings_button"),
        ) {
          Icon(
            imageVector =
              if (reminderEnabled) Icons.Rounded.NotificationsActive
              else Icons.Rounded.NotificationsOff,
            contentDescription = "إعداد تذكير الورد اليومي",
            tint =
              if (reminderEnabled) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (reminderEnabled) formattedTime else "تذكير",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}

@Composable
private fun HomeFavoritesPromptCard(
  onOpenFavorites: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("home_favorites_empty_card")
        .clickable(onClick = onOpenFavorites),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier =
            Modifier.size(42.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.FavoriteBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "القراء المفضلون",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "اضغط على القلب في أي بطاقة لحفظ قارئك محلياً",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      TextButton(
        onClick = onOpenFavorites,
        modifier = Modifier.testTag("home_open_favorites_button"),
      ) {
        Text(
          text = "المفضلة",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun ReciterHorizontalSection(
  title: String,
  reciters: List<Reciter>,
  portraits: Map<String, String?>,
  favoriteReciterIds: Set<String>,
  useSquareCards: Boolean,
  onRequestPortrait: (Reciter) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onReciterClick: (Reciter) -> Unit,
  onSeeAllClick: (() -> Unit)? = null,
  seeAllLabel: String = "عرض الكل",
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
      if (onSeeAllClick != null) {
        Text(
          text = seeAllLabel,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
              .clickable(onClick = onSeeAllClick)
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .testTag(
                if (title == "القراء المفضلون") "see_all_favorites_link"
                else "see_all_reciters_link"
              ),
        )
      }
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      items(reciters, key = { it.id }) { reciter ->
        LaunchedEffect(reciter.id) { onRequestPortrait(reciter) }
        val isFavorite = favoriteReciterIds.contains(reciter.id)
        if (useSquareCards) {
          SquareReciterCard(
            reciter = reciter,
            portraitUrl = portraits[reciter.id],
            isFavorite = isFavorite,
            onToggleFavorite = { onToggleFavorite(reciter.id) },
            onClick = { onReciterClick(reciter) },
          )
        } else {
          CircularReciterCard(
            reciter = reciter,
            portraitUrl = portraits[reciter.id],
            isFavorite = isFavorite,
            onToggleFavorite = { onToggleFavorite(reciter.id) },
            onClick = { onReciterClick(reciter) },
          )
        }
      }
    }
  }
}

@Composable
private fun SquareReciterCard(
  reciter: Reciter,
  portraitUrl: String?,
  isFavorite: Boolean,
  onToggleFavorite: () -> Unit,
  onClick: () -> Unit,
) {
  Card(
    modifier =
      Modifier.width(142.dp)
        .testTag("reciter_square_card_${reciter.id}")
        .clickable(onClick = onClick),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border =
      BorderStroke(
        1.dp,
        if (isFavorite) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
      ),
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(modifier = Modifier.size(120.dp)) {
        ReciterPortrait(
          portraitUrl = portraitUrl,
          contentDescription = reciter.nameAr,
          size = 120.dp,
          isCircular = false,
        )
        CardFavoriteBadgeButton(
          isFavorite = isFavorite,
          reciterName = reciter.nameAr,
          onClick = onToggleFavorite,
          modifier =
            Modifier.align(Alignment.TopEnd)
              .offset(x = 4.dp, y = (-4).dp)
              .testTag("home_favorite_btn_${reciter.id}"),
        )
      }
      ReciterNameWithMic(
        name = reciter.nameAr,
        portraitUrl = portraitUrl,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        centered = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Text(
        text =
          if (reciter.riwayat.size > 1) "${reciter.riwayat.size} روايات"
          else reciter.riwayat.first().nameAr,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun CircularReciterCard(
  reciter: Reciter,
  portraitUrl: String?,
  isFavorite: Boolean,
  onToggleFavorite: () -> Unit,
  onClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier.width(108.dp)
        .clip(RoundedCornerShape(16.dp))
        .testTag("reciter_circle_card_${reciter.id}")
        .clickable(onClick = onClick)
        .padding(vertical = 6.dp, horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(modifier = Modifier.size(92.dp)) {
      ReciterPortrait(
        portraitUrl = portraitUrl,
        contentDescription = reciter.nameAr,
        size = 92.dp,
        isCircular = true,
      )
      CardFavoriteBadgeButton(
        isFavorite = isFavorite,
        reciterName = reciter.nameAr,
        onClick = onToggleFavorite,
        modifier =
          Modifier.align(Alignment.TopEnd)
            .offset(x = 6.dp, y = (-6).dp)
            .testTag("home_favorite_btn_${reciter.id}"),
      )
    }
    ReciterNameWithMic(
      name = reciter.nameAr,
      portraitUrl = portraitUrl,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onBackground,
      maxLines = 2,
      centered = true,
      modifier = Modifier.fillMaxWidth(),
    )
    Text(
      text =
        if (reciter.riwayat.size > 1) "${reciter.riwayat.size} روايات"
        else reciter.riwayat.first().nameAr,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun CardFavoriteBadgeButton(
  isFavorite: Boolean,
  reciterName: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  IconButton(
    onClick = onClick,
    modifier = modifier.minimumInteractiveComponentSize(),
  ) {
    Box(
      modifier =
        Modifier.size(28.dp)
          .clip(CircleShape)
          .background(
            if (isFavorite) MaterialTheme.colorScheme.primary
            else Color(0xBD121413)
          )
          .border(
            BorderStroke(
              1.dp,
              if (isFavorite) MaterialTheme.colorScheme.primary
              else Color(0x66EDF1EE),
            ),
            CircleShape,
          ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        contentDescription =
          if (isFavorite) "إزالة $reciterName من المفضلة" else "إضافة $reciterName للمفضلة",
        tint = if (isFavorite) MaterialTheme.colorScheme.onPrimary else Color(0xFFEDF1EE),
        modifier = Modifier.size(15.dp),
      )
    }
  }
}
