package com.example.data

import android.content.Context
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.model.AyahLineBound
import com.example.model.MushafAyah
import com.example.model.MushafPageAyahEntry
import com.example.model.MushafPageMeta
import com.example.model.SelectedAyahKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class AlQuranPageResponse(
  @Json(name = "code") val code: Int = 0,
  @Json(name = "status") val status: String? = null,
  @Json(name = "data") val data: AlQuranPageDataDto? = null,
)

@JsonClass(generateAdapter = true)
data class AlQuranPageDataDto(
  @Json(name = "number") val number: Int = 1,
  @Json(name = "ayahs") val ayahs: List<AlQuranPageAyahDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class AlQuranPageAyahDto(
  @Json(name = "number") val number: Int = 0,
  @Json(name = "text") val text: String = "",
  @Json(name = "surah") val surah: AlQuranPageSurahRefDto? = null,
  @Json(name = "numberInSurah") val numberInSurah: Int = 0,
  @Json(name = "juz") val juz: Int = 1,
  @Json(name = "page") val page: Int = 1,
)

@JsonClass(generateAdapter = true)
data class AlQuranPageSurahRefDto(
  @Json(name = "number") val number: Int = 1,
  @Json(name = "name") val name: String = "",
)

@JsonClass(generateAdapter = true)
data class CachedMushafPagePayload(
  @Json(name = "pageNumber") val pageNumber: Int,
  @Json(name = "entries") val entries: List<CachedPageAyahEntryDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CachedPageAyahEntryDto(
  @Json(name = "surahNumber") val surahNumber: Int,
  @Json(name = "numberInSurah") val numberInSurah: Int,
  @Json(name = "globalNumber") val globalNumber: Int,
  @Json(name = "textAr") val textAr: String,
  @Json(name = "tafsirMuyassarAr") val tafsirMuyassarAr: String,
  @Json(name = "juz") val juz: Int,
  @Json(name = "page") val page: Int,
)

interface AlQuranCloudPageApi {
  @GET("page/{pageNumber}/{edition}")
  suspend fun getPageEdition(
    @Path("pageNumber") pageNumber: Int,
    @Path("edition") edition: String,
  ): AlQuranPageResponse
}

class MushafPageRepository(
  private val context: Context? = null,
  private val surahRepository: QuranMushafRepository? = null,
  private val pageApiOverride: AlQuranCloudPageApi? = null,
) {

  private val mutex = Mutex()
  private val pageAyahMemoryCache = mutableMapOf<Int, Map<SelectedAyahKey, MushafPageAyahEntry>>()
  private var boundsByPage: Map<Int, List<AyahLineBound>>? = null
  private var metaByPage: Map<Int, MushafPageMeta>? = null

  private val cacheDir: File? by lazy {
    context?.filesDir?.let { File(it, PAGE_TEXT_CACHE_DIR).apply { mkdirs() } }
  }

  private val moshi: Moshi by lazy {
    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  }

  private val cachedPageAdapter by lazy { moshi.adapter(CachedMushafPagePayload::class.java) }

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .build()
  }

  private val primaryPageApi: AlQuranCloudPageApi by lazy {
    pageApiOverride ?: createPageApi("https://api.alquran.cloud/v1/")
  }

  private val fallbackHttpPageApi: AlQuranCloudPageApi by lazy {
    createPageApi("http://api.alquran.cloud/v1/")
  }

  private fun createPageApi(baseUrl: String): AlQuranCloudPageApi {
    return Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(AlQuranCloudPageApi::class.java)
  }

  suspend fun ensureAssetsLoaded() {
    if (boundsByPage != null && metaByPage != null) return
    withContext(Dispatchers.IO) { loadAssetsSyncIfNeeded() }
  }

  private fun loadAssetsSyncIfNeeded() {
    if (boundsByPage != null && metaByPage != null) return
    val appContext = context ?: run {
      boundsByPage = emptyMap()
      metaByPage = emptyMap()
      return
    }

    val loadedBounds = mutableMapOf<Int, MutableList<AyahLineBound>>()
    try {
      appContext.assets.open(BOUNDS_ASSET_FILE).use { raw ->
        BufferedReader(InputStreamReader(raw, Charsets.UTF_8)).useLines { lines ->
          lines.forEach { line ->
            val parts = line.split(',')
            if (parts.size == 7) {
              val page = parts[0].toIntOrNull() ?: return@forEach
              val surah = parts[1].toIntOrNull() ?: return@forEach
              val ayah = parts[2].toIntOrNull() ?: return@forEach
              val minX = parts[3].toIntOrNull() ?: return@forEach
              val minY = parts[4].toIntOrNull() ?: return@forEach
              val maxX = parts[5].toIntOrNull() ?: return@forEach
              val maxY = parts[6].toIntOrNull() ?: return@forEach
              loadedBounds
                .getOrPut(page) { mutableListOf() }
                .add(AyahLineBound(surah, ayah, minX, minY, maxX, maxY))
            }
          }
        }
      }
    } catch (_: Exception) {
    }

    val loadedMeta = mutableMapOf<Int, MushafPageMeta>()
    try {
      appContext.assets.open(META_ASSET_FILE).use { raw ->
        BufferedReader(InputStreamReader(raw, Charsets.UTF_8)).useLines { lines ->
          lines.forEach { line ->
            val parts = line.split('|')
            if (parts.size == 5) {
              val page = parts[0].toIntOrNull() ?: return@forEach
              val juz = parts[1].toIntOrNull() ?: 1
              val firstSurah = parts[2].toIntOrNull() ?: 1
              val firstAyah = parts[3].toIntOrNull() ?: 1
              val surahs = parts[4].split(',').mapNotNull { it.toIntOrNull() }
              loadedMeta[page] =
                MushafPageMeta(
                  pageNumber = page,
                  juzNumber = juz,
                  firstSurahNumber = firstSurah,
                  firstAyahNumber = firstAyah,
                  surahNumbers = surahs.ifEmpty { listOf(firstSurah) },
                )
            }
          }
        }
      }
    } catch (_: Exception) {
    }

    boundsByPage = loadedBounds
    metaByPage = loadedMeta
  }

  fun getPageBounds(pageNumber: Int): List<AyahLineBound> {
    if (boundsByPage == null) loadAssetsSyncIfNeeded()
    return boundsByPage?.get(pageNumber.coerceIn(1, TOTAL_MUSHAF_PAGES)).orEmpty()
  }

  fun getPageMeta(pageNumber: Int): MushafPageMeta {
    val safePage = pageNumber.coerceIn(1, TOTAL_MUSHAF_PAGES)
    if (metaByPage == null) loadAssetsSyncIfNeeded()
    metaByPage?.get(safePage)?.let { return it }
    return fallbackPageMeta(safePage)
  }

  suspend fun loadPageAyahs(
    pageNumber: Int,
    forceRefresh: Boolean = false,
  ): Result<Map<SelectedAyahKey, MushafPageAyahEntry>> {
    val safePage = pageNumber.coerceIn(1, TOTAL_MUSHAF_PAGES)

    if (!forceRefresh) {
      mutex.withLock {
        pageAyahMemoryCache[safePage]?.let { cached ->
          return Result.success(cached)
        }
      }
      val fromDisk = readPageFromDiskCache(safePage)
      if (fromDisk != null && fromDisk.isNotEmpty()) {
        mutex.withLock { pageAyahMemoryCache[safePage] = fromDisk }
        return Result.success(fromDisk)
      }
    }

    return withContext(Dispatchers.IO) {
      val apis =
        if (pageApiOverride != null) listOf(primaryPageApi)
        else listOf(primaryPageApi, fallbackHttpPageApi)
      var lastError: Throwable? = null
      for (api in apis) {
        try {
          val entries = fetchAndMergePage(api, safePage)
          if (entries.isNotEmpty()) {
            val map = entries.associateBy { SelectedAyahKey(it.surahNumber, it.ayah.numberInSurah) }
            mutex.withLock { pageAyahMemoryCache[safePage] = map }
            writePageToDiskCache(safePage, entries)
            return@withContext Result.success(map)
          }
        } catch (t: Throwable) {
          lastError = t
        }
      }
      Result.failure(
        lastError ?: IllegalStateException("تعذر تحميل نصوص وتفسير هذه الصفحة")
      )
    }
  }

  suspend fun resolveSelectedAyahs(
    pageNumber: Int,
    selectedKeys: List<SelectedAyahKey>,
  ): Result<List<MushafAyah>> {
    if (selectedKeys.isEmpty()) return Result.success(emptyList())
    val sorted = selectedKeys.distinct().sorted()
    val pageEntries = loadPageAyahs(pageNumber).getOrNull().orEmpty()

    val missingFromPage = sorted.filter { !pageEntries.containsKey(it) }
    if (missingFromPage.isEmpty()) {
      return Result.success(sorted.mapNotNull { pageEntries[it]?.ayah })
    }

    // Pull remaining Ayahs of the Surah (when selection spans beyond current page)
    val surahNum = sorted.first().surahNumber
    val surahResult = surahRepository?.loadSurah(surahNum)
    val surahAyahMap =
      surahResult?.getOrNull()?.content?.ayahs?.associateBy { it.numberInSurah }.orEmpty()

    val resolved =
      sorted.mapNotNull { key ->
        pageEntries[key]?.ayah
          ?: if (key.surahNumber == surahNum) surahAyahMap[key.ayahNumber] else null
      }

    return if (resolved.isNotEmpty()) {
      Result.success(resolved)
    } else {
      loadPageAyahs(pageNumber, forceRefresh = true).map { refreshed ->
        sorted.mapNotNull { refreshed[it]?.ayah }
      }
    }
  }

  private suspend fun fetchAndMergePage(
    api: AlQuranCloudPageApi,
    pageNumber: Int,
  ): List<MushafPageAyahEntry> = coroutineScope {
    val uthmaniDeferred = async { api.getPageEdition(pageNumber, QuranMushafRepository.UTHMANI_EDITION) }
    val tafsirDeferred = async { api.getPageEdition(pageNumber, QuranMushafRepository.MUYASSAR_EDITION) }
    val uthmaniResp = uthmaniDeferred.await()
    val tafsirResp = tafsirDeferred.await()
    val uthmaniAyahs = uthmaniResp.data?.ayahs.orEmpty()
    val tafsirAyahs = tafsirResp.data?.ayahs.orEmpty()
    mergePageEditions(pageNumber, uthmaniAyahs, tafsirAyahs)
  }

  private suspend fun readPageFromDiskCache(
    pageNumber: Int
  ): Map<SelectedAyahKey, MushafPageAyahEntry>? {
    val dir = cacheDir ?: return null
    val file = File(dir, "page_$pageNumber.json")
    if (!file.exists()) return null
    return withContext(Dispatchers.IO) {
      try {
        val parsed = cachedPageAdapter.fromJson(file.readText()) ?: return@withContext null
        if (parsed.pageNumber != pageNumber || parsed.entries.isEmpty()) return@withContext null
        parsed.entries
          .map { dto ->
            MushafPageAyahEntry(
              surahNumber = dto.surahNumber,
              ayah =
                MushafAyah(
                  numberInSurah = dto.numberInSurah,
                  globalNumber = dto.globalNumber,
                  textAr = dto.textAr,
                  tafsirMuyassarAr = dto.tafsirMuyassarAr,
                  juz = dto.juz,
                  page = dto.page,
                ),
            )
          }
          .associateBy { SelectedAyahKey(it.surahNumber, it.ayah.numberInSurah) }
      } catch (_: Exception) {
        null
      }
    }
  }

  private suspend fun writePageToDiskCache(
    pageNumber: Int,
    entries: List<MushafPageAyahEntry>,
  ) {
    val dir = cacheDir ?: return
    val file = File(dir, "page_$pageNumber.json")
    withContext(Dispatchers.IO) {
      try {
        val payload =
          CachedMushafPagePayload(
            pageNumber = pageNumber,
            entries =
              entries.map { entry ->
                CachedPageAyahEntryDto(
                  surahNumber = entry.surahNumber,
                  numberInSurah = entry.ayah.numberInSurah,
                  globalNumber = entry.ayah.globalNumber,
                  textAr = entry.ayah.textAr,
                  tafsirMuyassarAr = entry.ayah.tafsirMuyassarAr,
                  juz = entry.ayah.juz,
                  page = entry.ayah.page,
                )
              },
          )
        file.writeText(cachedPageAdapter.toJson(payload))
      } catch (_: Exception) {
      }
    }
  }

  fun prefetchAdjacentPages(
    imageLoader: ImageLoader,
    appContext: Context,
    currentPage: Int,
  ) {
    val neighbors = listOf(currentPage - 1, currentPage + 1, currentPage + 2)
    neighbors.forEach { p ->
      if (p in 1..TOTAL_MUSHAF_PAGES) {
        val url = candidatePageImageUrls(p).first()
        val req = ImageRequest.Builder(appContext).data(url).build()
        imageLoader.enqueue(req)
      }
    }
  }

  companion object {
    const val TOTAL_MUSHAF_PAGES = 604
    const val MUSHAF_IMAGE_WIDTH = 1260f
    const val MUSHAF_IMAGE_HEIGHT = 2038f
    private const val BOUNDS_ASSET_FILE = "mushaf_bounds_1260.txt"
    private const val META_ASSET_FILE = "mushaf_page_meta.txt"
    private const val PAGE_TEXT_CACHE_DIR = "mushaf_page_text_cache"

    val SURAH_START_PAGES: IntArray =
      intArrayOf(
        1, 2, 50, 77, 106, 128, 151, 177, 187, 208, 221, 235, 249, 255, 262, 267, 282, 293, 305,
        312, 322, 332, 342, 350, 359, 367, 377, 385, 396, 404, 411, 415, 418, 428, 434, 440, 446,
        453, 458, 467, 477, 483, 489, 496, 499, 502, 507, 511, 515, 518, 520, 523, 526, 528, 531,
        534, 537, 542, 545, 549, 551, 553, 554, 556, 558, 560, 562, 564, 566, 568, 570, 572, 574,
        575, 577, 578, 580, 582, 583, 585, 586, 587, 587, 589, 590, 591, 591, 592, 593, 594, 595,
        595, 596, 596, 597, 597, 598, 598, 599, 599, 600, 600, 601, 601, 601, 602, 602, 602, 603,
        603, 603, 604, 604, 604,
      )

    val SURAH_END_PAGES: IntArray =
      intArrayOf(
        1, 49, 76, 106, 127, 150, 176, 186, 207, 221, 235, 248, 255, 261, 267, 281, 293, 304, 312,
        321, 331, 341, 349, 359, 366, 376, 385, 396, 404, 410, 414, 417, 427, 434, 440, 445, 452,
        458, 467, 476, 482, 489, 495, 498, 502, 506, 510, 515, 517, 520, 523, 525, 528, 531, 534,
        537, 541, 545, 548, 551, 552, 554, 555, 557, 559, 561, 564, 566, 568, 570, 571, 573, 575,
        577, 578, 580, 581, 583, 584, 585, 586, 587, 589, 589, 590, 591, 592, 592, 594, 594, 595,
        596, 596, 596, 597, 597, 598, 599, 599, 600, 600, 600, 601, 601, 601, 602, 602, 602, 603,
        603, 603, 604, 604, 604,
      )

    val SURAH_FIRST_JUZ: IntArray =
      intArrayOf(
        1, 1, 3, 4, 6, 7, 8, 9, 10, 11, 11, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17, 17, 18, 18, 18,
        19, 19, 20, 20, 21, 21, 21, 21, 22, 22, 22, 23, 23, 23, 24, 24, 25, 25, 25, 25, 25, 26, 26,
        26, 26, 26, 27, 27, 27, 27, 27, 27, 28, 28, 28, 28, 28, 28, 28, 28, 28, 29, 29, 29, 29, 29,
        29, 29, 29, 29, 29, 29, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
        30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
      )

    val SURAH_LAST_JUZ: IntArray =
      intArrayOf(
        1, 3, 4, 6, 7, 8, 9, 10, 11, 11, 12, 13, 13, 13, 14, 14, 15, 16, 16, 16, 17, 17, 18, 18, 19,
        19, 20, 20, 21, 21, 21, 21, 22, 22, 22, 23, 23, 23, 24, 24, 25, 25, 25, 25, 25, 26, 26, 26,
        26, 26, 27, 27, 27, 27, 27, 27, 27, 28, 28, 28, 28, 28, 28, 28, 28, 28, 29, 29, 29, 29, 29,
        29, 29, 29, 29, 29, 29, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
        30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
      )

    val JUZ_START_PAGES: IntArray =
      intArrayOf(
        1, 22, 42, 62, 82, 102, 121, 142, 162, 182, 201, 222, 242, 262, 282, 302, 322, 342, 362,
        382, 402, 422, 442, 462, 482, 502, 522, 542, 562, 582,
      )

    val JUZ_END_PAGES: IntArray =
      intArrayOf(
        21, 41, 62, 81, 101, 121, 141, 161, 181, 201, 221, 241, 261, 281, 301, 321, 341, 361, 381,
        401, 421, 441, 461, 481, 502, 521, 541, 561, 581, 604,
      )

    val JUZ_ARABIC_NAMES: List<String> =
      listOf(
        "الجزء الأول",
        "الجزء الثاني",
        "الجزء الثالث",
        "الجزء الرابع",
        "الجزء الخامس",
        "الجزء السادس",
        "الجزء السابع",
        "الجزء الثامن",
        "الجزء التاسع",
        "الجزء العاشر",
        "الجزء الحادي عشر",
        "الجزء الثاني عشر",
        "الجزء الثالث عشر",
        "الجزء الرابع عشر",
        "الجزء الخامس عشر",
        "الجزء السادس عشر",
        "الجزء السابع عشر",
        "الجزء الثامن عشر",
        "الجزء التاسع عشر",
        "الجزء العشرون",
        "الجزء الحادي والعشرون",
        "الجزء الثاني والعشرون",
        "الجزء الثالث والعشرون",
        "الجزء الرابع والعشرون",
        "الجزء الخامس والعشرون",
        "الجزء السادس والعشرون",
        "الجزء السابع والعشرون",
        "الجزء الثامن والعشرون",
        "الجزء التاسع والعشرون",
        "الجزء الثلاثون",
      )

    fun startPageForSurah(surahNumber: Int): Int {
      return SURAH_START_PAGES.getOrElse(surahNumber - 1) { 1 }
    }

    fun endPageForSurah(surahNumber: Int): Int {
      return SURAH_END_PAGES.getOrElse(surahNumber - 1) { startPageForSurah(surahNumber) }
    }

    fun juzRangeForSurah(surahNumber: Int): IntRange {
      val first = SURAH_FIRST_JUZ.getOrElse(surahNumber - 1) { 1 }
      val last = SURAH_LAST_JUZ.getOrElse(surahNumber - 1) { first }
      return first..max(first, last)
    }

    fun surahBelongsToJuz(surahNumber: Int, juzNumber: Int): Boolean {
      return juzNumber in juzRangeForSurah(surahNumber)
    }

    fun surahsInJuz(juzNumber: Int): List<com.example.model.Surah> {
      return QuranCatalog.surahs.filter { surahBelongsToJuz(it.number, juzNumber) }
    }

    fun startPageForJuz(juzNumber: Int): Int {
      return JUZ_START_PAGES.getOrElse(juzNumber - 1) { 1 }
    }

    fun endPageForJuz(juzNumber: Int): Int {
      return JUZ_END_PAGES.getOrElse(juzNumber - 1) { 604 }
    }

    fun surahOpenPageInJuz(surahNumber: Int, juzNumber: Int): Int {
      val surahStart = startPageForSurah(surahNumber)
      val juzStart = startPageForJuz(juzNumber)
      val surahEnd = endPageForSurah(surahNumber)
      return max(surahStart, juzStart).coerceAtMost(surahEnd)
    }

    fun fallbackPageMeta(pageNumber: Int): MushafPageMeta {
      val safePage = pageNumber.coerceIn(1, TOTAL_MUSHAF_PAGES)
      var juz = 1
      for (i in JUZ_START_PAGES.indices) {
        if (safePage >= JUZ_START_PAGES[i]) juz = i + 1
      }
      val surahsOnPage =
        (1..114).filter { s ->
          val start = startPageForSurah(s)
          val nextStart = if (s < 114) startPageForSurah(s + 1) else TOTAL_MUSHAF_PAGES + 1
          safePage in start until max(start + 1, nextStart) || start == safePage
        }
      val firstSurah = surahsOnPage.firstOrNull() ?: 1
      return MushafPageMeta(
        pageNumber = safePage,
        juzNumber = juz,
        firstSurahNumber = firstSurah,
        firstAyahNumber = 1,
        surahNumbers = surahsOnPage.ifEmpty { listOf(firstSurah) },
      )
    }

    fun candidatePageImageUrls(pageNumber: Int): List<String> {
      val safePage = pageNumber.coerceIn(1, TOTAL_MUSHAF_PAGES)
      val padded = safePage.toString().padStart(3, '0')
      return listOf(
        "https://files.quran.app/hafs/madani/width_1260/page$padded.png",
        "https://android.quran.com/data/width_1280/page$padded.png",
        "https://android.quran.com/data/width_1024/page$padded.png",
        "https://quran.ksu.edu.sa/ayat/safahat1/$safePage.png",
      )
    }

    fun findAyahAtNormalizedPoint(
      bounds: List<AyahLineBound>,
      normX: Float,
      normY: Float,
    ): SelectedAyahKey? {
      if (bounds.isEmpty()) return null
      val px = normX.coerceIn(0f, 1f) * MUSHAF_IMAGE_WIDTH
      val py = normY.coerceIn(0f, 1f) * MUSHAF_IMAGE_HEIGHT

      // 1. Direct hit with slight finger tolerance
      val directHits =
        bounds.filter { b ->
          px in (b.minX - 16f)..(b.maxX + 16f) && py in (b.minY - 12f)..(b.maxY + 12f)
        }
      if (directHits.isNotEmpty()) {
        val closest =
          directHits.minByOrNull { b ->
            val cx = (b.minX + b.maxX) / 2f
            val cy = (b.minY + b.maxY) / 2f
            abs(px - cx) * 0.4f + abs(py - cy)
          }
        if (closest != null) {
          return SelectedAyahKey(closest.surahNumber, closest.ayahNumber)
        }
      }

      // 2. Nearest bound within 42px margin (helpful when long-pressing near edges)
      var bestBound: AyahLineBound? = null
      var bestDist = Float.MAX_VALUE
      for (b in bounds) {
        val dx =
          when {
            px < b.minX -> b.minX - px
            px > b.maxX -> px - b.maxX
            else -> 0f
          }
        val dy =
          when {
            py < b.minY -> b.minY - py
            py > b.maxY -> py - b.maxY
            else -> 0f
          }
        val dist = dx + dy * 1.4f
        if (dist < bestDist && dist <= 46f) {
          bestDist = dist
          bestBound = b
        }
      }
      return bestBound?.let { SelectedAyahKey(it.surahNumber, it.ayahNumber) }
    }

    fun orderedAyahKeysOnPage(bounds: List<AyahLineBound>): List<SelectedAyahKey> {
      return bounds
        .map { SelectedAyahKey(it.surahNumber, it.ayahNumber) }
        .distinct()
        .sorted()
    }

    fun computeConsecutiveRange(
      anchor: SelectedAyahKey,
      target: SelectedAyahKey,
    ): List<SelectedAyahKey> {
      if (anchor.surahNumber != target.surahNumber) {
        return listOf(target)
      }
      val start = min(anchor.ayahNumber, target.ayahNumber)
      val end = max(anchor.ayahNumber, target.ayahNumber)
      return (start..end).map { SelectedAyahKey(anchor.surahNumber, it) }
    }

    fun mergePageEditions(
      pageNumber: Int,
      uthmaniAyahs: List<AlQuranPageAyahDto>,
      tafsirAyahs: List<AlQuranPageAyahDto>,
    ): List<MushafPageAyahEntry> {
      if (uthmaniAyahs.isEmpty()) return emptyList()
      val tafsirByGlobal = tafsirAyahs.associateBy { it.number }
      val tafsirByPair =
        tafsirAyahs.associateBy { (it.surah?.number ?: 0) to it.numberInSurah }
      return uthmaniAyahs.mapIndexed { index, dto ->
        val surahNum = dto.surah?.number ?: 1
        val tafsirDto =
          tafsirByGlobal[dto.number]
            ?: tafsirByPair[surahNum to dto.numberInSurah]
            ?: tafsirAyahs.getOrNull(index)
        val cleanText =
          QuranMushafRepository.stripLeadingBasmalahIfNeeded(
            surahNumber = surahNum,
            numberInSurah = dto.numberInSurah,
            rawText = dto.text,
          )
        MushafPageAyahEntry(
          surahNumber = surahNum,
          ayah =
            MushafAyah(
              numberInSurah = dto.numberInSurah,
              globalNumber = dto.number,
              textAr = cleanText,
              tafsirMuyassarAr =
                tafsirDto?.text?.trim().orEmpty().ifBlank { "التفسير الميسر غير متاح لهذه الآية." },
              juz = dto.juz,
              page = dto.page.takeIf { it > 0 } ?: pageNumber,
            ),
        )
      }
    }
  }
}
