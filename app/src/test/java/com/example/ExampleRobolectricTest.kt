package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.AlQuranAyahDto
import com.example.data.AlQuranCloudApi
import com.example.data.AlQuranEditionDataDto
import com.example.data.AlQuranEditionMetaDto
import com.example.data.AlQuranMultiEditionResponse
import com.example.data.AlQuranSingleEditionResponse
import com.example.data.Mp3QuranApi
import com.example.data.Mp3QuranMoshafDto
import com.example.data.Mp3QuranRadioDto
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.example.data.Mp3QuranRadiosResponse
import com.example.data.Mp3QuranReciterDto
import com.example.data.Mp3QuranRecitersResponse
import com.example.data.MushafPageRepository
import com.example.data.QuranCatalog
import com.example.data.QuranMushafRepository
import com.example.data.RecentSearchesManager
import com.example.data.ReciterCatalogRepository
import com.example.data.UserPreferencesRepository
import com.example.data.UserPreferencesState
import com.example.model.ActiveAudioTarget
import com.example.model.DailyWirdGoal
import com.example.model.MushafAyah
import com.example.model.MushafReaderUiState
import com.example.model.MushafSurahContent
import com.example.model.PlaybackUiState
import com.example.model.SelectedAyahKey
import com.example.notifications.DailyWirdReminderScheduler
import com.example.playback.QuranPlaybackService
import com.example.ui.components.AyahShareCardGenerator
import com.example.ui.components.QuranMedia3PlayerCard
import com.example.ui.components.SEEK_STEP_MS
import com.example.ui.screens.AdhkarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MushafPageReaderScreen
import com.example.ui.screens.QuranMushafScreen
import com.example.ui.screens.SadaqahScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SurahReaderScreen
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
  fun `quran playback service initializes player and plays surah and cairo radio without SecurityException`() {
    val controller = Robolectric.buildService(QuranPlaybackService::class.java).create()
    val service = controller.get()
    val player = service.activePlayerForTest()
    assertNotNull(player)
    player!!.setMediaItem(MediaItem.fromUri("https://server13.mp3quran.net/husr/001.mp3"))
    player.prepare()
    player.play()
    assertTrue(player.playWhenReady)

    player.setMediaItem(MediaItem.fromUri(QuranCatalog.CAIRO_RADIO_STREAM_URL))
    player.prepare()
    player.play()
    assertTrue(player.playWhenReady)
    controller.destroy()
  }

  @Test
  fun `reciter catalog repository refreshes cairo radio urls from api and reloads disk cache`() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val fakeApi =
        object : Mp3QuranApi {
          override suspend fun getReciters(language: String): Mp3QuranRecitersResponse =
            Mp3QuranRecitersResponse()

          override suspend fun getRadios(language: String): Mp3QuranRadiosResponse {
            return Mp3QuranRadiosResponse(
              radios =
                listOf(
                  Mp3QuranRadioDto(
                    id = 10,
                    name = "إذاعة القرآن الكريم من القاهرة",
                    url = "https://updated-cairo.mp3quran.net/live",
                  )
                )
            )
          }
        }

      val repo = ReciterCatalogRepository(context, apiOverride = fakeApi)
      val refreshedUrls = repo.refreshRadios()
      assertTrue(refreshedUrls.contains("https://updated-cairo.mp3quran.net/live"))

      val repoFromCache = ReciterCatalogRepository(context, apiOverride = fakeApi)
      assertTrue(repoFromCache.loadRadiosFromDiskCache())
      assertTrue(
        repoFromCache.getCairoRadioUrls().contains("https://updated-cairo.mp3quran.net/live")
      )
    }

  @Test
  fun `application enables cleartext traffic for live radio stream redirects`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val flags = context.applicationInfo.flags
    assertTrue((flags and android.content.pm.ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0)
  }

  @Test
  fun `splash screen renders animated tilawah branding and allows entering app`() {
    var finished = false
    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      MushafAlTilawahTheme {
        SplashScreen(
          onSplashFinished = { finished = true },
          autoAdvanceDelayMs = 10_000L,
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1_600L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("splash_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("splash_emblem").assertIsDisplayed()
    composeTestRule.onNodeWithTag("splash_title_text").assertIsDisplayed()
    composeTestRule.onNodeWithText("تلاوة").assertIsDisplayed()

    composeTestRule.onNodeWithTag("splash_screen").performClick()
    composeTestRule.waitForIdle()
    assertTrue(finished)
  }

  @Test
  fun `sadaqah screen displays memorial portrait full name short dua and existing elements`() {
    composeTestRule.setContent {
      MushafAlTilawahTheme {
        SadaqahScreen(
          duaCounter = 12,
          onIncrementDuaCounter = {},
          onShowFeedback = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("sadaqah_memorial_portrait").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sadaqah_memorial_honorific").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sadaqah_memorial_name").assertIsDisplayed()
    composeTestRule.onNodeWithText("المغفور له بإذن الله").assertIsDisplayed()
    composeTestRule.onNodeWithText("الحاج عبد العظيم رجب زكي").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sadaqah_memorial_short_dua")
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun `quran mushaf repository fetches uthmani with tafsir muyassar and reloads disk cache`() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      context.filesDir.resolve("mushaf_cache/surah_112.json").delete()
      var networkCalls = 0
      val fakeApi =
        object : AlQuranCloudApi {
          override suspend fun getSurahWithTafsir(
            surahNumber: Int
          ): AlQuranMultiEditionResponse {
            networkCalls++
            return AlQuranMultiEditionResponse(
              code = 200,
              status = "OK",
              data =
                listOf(
                  AlQuranEditionDataDto(
                    number = 112,
                    name = "سورة الإخلاص",
                    edition = AlQuranEditionMetaDto(identifier = "quran-uthmani"),
                    ayahs =
                      listOf(
                        AlQuranAyahDto(
                          number = 6222,
                          text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ قُلْ هُوَ ٱللَّهُ أَحَدٌ",
                          numberInSurah = 1,
                          juz = 30,
                          page = 604,
                        )
                      ),
                  ),
                  AlQuranEditionDataDto(
                    number = 112,
                    name = "سورة الإخلاص",
                    edition = AlQuranEditionMetaDto(identifier = "ar.muyassar"),
                    ayahs =
                      listOf(
                        AlQuranAyahDto(
                          number = 6222,
                          text = "قل أيها الرسول: هو الله الواحد الأحد...",
                          numberInSurah = 1,
                          juz = 30,
                          page = 604,
                        )
                      ),
                  ),
                ),
            )
          }

          override suspend fun getSurahEdition(
            surahNumber: Int,
            edition: String,
          ): AlQuranSingleEditionResponse = AlQuranSingleEditionResponse()
        }

      val repo = QuranMushafRepository(context, apiOverride = fakeApi)
      val firstLoad = repo.loadSurah(112).getOrThrow()
      assertFalse(firstLoad.isFromCache)
      assertEquals(1, networkCalls)
      assertEquals("قُلْ هُوَ ٱللَّهُ أَحَدٌ", firstLoad.content.ayahs.first().textAr)
      assertEquals(
        "قل أيها الرسول: هو الله الواحد الأحد...",
        firstLoad.content.ayahs.first().tafsirMuyassarAr,
      )

      // Fresh instance loads cleanly from disk cache without extra network requests
      val repoFromDisk = QuranMushafRepository(context, apiOverride = fakeApi)
      val secondLoad = repoFromDisk.loadSurah(112).getOrThrow()
      assertTrue(secondLoad.isFromCache)
      assertEquals(1, networkCalls)
      assertTrue(repoFromDisk.cachedSurahNumbers().contains(112))
    }

  @Test
  fun `adhkar screen increments repetition counter switches categories and completes dhikr`() {
    val counts = androidx.compose.runtime.mutableStateMapOf<String, Int>()
    composeTestRule.setContent {
      MushafAlTilawahTheme {
        AdhkarScreen(
          adhkarCounts = counts,
          onIncrementDhikr = { dhikr ->
            val current = counts[dhikr.id] ?: 0
            if (current < dhikr.targetCount) counts[dhikr.id] = current + 1
          },
          onResetCategory = { category ->
            com.example.data.AdhkarCatalog.itemsForCategory(category).forEach {
              counts.remove(it.id)
            }
          },
          onShowFeedback = {},
          onBackClick = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("adhkar_category_post_prayer").assertIsDisplayed().performClick()
    composeTestRule
      .onNodeWithTag("dhikr_counter_prayer_istighfar_salam")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()

    assertEquals(1, counts["prayer_istighfar_salam"])
  }

  @Test
  fun `surah reader screen displays uthmani ayah and tafsir muyassar`() {
    val surah = QuranCatalog.findSurah(112)!!
    composeTestRule.setContent {
      androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides
          androidx.compose.ui.unit.LayoutDirection.Rtl
      ) {
        MushafAlTilawahTheme {
          SurahReaderScreen(
            surahNumber = 112,
            readerState =
              MushafReaderUiState.Success(
                content =
                  MushafSurahContent(
                    surah = surah,
                    ayahs =
                      listOf(
                        MushafAyah(
                          numberInSurah = 1,
                          globalNumber = 6222,
                          textAr = "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
                          tafsirMuyassarAr = "قل -أيها الرسول-: هو الله المتفرد بالألوهية.",
                          juz = 30,
                          page = 604,
                        )
                      ),
                  ),
                isFromCache = true,
              ),
            fontScale = 1.0f,
            bookmarkedSurahNumber = 1,
            bookmarkedAyahNumber = 1,
            hasBookmark = false,
            onChangeFontScale = {},
            onSaveBookmark = { _, _ -> },
            onNavigateSurah = {},
            onRetry = {},
            onShowFeedback = {},
            onBackClick = {},
          )
        }
      }
    }

    composeTestRule.onNodeWithTag("surah_reader_title").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mushaf_horizontal_pager").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mushaf_page_card_604").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mushaf_pager_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mushaf_page_text_604").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("ayah_tafsir_bottom_sheet").assertIsDisplayed()
    composeTestRule
      .onNodeWithText("قل -أيها الرسول-: هو الله المتفرد بالألوهية.")
      .assertIsDisplayed()

    composeTestRule.onNodeWithTag("tafsir_sheet_close_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("reader_mode_with_tafsir").performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("ayah_tafsir_card_1")
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun `surah reader screen flips mushaf pages horizontally like a book`() {
    val surah = QuranCatalog.findSurah(2)!!
    composeTestRule.setContent {
      androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides
          androidx.compose.ui.unit.LayoutDirection.Rtl
      ) {
        MushafAlTilawahTheme(darkTheme = false) {
          SurahReaderScreen(
            surahNumber = 2,
            readerState =
              MushafReaderUiState.Success(
                content =
                  MushafSurahContent(
                    surah = surah,
                    ayahs =
                      listOf(
                        MushafAyah(
                          numberInSurah = 1,
                          globalNumber = 8,
                          textAr = "الٓمٓ",
                          tafsirMuyassarAr = "تفسير الآية الأولى",
                          juz = 1,
                          page = 2,
                        ),
                        MushafAyah(
                          numberInSurah = 6,
                          globalNumber = 13,
                          textAr = "إِنَّ ٱلَّذِينَ كَفَرُوا۟ سَوَآءٌ عَلَيْهِمْ ۖ أَمْ لَمْ تُنذِرْهُمْ",
                          tafsirMuyassarAr = "تفسير الآية السادسة",
                          juz = 1,
                          page = 3,
                        ),
                      ),
                  ),
                isFromCache = true,
              ),
            fontScale = 1.0f,
            bookmarkedSurahNumber = 1,
            bookmarkedAyahNumber = 1,
            hasBookmark = false,
            onChangeFontScale = {},
            onSaveBookmark = { _, _ -> },
            onNavigateSurah = {},
            onRetry = {},
            onShowFeedback = {},
            onBackClick = {},
          )
        }
      }
    }

    composeTestRule.onNodeWithTag("mushaf_horizontal_pager").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mushaf_page_card_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("next_mushaf_page_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("mushaf_page_card_3").assertIsDisplayed()
  }

  @Test
  fun `home screen quick access cards trigger quran mushaf and adhkar navigation`() {
    var mushafOpened = false
    var adhkarOpened = false

    composeTestRule.setContent {
      MushafAlTilawahTheme {
        HomeScreen(
          reciters = QuranCatalog.reciters,
          portraits = emptyMap(),
          preferences = UserPreferencesState(),
          onRequestPortrait = {},
          onToggleFavorite = {},
          onOpenFavorites = {},
          onToggleWirdCompleted = {},
          onOpenWirdSettings = {},
          onPlayTodayRecitation = {},
          onResumeLastPlayed = {},
          onReciterClick = {},
          onSeeAllReciters = {},
          onOpenQuranMushaf = { mushafOpened = true },
          onOpenAdhkar = { adhkarOpened = true },
        )
      }
    }

    composeTestRule.onNodeWithTag("home_open_mushaf_card").assertIsDisplayed().performClick()
    composeTestRule.onNodeWithTag("home_open_adhkar_card").assertIsDisplayed().performClick()
    assertTrue(mushafOpened)
    assertTrue(adhkarOpened)
  }

  @Test
  fun `ayah share card generator renders valid bitmap with app branding and memorial footer`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val surah = QuranCatalog.findSurah(112)!!
    val ayahs =
      listOf(
        MushafAyah(
          numberInSurah = 1,
          globalNumber = 6222,
          textAr = "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
          tafsirMuyassarAr = "قل هو الله المتفرد بالألوهية",
          juz = 30,
          page = 604,
        ),
        MushafAyah(
          numberInSurah = 2,
          globalNumber = 6223,
          textAr = "ٱللَّهُ ٱلصَّمَدُ",
          tafsirMuyassarAr = "الله المقصود في جميع الحوائج",
          juz = 30,
          page = 604,
        ),
      )

    assertEquals("تلاوة", com.example.ui.components.AyahShareCardGenerator.APP_BRAND_TITLE)
    assertTrue(
      com.example.ui.components.AyahShareCardGenerator.FOOTER_REQUEST_LINE.contains("نسألكم الدعاء")
    )
    assertTrue(
      com.example.ui.components.AyahShareCardGenerator.FOOTER_NAME_LINE.contains(
        "الحاج عبد العظيم رجب زكي"
      )
    )
    assertEquals(
      "سورة الإخلاص • الآيات ١ - ٢",
      com.example.ui.components.AyahShareCardGenerator.buildAyahRangeLabel(surah, ayahs),
    )

    val bitmapLight =
      com.example.ui.components.AyahShareCardGenerator.renderAyahShareBitmap(
        context = context,
        surah = surah,
        ayahs = ayahs,
        includeTafsir = true,
        useDarkTheme = false,
      )
    assertEquals(1080, bitmapLight.width)
    assertTrue(bitmapLight.height >= 760)
  }

  @Test
  fun `surah reader screen multi ayah selection opens image share preview dialog`() {
    val surah = QuranCatalog.findSurah(112)!!
    composeTestRule.setContent {
      androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides
          androidx.compose.ui.unit.LayoutDirection.Rtl
      ) {
        MushafAlTilawahTheme {
          SurahReaderScreen(
            surahNumber = 112,
            readerState =
              MushafReaderUiState.Success(
                content =
                  MushafSurahContent(
                    surah = surah,
                    ayahs =
                      listOf(
                        MushafAyah(
                          numberInSurah = 1,
                          globalNumber = 6222,
                          textAr = "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
                          tafsirMuyassarAr = "قل -أيها الرسول-: هو الله المتفرد بالألوهية.",
                          juz = 30,
                          page = 604,
                        )
                      ),
                  ),
                isFromCache = true,
              ),
            fontScale = 1.0f,
            bookmarkedSurahNumber = 1,
            bookmarkedAyahNumber = 1,
            hasBookmark = false,
            onChangeFontScale = {},
            onSaveBookmark = { _, _ -> },
            onNavigateSurah = {},
            onRetry = {},
            onShowFeedback = {},
            onBackClick = {},
          )
        }
      }
    }

    // Enter multi-Ayah selection mode, select Ayah, and open share preview dialog
    composeTestRule.onNodeWithTag("reader_multi_select_toggle").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("ayah_selection_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mushaf_page_text_604").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("share_selected_ayahs_image_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("ayah_share_image_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ayah_share_preview_image").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `bundled 604 page bounds and metadata assets cover every page 1 to 604`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = MushafPageRepository(context)
    for (p in 1..604) {
      val bounds = repo.getPageBounds(p)
      val meta = repo.getPageMeta(p)
      assertTrue("Page $p bounds must not be empty", bounds.isNotEmpty())
      assertEquals(p, meta.pageNumber)
      assertTrue("Page $p juz must be 1..30", meta.juzNumber in 1..30)
      assertTrue("Page $p surahs must not be empty", meta.surahNumbers.isNotEmpty())
    }
    val page604Meta = repo.getPageMeta(604)
    assertEquals(listOf(112, 113, 114), page604Meta.surahNumbers)
    assertEquals(30, page604Meta.juzNumber)
  }

  @Test
  fun `quran mushaf index supports direct page jump and juz navigation`() {
    var openedPage: Int? = null
    composeTestRule.setContent {
      androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides
          androidx.compose.ui.unit.LayoutDirection.Rtl
      ) {
        MushafAlTilawahTheme {
          QuranMushafScreen(
            preferences = UserPreferencesState(),
            cachedSurahNumbers = emptySet(),
            onOpenSurah = {},
            onOpenMushafPage = { page -> openedPage = page },
            onBackClick = {},
          )
        }
      }
    }

    val list = composeTestRule.onNodeWithTag("quran_mushaf_surah_list")

    // Verify Surah page badges and quick Juz navigation bar are shown
    list.performScrollToNode(hasTestTag("mushaf_surah_card_1"))
    composeTestRule.onNodeWithTag("mushaf_surah_page_badge_1", useUnmergedTree = true).assertIsDisplayed()

    list.performScrollToNode(hasTestTag("mushaf_juz_quick_bar"))
    composeTestRule.onNodeWithTag("mushaf_juz_quick_bar").assertIsDisplayed()

    // Select Juz 2 from the quick strip -> displays Juz 2 banner and opens Al-Baqarah at page 22
    composeTestRule
      .onNodeWithTag("mushaf_juz_quick_bar")
      .performScrollToNode(hasTestTag("juz_filter_chip_2"))
    composeTestRule.onNodeWithTag("juz_filter_chip_2").performClick()
    composeTestRule.waitForIdle()
    list.performScrollToNode(hasTestTag("selected_juz_banner_2"))
    composeTestRule.onNodeWithTag("selected_juz_banner_2").assertIsDisplayed()
    list.performScrollToNode(hasTestTag("mushaf_surah_card_2"))
    composeTestRule.onNodeWithTag("mushaf_surah_card_2").performClick()
    assertEquals(22, openedPage)

    // Clear Juz filter back to all Surahs
    list.performScrollToNode(hasTestTag("mushaf_juz_quick_bar"))
    composeTestRule
      .onNodeWithTag("mushaf_juz_quick_bar")
      .performScrollToNode(hasTestTag("juz_filter_chip_all"))
    composeTestRule.onNodeWithTag("juz_filter_chip_all").performClick()
    composeTestRule.waitForIdle()

    // Direct page jump
    list.performScrollToNode(hasTestTag("mushaf_page_number_input"))
    composeTestRule.onNodeWithTag("mushaf_page_number_input").performTextInput("293")
    composeTestRule.onNodeWithTag("mushaf_page_jump_button").performClick()
    assertEquals(293, openedPage)

    // Switch to Juz tab, verify Juz card click & "عرض السور" button
    list.performScrollToNode(hasTestTag("mushaf_index_tab_juz"))
    composeTestRule.onNodeWithTag("mushaf_index_tab_juz").performClick()
    composeTestRule.waitForIdle()
    list.performScrollToNode(hasTestTag("mushaf_juz_card_1"))
    composeTestRule.onNodeWithTag("mushaf_juz_card_1").performClick()
    assertEquals(1, openedPage)

    composeTestRule.onNodeWithTag("juz_show_surahs_button_1").performClick()
    composeTestRule.waitForIdle()
    list.performScrollToNode(hasTestTag("selected_juz_banner_1"))
    composeTestRule.onNodeWithTag("selected_juz_banner_1").assertIsDisplayed()
  }

  @Test
  fun `mushaf 604 page reader supports quick jump long press selection and share choice flow`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = MushafPageRepository(context)

    composeTestRule.setContent {
      androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides
          androidx.compose.ui.unit.LayoutDirection.Rtl
      ) {
        MushafAlTilawahTheme {
          MushafPageReaderScreen(
            initialPage = 1,
            bookmarkedPage = 1,
            hasBookmark = false,
            getPageMeta = repo::getPageMeta,
            getPageBounds = repo::getPageBounds,
            onPageChanged = {},
            onResolveSelectedAyahs = { _, keys ->
              val resolved =
                keys.map { k ->
                  MushafAyah(
                    numberInSurah = k.ayahNumber,
                    globalNumber = k.ayahNumber,
                    textAr = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                    tafsirMuyassarAr = "أبتدئ قراءة القرآن باسم الله مستعيناً به.",
                    juz = 1,
                    page = 1,
                  )
                }
              Result.success(resolved)
            },
            onSavePageBookmark = { _, _, _ -> },
            onShowFeedback = {},
            onBackClick = {},
          )
        }
      }
    }

    composeTestRule.onNodeWithTag("mushaf_604_horizontal_pager").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mushaf_page_canvas_1").assertIsDisplayed()

    // Long-press on page 1 Ayah 1 center coordinate to trigger interactive selection toolbar
    composeTestRule.onNodeWithTag("mushaf_page_interactive_surface_1").performTouchInput {
      val targetOffset = Offset(centerX, height * (294f / 2038f))
      longClick(targetOffset)
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("mushaf_604_selection_toolbar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("selection_extend_next_ayah_button").performClick()
    composeTestRule.waitForIdle()

    // Verify Tafsir button opens Al-Tafsir Al-Muyassar sheet
    composeTestRule.onNodeWithTag("selection_action_tafsir").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("mushaf_selected_tafsir_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("selected_tafsir_close").performClick()
    composeTestRule.waitForIdle()

    // Verify Share opens unified Modal Sheet with Text & Image tabs, Surah & range controls, and actions
    composeTestRule.onNodeWithTag("selection_action_share").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("ayah_share_image_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("choose_share_as_image").assertIsDisplayed()
    composeTestRule.onNodeWithTag("choose_share_as_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("share_surah_selector").assertIsDisplayed()
    composeTestRule.onNodeWithTag("share_from_ayah_value").assertIsDisplayed()
    composeTestRule.onNodeWithTag("share_to_ayah_value").assertIsDisplayed()

    composeTestRule.onNodeWithTag("save_ayah_image_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm_share_ayah_image_button").performScrollTo().assertIsDisplayed()

    // Switch to Text tab and verify formatted text preview and Copy/Share Text buttons
    composeTestRule.onNodeWithTag("choose_share_as_text").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("ayah_share_text_preview").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("copy_ayah_text_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm_share_ayah_text_button").performScrollTo().assertIsDisplayed()

    assertEquals(
      "نسألكم الدعاء للمغفور له بإذن الله الحاج عبد العظيم رجب زكي",
      AyahShareCardGenerator.FOOTER_FULL_DUA_REQUEST,
    )
  }

  @Test
  fun `ayah share bitmap renders clean warm mushaf paper card with header and separated footer`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val surah = QuranCatalog.findSurah(1)!!
    val ayahs =
      listOf(
        MushafAyah(
          numberInSurah = 1,
          globalNumber = 1,
          textAr = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
          tafsirMuyassarAr = "أبتدئ قراءة القرآن باسم الله مستعيناً به.",
          juz = 1,
          page = 1,
        ),
        MushafAyah(
          numberInSurah = 2,
          globalNumber = 2,
          textAr = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ",
          tafsirMuyassarAr = "الثناء على الله بصفاته التي كلها أوصاف كمال.",
          juz = 1,
          page = 1,
        ),
      )

    val lightBitmap =
      AyahShareCardGenerator.renderAyahShareBitmap(
        context = context,
        surah = surah,
        ayahs = ayahs,
        includeTafsir = false,
        useDarkTheme = false,
      )
    val lightWithTafsirBitmap =
      AyahShareCardGenerator.renderAyahShareBitmap(
        context = context,
        surah = surah,
        ayahs = ayahs,
        includeTafsir = true,
        useDarkTheme = false,
      )
    val darkBitmap =
      AyahShareCardGenerator.renderAyahShareBitmap(
        context = context,
        surah = surah,
        ayahs = ayahs,
        includeTafsir = true,
        useDarkTheme = true,
      )

    assertEquals(1080, lightBitmap.width)
    assertEquals(1080, darkBitmap.width)
    assertTrue(lightBitmap.height >= 860)
    assertFalse(lightBitmap.isRecycled)
    assertFalse(darkBitmap.isRecycled)
    assertEquals(lightWithTafsirBitmap.height, darkBitmap.height)
    assertTrue(
      "Card with Tafsir must be at least as tall as card without Tafsir",
      lightWithTafsirBitmap.height >= lightBitmap.height,
    )
    assertEquals("الآيات ١ - ٢", AyahShareCardGenerator.buildAyahOnlyRangeBadge(ayahs))
    assertEquals("الآية ١", AyahShareCardGenerator.buildAyahOnlyRangeBadge(listOf(ayahs.first())))
    val textWithTafsir = AyahShareCardGenerator.buildShareTextFormatted(surah, ayahs, includeTafsir = true)
    assertTrue(textWithTafsir.contains("التفسير الميسر"))
    assertTrue(textWithTafsir.contains(AyahShareCardGenerator.FOOTER_FULL_DUA_REQUEST))
  }

  @Test
  fun `ayah share modal supports switching surah and ayah range dynamically`() {
    val fatihah = QuranCatalog.findSurah(1)!!
    val initialAyahs =
      listOf(
        MushafAyah(1, 1, "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "تفسير ١", 1, 1),
        MushafAyah(2, 2, "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ", "تفسير ٢", 1, 1),
      )
    var requestedSurah: Int? = null

    composeTestRule.setContent {
      androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides
          androidx.compose.ui.unit.LayoutDirection.Rtl
      ) {
        MushafAlTilawahTheme {
          com.example.ui.components.AyahShareModalSheet(
            surah = fatihah,
            ayahs = initialAyahs,
            initialDarkTheme = false,
            loadSurahAyahs = { surahNum ->
              requestedSurah = surahNum
              Result.success(
                listOf(
                  MushafAyah(1, 6222, "قُلْ هُوَ ٱللَّهُ أَحَدٌ", "قل هو الله الواحد الأحد", 30, 604),
                  MushafAyah(2, 6223, "ٱللَّهُ ٱلصَّمَدُ", "الله المقصود في قضاء الحوائج", 30, 604),
                )
              )
            },
            onDismiss = {},
            onShareFailure = {},
          )
        }
      }
    }

    composeTestRule.onNodeWithTag("ayah_share_image_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("share_surah_selector").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("share_surah_picker_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("share_surah_search_input").performTextInput("الإخلاص")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("share_pick_surah_112").performClick()
    composeTestRule.waitForIdle()

    assertEquals(112, requestedSurah)
    composeTestRule.onNodeWithTag("share_to_ayah_inc").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("ayah_share_preview_image").performScrollTo().assertIsDisplayed()
  }
}
