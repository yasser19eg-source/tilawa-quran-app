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
