package com.example.model

enum class ReciterFilter(val labelAr: String) {
  ALL("الكل"),
  FAVORITES("المفضلة"),
  HAFS("حفص عن عاصم"),
  WARSH("ورش عن نافع"),
  QALON_DORI("قالون والدوري"),
  MOJAWWAD("المصحف المجود"),
}

data class Riwayah(
  val nameAr: String,
  val baseUrl: String,
  val fallbackBaseUrls: List<String> = emptyList(),
  val availableSurahs: Set<Int>? = null,
) {
  fun surahUrl(surahNumber: Int): String = formatSurahUrl(baseUrl, surahNumber)

  fun candidateSurahUrls(surahNumber: Int): List<String> {
    val allBases = (listOf(baseUrl) + fallbackBaseUrls).distinct()
    return allBases.map { formatSurahUrl(it, surahNumber) }
  }

  fun isSurahListed(surahNumber: Int): Boolean {
    return availableSurahs == null || availableSurahs.contains(surahNumber)
  }

  private fun formatSurahUrl(base: String, surahNumber: Int): String {
    val normalizedBase = if (base.endsWith("/")) base else "$base/"
    val padded = surahNumber.toString().padStart(3, '0')
    return "$normalizedBase$padded.mp3"
  }
}

data class Reciter(
  val id: String,
  val nameAr: String,
  val isMostListened: Boolean = false,
  val wikipediaTitles: List<String>,
  val enWikipediaTitles: List<String> = emptyList(),
  val riwayat: List<Riwayah>,
)

data class Surah(
  val number: Int,
  val nameAr: String,
  val revelationTypeAr: String,
  val ayahCount: Int,
) {
  val paddedNumber: String
    get() = number.toString().padStart(3, '0')
}

data class DailyRecitation(
  val reciterId: String,
  val riwayahIndex: Int,
  val surahNumber: Int,
  val highlightNoteAr: String,
)

data class MemorialDua(
  val id: String,
  val titleAr: String,
  val textAr: String,
  val sourceAr: String,
)

enum class AppTab(val labelAr: String) {
  HOME("الرئيسية"),
  RECITERS("القراء"),
  RADIO("الإذاعة"),
  SADAQAH("صدقة جارية"),
}

sealed interface AppRoute {
  data class Tab(val tab: AppTab) : AppRoute
  data class ReciterDetail(val reciterId: String) : AppRoute
  data object Favorites : AppRoute
  data object Settings : AppRoute
  data object QuranMushaf : AppRoute
  data class SurahReader(val surahNumber: Int) : AppRoute
  data class MushafPageReader(val initialPage: Int = 1) : AppRoute
  data object Adhkar : AppRoute
}

enum class DhikrCategory(
  val id: String,
  val titleAr: String,
  val subtitleAr: String,
) {
  MORNING("morning", "أذكار الصباح", "من طلوع الفجر حتى الضحى"),
  EVENING("evening", "أذكار المساء", "من العصر حتى الليل"),
  POST_PRAYER("post_prayer", "أذكار بعد الصلاة", "ما يقال عقب السلام من الفريضة"),
  SLEEP("sleep", "أذكار النوم", "حصن المسلم قبل المنام"),
  WAKING_UP("waking_up", "أذكار الاستيقاظ", "عند القيام من النوم"),
  MOSQUE_WUDU("mosque_wudu", "الوضوء والمسجد", "أذكار الطهارة وبيوت الله"),
  HOME_FOOD_TRAVEL("home_food_travel", "المنزل والطعام والسفر", "أذكار الحياة اليومية"),
  QURANIC_DUAS("quranic_duas", "جوامع الدعاء", "أدعية قرآنية ونبوية مأثورة"),
}

data class DhikrItem(
  val id: String,
  val category: DhikrCategory,
  val titleAr: String,
  val textAr: String,
  val targetCount: Int,
  val sourceAr: String,
  val virtueAr: String? = null,
)

