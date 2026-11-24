package com.example

import com.example.data.Mp3QuranMoshafDto
import com.example.data.Mp3QuranReciterDto
import com.example.data.QuranCatalog
import com.example.data.ReciterCatalogRepository
import com.example.data.UserPreferencesRepository
import com.example.data.WikipediaPortraitRepository
import com.example.model.PlaybackUiState
import com.example.model.Riwayah
import com.example.notifications.DailyWirdReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

  @Test
  fun catalog_contains_exact_20_verified_reciters_and_excludes_unverified() {
    assertEquals(20, QuranCatalog.reciters.size)
    val names = QuranCatalog.reciters.map { it.nameAr }
    assertFalse(names.any { it.contains("علاء عقل") })
    assertFalse(names.any { it.contains("الشحات") })
  }

  @Test
  fun catalog_contains_all_114_surahs_and_formats_mp3_urls_correctly() {
    assertEquals(114, QuranCatalog.surahs.size)
    assertEquals("الفاتحة", QuranCatalog.surahs.first().nameAr)
    assertEquals("الناس", QuranCatalog.surahs.last().nameAr)

    val husary = QuranCatalog.findReciter("husary")!!
    assertEquals(4, husary.riwayat.size)
    assertEquals("https://server13.mp3quran.net/husr/001.mp3", husary.riwayat[0].surahUrl(1))
    assertEquals("https://server13.mp3quran.net/husr/114.mp3", husary.riwayat[0].surahUrl(114))

    val nourin = QuranCatalog.findReciter("nourin")!!
    assertEquals(
      "https://server16.mp3quran.net/nourin_siddig/Rewayat-Aldori-A-n-Abi-Amr/018.mp3",
      nourin.riwayat.first().surahUrl(18),
    )
  }

  @Test
  fun daily_recitation_rotates_with_day_of_year() {
    val day1 = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1) }
    val day2 = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 2) }
    val rec1 = QuranCatalog.todayRecitation(day1)
    val rec2 = QuranCatalog.todayRecitation(day2)
    assertTrue(rec1 != rec2)
  }

  @Test
  fun arabic_normalization_matches_hamza_and_ta_marbuta_variants() {
    val norm = QuranCatalog.normalizeArabic("إبراهيم")
    assertEquals(QuranCatalog.normalizeArabic("ابراهيم"), norm)
  }

  @Test
  fun riwayah_candidate_surah_urls_include_primary_and_fallbacks_and_checks_listed_surahs() {
    val riwayah =
      Riwayah(
        nameAr = "حفص عن عاصم",
        baseUrl = "https://server15.mp3quran.net/husr/",
        fallbackBaseUrls = listOf("https://server13.mp3quran.net/husr/"),
        availableSurahs = setOf(1, 18, 114),
      )

    val candidates = riwayah.candidateSurahUrls(18)
    assertEquals(
      listOf(
        "https://server15.mp3quran.net/husr/018.mp3",
        "https://server13.mp3quran.net/husr/018.mp3",
      ),
      candidates,
    )
    assertTrue(riwayah.isSurahListed(18))
    assertFalse(riwayah.isSurahListed(2))
  }

  @Test
  fun merge_catalog_updates_server_urls_preserves_fallbacks_filters_invalid_and_adds_extra_reciters() {
    val apiReciters =
      listOf(
        // Curated reciter with updated server mirror
        Mp3QuranReciterDto(
          id = 118,
          name = "محمود خليل الحصري",
          moshaf =
            listOf(
              Mp3QuranMoshafDto(
                id = 1,
                name = "حفص عن عاصم - مرتل",
                server = "http://server14.mp3quran.net/husr/",
                surahTotal = 114,
                surahList = "1,2,18,114",
              )
            ),
        ),
        // Invalid reciter with blank server and empty surah list (must be filtered out)
        Mp3QuranReciterDto(
          id = 9999,
          name = "قارئ بدون خادم",
          moshaf =
            listOf(
              Mp3QuranMoshafDto(
                id = 99,
                name = "حفص عن عاصم",
                server = "",
                surahTotal = 0,
                surahList = "",
              )
            ),
        ),
        // Additional verified reciter
        Mp3QuranReciterDto(
          id = 205,
          name = "الشيخ بندر بليلة",
          moshaf =
            listOf(
              Mp3QuranMoshafDto(
                id = 2051,
                name = "حفص عن عاصم",
                server = "https://server6.mp3quran.net/balilah/",
                surahTotal = 114,
                surahList = "1,2,3",
              )
            ),
        ),
      )

    val merged = ReciterCatalogRepository.mergeCatalog(QuranCatalog.reciters, apiReciters)
    assertEquals(21, merged.size)

    val husary = merged.first { it.id == "husary" }
    assertEquals("https://server14.mp3quran.net/husr/", husary.riwayat.first().baseUrl)
    assertTrue(
      husary.riwayat.first().fallbackBaseUrls.contains("https://server13.mp3quran.net/husr/")
    )
    assertEquals(setOf(1, 2, 18, 114), husary.riwayat.first().availableSurahs)

    val bandar = merged.first { it.id == "mp3q_205" }
    assertEquals("الشيخ بندر بليلة", bandar.nameAr)
    assertEquals("https://server6.mp3quran.net/balilah/", bandar.riwayat.first().baseUrl)
    assertEquals(setOf(1, 2, 3), bandar.riwayat.first().availableSurahs)
    assertTrue(bandar.wikipediaTitles.contains("بندر بليلة"))
  }

  @Test
  fun portrait_image_validator_rejects_svg_and_flags_and_accepts_photos() {
    assertFalse(WikipediaPortraitRepository.isLikelyPortraitImage("https://upload.wikimedia.org/flag_of_egypt.svg"))
    assertFalse(WikipediaPortraitRepository.isLikelyPortraitImage("https://upload.wikimedia.org/Flag_of_Saudi_Arabia.png"))
    assertTrue(WikipediaPortraitRepository.isLikelyPortraitImage("https://upload.wikimedia.org/wikipedia/commons/thumb/a/b/Al-Hussary.jpg/320px-Al-Hussary.jpg"))
  }

  @Test
  fun daily_wird_streak_handles_consecutive_days_gaps_and_undo() {
    val firstDay = UserPreferencesRepository.computeWirdToggleResult(null, 0, 500L)
    assertEquals(500L to 1, firstDay)

    val consecutive = UserPreferencesRepository.computeWirdToggleResult(500L, 1, 501L)
    assertEquals(501L to 2, consecutive)

    val afterGap = UserPreferencesRepository.computeWirdToggleResult(501L, 2, 505L)
    assertEquals(505L to 1, afterGap)

    val undoConsecutive = UserPreferencesRepository.computeWirdToggleResult(501L, 2, 501L)
    assertEquals(500L to 1, undoConsecutive)
  }

  @Test
  fun daily_wird_scheduler_computes_future_trigger_and_formats_arabic_time() {
    val cal =
      Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 24, 10, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }
    val nowMs = cal.timeInMillis

    val laterToday = DailyWirdReminderScheduler.computeNextTriggerTimeMillis(18, 30, nowMs)
    assertTrue(laterToday > nowMs)
    val laterCal = Calendar.getInstance().apply { timeInMillis = laterToday }
    assertEquals(24, laterCal.get(Calendar.DAY_OF_MONTH))
    assertEquals(18, laterCal.get(Calendar.HOUR_OF_DAY))
    assertEquals(30, laterCal.get(Calendar.MINUTE))

    val tomorrow = DailyWirdReminderScheduler.computeNextTriggerTimeMillis(8, 0, nowMs)
    assertTrue(tomorrow > nowMs)
    val tomorrowCal = Calendar.getInstance().apply { timeInMillis = tomorrow }
    assertEquals(25, tomorrowCal.get(Calendar.DAY_OF_MONTH))
    assertEquals(8, tomorrowCal.get(Calendar.HOUR_OF_DAY))

    assertEquals("08:00 ص", DailyWirdReminderScheduler.formatArabicTime(8, 0))
    assertEquals("06:30 م", DailyWirdReminderScheduler.formatArabicTime(18, 30))
    assertEquals("12:05 ص", DailyWirdReminderScheduler.formatArabicTime(0, 5))
  }

  @Test
  fun playback_ui_state_computes_progress_and_buffered_fractions_safely() {
    val empty = PlaybackUiState()
    assertEquals(0f, empty.progressFraction, 0.0001f)
    assertEquals(0f, empty.bufferedFraction, 0.0001f)

    val active =
      PlaybackUiState(
        currentPositionMs = 30_000L,
        bufferedPositionMs = 90_000L,
        durationMs = 120_000L,
      )
    assertEquals(0.25f, active.progressFraction, 0.001f)
    assertEquals(0.75f, active.bufferedFraction, 0.001f)

    val clamped =
      PlaybackUiState(
        currentPositionMs = 150_000L,
        bufferedPositionMs = 200_000L,
        durationMs = 120_000L,
      )
    assertEquals(1f, clamped.progressFraction, 0.0001f)
    assertEquals(1f, clamped.bufferedFraction, 0.0001f)
  }
}
