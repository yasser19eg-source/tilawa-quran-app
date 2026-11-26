package com.example

import com.example.data.AdhkarCatalog
import com.example.data.AlQuranAyahDto
import com.example.data.Mp3QuranMoshafDto
import com.example.data.Mp3QuranRadioDto
import com.example.data.Mp3QuranReciterDto
import com.example.data.MushafPageRepository
import com.example.data.QuranCatalog
import com.example.data.QuranMushafRepository
import com.example.data.ReciterCatalogRepository
import com.example.data.UserPreferencesRepository
import com.example.data.UserPreferencesState
import com.example.data.WikipediaPortraitRepository
import com.example.model.AyahLineBound
import com.example.model.DhikrCategory
import com.example.model.MushafAyah
import com.example.model.PlaybackUiState
import com.example.model.Riwayah
import com.example.model.SelectedAyahKey
import com.example.notifications.DailyWirdReminderScheduler
import com.example.ui.components.AyahShareCardGenerator
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
    assertTrue(
      husary.riwayat.first().fallbackBaseUrls.contains("http://server14.mp3quran.net/husr/")
    )
    assertEquals(setOf(1, 2, 18, 114), husary.riwayat.first().availableSurahs)

    val bandar = merged.first { it.id == "mp3q_205" }
    assertEquals("الشيخ بندر بليلة", bandar.nameAr)
    assertEquals("https://server6.mp3quran.net/balilah/", bandar.riwayat.first().baseUrl)
    assertTrue(bandar.riwayat.first().fallbackBaseUrls.contains("http://server6.mp3quran.net/balilah/"))
    assertEquals(setOf(1, 2, 3), bandar.riwayat.first().availableSurahs)
    assertTrue(bandar.wikipediaTitles.contains("بندر بليلة"))
  }

  @Test
  fun extract_cairo_radio_urls_prioritizes_live_api_url_with_http_and_curated_mirrors() {
    val radios =
      listOf(
        Mp3QuranRadioDto(
          id = 1,
          name = "إذاعة مشاري العفاسي",
          url = "https://backup.qurango.net/radio/mishary",
        ),
        Mp3QuranRadioDto(
          id = 2,
          name = "إذاعة القرآن الكريم من القاهرة",
          url = "http://live.cairo-radio.example.net/stream",
        ),
      )

    val extracted = ReciterCatalogRepository.extractCairoRadioUrls(radios)
    assertTrue(extracted.contains("https://live.cairo-radio.example.net/stream"))
    assertTrue(extracted.contains("http://live.cairo-radio.example.net/stream"))
    assertTrue(extracted.contains(QuranCatalog.CAIRO_RADIO_STREAM_URL))
    assertTrue(extracted.contains("http://stream.radiojar.com/8s5u5tpdtwzuv"))
    assertEquals("https://live.cairo-radio.example.net/stream", extracted.first())
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

  @Test
  fun adhkar_catalog_covers_all_categories_with_valid_targets() {
    DhikrCategory.entries.forEach { category ->
      val items = AdhkarCatalog.itemsForCategory(category)
      assertTrue("Category ${category.titleAr} must not be empty", items.isNotEmpty())
      items.forEach { item ->
        assertTrue(item.textAr.isNotBlank())
        assertTrue(item.targetCount > 0)
      }
    }
  }

  @Test
  fun mushaf_repository_merges_uthmani_with_tafsir_muyassar_and_handles_basmalah() {
    // Surah 1 preserves Basmalah as Ayah 1
    val fatihah =
      QuranMushafRepository.mergeEditions(
        surahNumber = 1,
        uthmaniAyahs =
          listOf(
            AlQuranAyahDto(
              number = 1,
              text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
              numberInSurah = 1,
              juz = 1,
              page = 1,
            )
          ),
        tafsirAyahs =
          listOf(
            AlQuranAyahDto(
              number = 1,
              text = "أبتدئ قراءة القرآن باسم الله...",
              numberInSurah = 1,
              juz = 1,
              page = 1,
            )
          ),
      )
    assertEquals(1, fatihah.size)
    assertTrue(fatihah.first().textAr.contains("بِسْمِ"))
    assertEquals("أبتدئ قراءة القرآن باسم الله...", fatihah.first().tafsirMuyassarAr)

    // Surah 2 strips leading Basmalah from Ayah 1 cleanly
    val baqarah =
      QuranMushafRepository.mergeEditions(
        surahNumber = 2,
        uthmaniAyahs =
          listOf(
            AlQuranAyahDto(
              number = 8,
              text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ الٓمٓ",
              numberInSurah = 1,
              juz = 1,
              page = 2,
            )
          ),
        tafsirAyahs =
          listOf(
            AlQuranAyahDto(
              number = 8,
              text = "هذه الحروف وغيرها...",
              numberInSurah = 1,
              juz = 1,
              page = 2,
            )
          ),
      )
    assertEquals("الٓمٓ", baqarah.first().textAr)
    assertEquals("هذه الحروف وغيرها...", baqarah.first().tafsirMuyassarAr)
  }

  @Test
  fun mushaf_page_repository_maps_all_114_surahs_and_30_juzs_to_604_pages() {
    assertEquals(114, MushafPageRepository.SURAH_START_PAGES.size)
    assertEquals(30, MushafPageRepository.JUZ_START_PAGES.size)
    assertEquals(1, MushafPageRepository.startPageForSurah(1))
    assertEquals(2, MushafPageRepository.startPageForSurah(2))
    assertEquals(293, MushafPageRepository.startPageForSurah(18))
    assertEquals(604, MushafPageRepository.startPageForSurah(114))
    assertEquals(1, MushafPageRepository.startPageForJuz(1))
    assertEquals(282, MushafPageRepository.startPageForJuz(15))
    assertEquals(582, MushafPageRepository.startPageForJuz(30))
    assertEquals(
      "https://files.quran.app/hafs/madani/width_1260/page003.png",
      MushafPageRepository.candidatePageImageUrls(3).first(),
    )
  }

  @Test
  fun mushaf_coordinate_hit_testing_and_consecutive_range_selection_work_accurately() {
    val page1Bounds =
      listOf(
        AyahLineBound(1, 1, 410, 254, 854, 333),
        AyahLineBound(1, 2, 318, 365, 945, 442),
        AyahLineBound(1, 3, 648, 474, 1009, 552),
        AyahLineBound(1, 4, 254, 474, 649, 548),
      )
    val hitAyah1 =
      MushafPageRepository.findAyahAtNormalizedPoint(
        bounds = page1Bounds,
        normX = 630f / MushafPageRepository.MUSHAF_IMAGE_WIDTH,
        normY = 290f / MushafPageRepository.MUSHAF_IMAGE_HEIGHT,
      )
    assertEquals(SelectedAyahKey(1, 1), hitAyah1)

    val hitAyah4 =
      MushafPageRepository.findAyahAtNormalizedPoint(
        bounds = page1Bounds,
        normX = 450f / MushafPageRepository.MUSHAF_IMAGE_WIDTH,
        normY = 510f / MushafPageRepository.MUSHAF_IMAGE_HEIGHT,
      )
    assertEquals(SelectedAyahKey(1, 4), hitAyah4)

    val range =
      MushafPageRepository.computeConsecutiveRange(
        anchor = SelectedAyahKey(1, 1),
        target = SelectedAyahKey(1, 4),
      )
    assertEquals(
      listOf(
        SelectedAyahKey(1, 1),
        SelectedAyahKey(1, 2),
        SelectedAyahKey(1, 3),
        SelectedAyahKey(1, 4),
      ),
      range,
    )

    // Cross-surah tap starts new Surah selection cleanly
    val crossSurah =
      MushafPageRepository.computeConsecutiveRange(
        anchor = SelectedAyahKey(112, 4),
        target = SelectedAyahKey(113, 1),
      )
    assertEquals(listOf(SelectedAyahKey(113, 1)), crossSurah)
  }

  @Test
  fun ayah_share_formatted_text_includes_brand_and_memorial_dua() {
    val surah = QuranCatalog.findSurah(1)!!
    val ayahs =
      listOf(
        MushafAyah(
          numberInSurah = 1,
          globalNumber = 1,
          textAr = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
          tafsirMuyassarAr = "أبتدئ قراءة القرآن باسم الله...",
          juz = 1,
          page = 1,
        )
      )
    val text = AyahShareCardGenerator.buildShareTextFormatted(surah, ayahs)
    assertTrue(text.contains("تلاوة"))
    assertTrue(text.contains("نسألكم الدعاء للمغفور له بإذن الله الحاج عبد العظيم رجب زكي"))
  }

  @Test
  fun surah_and_juz_index_ranges_are_complete_and_consistent() {
    assertEquals(114, MushafPageRepository.SURAH_END_PAGES.size)
    assertEquals(114, MushafPageRepository.SURAH_FIRST_JUZ.size)
    assertEquals(114, MushafPageRepository.SURAH_LAST_JUZ.size)
    assertEquals(30, MushafPageRepository.JUZ_END_PAGES.size)

    for (surah in 1..114) {
      val start = MushafPageRepository.startPageForSurah(surah)
      val end = MushafPageRepository.endPageForSurah(surah)
      val juzRange = MushafPageRepository.juzRangeForSurah(surah)
      assertTrue("Surah $surah start <= end", start in 1..end && end <= 604)
      assertTrue("Surah $surah juz range valid", juzRange.first in 1..30 && juzRange.last in juzRange.first..30)
    }

    assertEquals(1..3, MushafPageRepository.juzRangeForSurah(2))
    assertEquals(15..16, MushafPageRepository.juzRangeForSurah(18))
    assertEquals(22, MushafPageRepository.surahOpenPageInJuz(2, 2))
    assertEquals(302, MushafPageRepository.surahOpenPageInJuz(18, 16))
    assertEquals(listOf(1, 2), MushafPageRepository.surahsInJuz(1).map { it.number })
    assertEquals(37, MushafPageRepository.surahsInJuz(30).size)
  }

  @Test
  fun warm_mushaf_paper_and_calm_sage_palette_tokens_match_specification() {
    assertFalse(UserPreferencesState().isDarkMode)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFF6F3EC), com.example.ui.theme.LightBackground)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFFFFFFF), com.example.ui.theme.LightSurface)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFEFECE3), com.example.ui.theme.LightSurfaceVariant)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFDCD6C8), com.example.ui.theme.LightOutline)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF2E6F5E), com.example.ui.theme.LightOlivePrimary)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFB8924A), com.example.ui.theme.WarmGoldMuted)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF1E2A25), com.example.ui.theme.LightTextPrimary)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF5A6962), com.example.ui.theme.LightTextSecondary)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF232624), com.example.ui.theme.MushafPrintedInkLight)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFEDE8DC), com.example.ui.theme.MushafPrintedInkDark)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF1E2B25), com.example.ui.theme.MushafCardDarkBg)
  }
}