data class MushafAyah(
  val numberInSurah: Int,
  val globalNumber: Int,
  val textAr: String,
  val tafsirMuyassarAr: String,
  val juz: Int,
  val page: Int,
)

data class MushafPageAyahEntry(
  val surahNumber: Int,
  val ayah: MushafAyah,
)

data class AyahLineBound(
  val surahNumber: Int,
  val ayahNumber: Int,
  val minX: Int,
  val minY: Int,
  val maxX: Int,
  val maxY: Int,
)

data class MushafPageMeta(
  val pageNumber: Int,
  val juzNumber: Int,
  val firstSurahNumber: Int,
  val firstAyahNumber: Int,
  val surahNumbers: List<Int>,
)

data class SelectedAyahKey(
  val surahNumber: Int,
  val ayahNumber: Int,
) : Comparable<SelectedAyahKey> {
  override fun compareTo(other: SelectedAyahKey): Int {
    val surahCmp = surahNumber.compareTo(other.surahNumber)
    return if (surahCmp != 0) surahCmp else ayahNumber.compareTo(other.ayahNumber)
  }
}

data class MushafSurahContent(
  val surah: Surah,
  val ayahs: List<MushafAyah>,
)

sealed interface MushafReaderUiState {
  data class Loading(val surahNumber: Int) : MushafReaderUiState
  data class Success(
    val content: MushafSurahContent,
    val isFromCache: Boolean,
  ) : MushafReaderUiState
  data class Error(
    val surahNumber: Int,
    val messageAr: String,
  ) : MushafReaderUiState
}

enum class FontScaleOption(val labelAr: String, val scaleFactor: Float) {
  SMALL("صغير", 0.90f),
  MEDIUM("متوسط", 1.00f),
  LARGE("كبير", 1.15f),
  EXTRA_LARGE("كبير جداً", 1.30f),
}

enum class SleepTimerOption(val labelAr: String, val durationMinutes: Int?) {
  OFF("إيقاف المؤقت", null),
  MIN_15("15 دقيقة", 15),
  MIN_30("30 دقيقة", 30),
  MIN_45("45 دقيقة", 45),
  MIN_60("60 دقيقة", 60),
  END_OF_SURAH("نهاية السورة الحالية", -1),
}

enum class DailyWirdGoal(val labelAr: String, val descriptionAr: String) {
  PAGE("صفحة واحدة", "قراءة أو استماع صفحة واحدة يومياً"),
  QUARTER_HIZB("ربع حزب", "ورد ميسّر بنحو صفحتين ونصف يومياً"),
  HALF_HIZB("نصف حزب", "نحو ٥ صفحات يومياً من كتاب الله"),
  ONE_HIZB("حزب كامل", "نحو ١٠ صفحات يومياً بانتظام"),
  ONE_JUZ("جزء كامل", "٢٠ صفحة يومياً لختم القرآن في شهر"),
}

sealed interface ActiveAudioTarget {
  data class SurahTrack(
    val reciter: Reciter,
    val riwayah: Riwayah,
    val surah: Surah,
  ) : ActiveAudioTarget {
    val trackKey: String
      get() = "${reciter.id}|${riwayah.baseUrl}|${surah.number}"
  }

  data object CairoRadio : ActiveAudioTarget
}

data class PlaybackUiState(
  val activeTarget: ActiveAudioTarget? = null,
  val isPlaying: Boolean = false,
  val isBuffering: Boolean = false,
  val currentPositionMs: Long = 0L,
  val bufferedPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val unavailableTrackKeys: Set<String> = emptySet(),
  val errorMessage: String? = null,
  val sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
  val sleepTimerRemainingMs: Long? = null,
) {
  val progressFraction: Float
    get() =
      if (durationMs > 0L) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
      else 0f

  val bufferedFraction: Float
    get() =
      if (durationMs > 0L) (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
      else 0f
}
