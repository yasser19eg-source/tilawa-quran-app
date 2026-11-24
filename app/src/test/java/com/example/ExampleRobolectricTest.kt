package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.Mp3QuranApi
import com.example.data.Mp3QuranMoshafDto
import com.example.data.Mp3QuranReciterDto
import com.example.data.Mp3QuranRecitersResponse
import com.example.data.QuranCatalog
import com.example.data.RecentSearchesManager
import com.example.data.ReciterCatalogRepository
import com.example.data.UserPreferencesRepository
import com.example.model.ActiveAudioTarget
import com.example.model.DailyWirdGoal
import com.example.model.PlaybackUiState
import com.example.notifications.DailyWirdReminderScheduler
import com.example.playback.QuranPlaybackService
import com.example.ui.components.QuranMedia3PlayerCard
import com.example.ui.components.SEEK_STEP_MS
import com.example.ui.theme.MushafAlTilawahTheme
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `app_name matches تلاوة`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("تلاوة", appName)
  }

  @Test
  fun `dedication string contains عبد العظيم رجب زكي`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dedication = context.getString(R.string.dedication_name)
    assertTrue(dedication.contains("عبد العظيم رجب زكي"))
  }

  @Test
  fun `recent searches manager persists last 10 queries in MRU order`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = RecentSearchesManager(context)
    manager.clearAll()

    val queries =
      listOf(
        "الحصري",
        "المنشاوي",
        "عبد الباسط",
        "العفاسي",
        "السديس",
        "ماهر",
        "الدوسري",
        "الطبلاوي",
        "البنا",
        "مصطفى",
        "العجمي",
        "الشاطري",
      )
    queries.forEach { manager.saveSearchQuery(it) }

    var recent = manager.getRecentSearches()
    assertEquals(10, recent.size)
    assertEquals("الشاطري", recent.first())
    assertEquals("عبد الباسط", recent.last())

    // Re-saving an existing query moves it to the top without duplicating
    manager.saveSearchQuery("الحصري")
    recent = manager.getRecentSearches()
    assertEquals(10, recent.size)
    assertEquals("الحصري", recent.first())

    manager.removeSearchQuery("الحصري")
    assertEquals(9, manager.getRecentSearches().size)

    manager.clearAll()
    assertTrue(manager.getRecentSearches().isEmpty())
  }

  @Test
  fun `reciter catalog repository seeds curated reciters refreshes from api and reloads disk cache`() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val fakeApi =
        object : Mp3QuranApi {
          override suspend fun getReciters(language: String): Mp3QuranRecitersResponse {
            return Mp3QuranRecitersResponse(
              reciters =
                listOf(
                  Mp3QuranReciterDto(
                    id = 501,
                    name = "إدريس أبكر",
                    moshaf =
                      listOf(
                        Mp3QuranMoshafDto(
                          id = 5011,
                          name = "حفص عن عاصم",
                          server = "https://server6.mp3quran.net/abkr/",
                          surahTotal = 114,
                          surahList = "1,2,18,114",
                        )
                      ),
                  )
                )
            )
          }
        }

      val repo = ReciterCatalogRepository(context, apiOverride = fakeApi)
      assertEquals(20, repo.recitersFlow.value.size)

      val refreshed = repo.refreshCatalog()
      assertEquals(21, refreshed.size)
      assertTrue(refreshed.any { it.id == "mp3q_501" && it.nameAr == "إدريس أبكر" })

      // Fresh repository instance should load the cached 21 reciters from disk
      val repoFromCache = ReciterCatalogRepository(context, apiOverride = fakeApi)
      assertTrue(repoFromCache.loadFromDiskCache())
      assertEquals(21, repoFromCache.recitersFlow.value.size)
    }

  @Test
  fun `favorite reciters toggle persists and removes reciter ids locally`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = UserPreferencesRepository(context)

    prefsRepo.toggleFavoriteReciter("husary")
    prefsRepo.toggleFavoriteReciter("minshawi")

    val afterAdd = prefsRepo.preferencesFlow.first().favoriteReciterIds
    assertTrue(afterAdd.contains("husary"))
    assertTrue(afterAdd.contains("minshawi"))

    prefsRepo.toggleFavoriteReciter("husary")
    val afterRemove = prefsRepo.preferencesFlow.first().favoriteReciterIds
    assertFalse(afterRemove.contains("husary"))
    assertTrue(afterRemove.contains("minshawi"))
  }

  @Test
  fun `daily wird preferences persist reminder time goal and completion streak`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = UserPreferencesRepository(context)

    prefsRepo.setWirdReminderEnabled(true)
    prefsRepo.setWirdReminderTime(hour = 5, minute = 30)
    prefsRepo.setWirdGoal(DailyWirdGoal.HALF_HIZB)

    val day100 = 20_100L
    val day101 = 20_101L
    prefsRepo.toggleTodayWirdCompleted(todayEpochDay = day100)
    prefsRepo.toggleTodayWirdCompleted(todayEpochDay = day101)

    val state = prefsRepo.preferencesFlow.first()
    assertTrue(state.wirdReminderEnabled)
    assertEquals(5, state.wirdReminderHour)
    assertEquals(30, state.wirdReminderMinute)
    assertEquals(DailyWirdGoal.HALF_HIZB, state.wirdGoal)
    assertTrue(state.isTodayWirdCompleted(day101))
    assertEquals(2, state.effectiveWirdStreakDays(day101))
  }

  @Test
  fun `daily wird scheduler registers and cancels AlarmManager alarm`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val shadowAlarm = Shadows.shadowOf(alarmManager)

    DailyWirdReminderScheduler.scheduleDailyReminder(context, hour = 8, minute = 15)
    assertNotNull(shadowAlarm.nextScheduledAlarm)

    DailyWirdReminderScheduler.cancelDailyReminder(context)
    assertNull(shadowAlarm.nextScheduledAlarm)
  }

  @Test
  fun `quran media3 player card supports play pause and seek controls`() {
    val reciter = QuranCatalog.findReciter("husary")!!
    val riwayah = reciter.riwayat.first()
    val surah = QuranCatalog.findSurah(1)!!

    var playPauseToggles = 0
    val seekDeltas = mutableListOf<Long>()

    composeTestRule.setContent {
      MushafAlTilawahTheme {
        QuranMedia3PlayerCard(
          playbackState =
            PlaybackUiState(
              activeTarget = ActiveAudioTarget.SurahTrack(reciter, riwayah, surah),
              isPlaying = true,
              isBuffering = false,
              currentPositionMs = 20_000L,
              bufferedPositionMs = 45_000L,
              durationMs = 60_000L,
            ),
          portraits = emptyMap(),
          onTogglePlayPause = { playPauseToggles++ },
          onSeekTo = {},
          onSeekBy = { delta -> seekDeltas.add(delta) },
          onPreviousSurah = {},
          onNextSurah = {},
          onExpandPlayer = {},
          onStop = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("quran_media3_player_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("سورة الفاتحة").assertIsDisplayed()
    composeTestRule.onNodeWithTag("inline_player_seekbar").assertIsDisplayed()

    composeTestRule.onNodeWithTag("inline_player_play_pause").performClick()
    composeTestRule.onNodeWithTag("inline_player_forward_10s").performClick()
    composeTestRule.onNodeWithTag("inline_player_rewind_10s").performClick()

    assertEquals(1, playPauseToggles)
    assertEquals(listOf(SEEK_STEP_MS, -SEEK_STEP_MS), seekDeltas)
  }

  @Test
  fun `quran playback service initializes player and plays surah without SecurityException`() {
    val controller = Robolectric.buildService(QuranPlaybackService::class.java).create()
    val service = controller.get()
    val player = service.activePlayerForTest()
    assertNotNull(player)
    player!!.setMediaItem(MediaItem.fromUri("https://server13.mp3quran.net/husr/001.mp3"))
    player.prepare()
    player.play()
    assertTrue(player.playWhenReady)
    controller.destroy()
  }
}
