package com.example.data

import android.content.Context
import com.example.model.MushafAyah
import com.example.model.MushafSurahContent
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.util.concurrent.TimeUnit
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
data class AlQuranMultiEditionResponse(
  @Json(name = "code") val code: Int = 0,
  @Json(name = "status") val status: String? = null,
  @Json(name = "data") val data: List<AlQuranEditionDataDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class AlQuranSingleEditionResponse(
  @Json(name = "code") val code: Int = 0,
  @Json(name = "status") val status: String? = null,
  @Json(name = "data") val data: AlQuranEditionDataDto? = null,
)

@JsonClass(generateAdapter = true)
data class AlQuranEditionDataDto(
  @Json(name = "number") val number: Int = 0,
  @Json(name = "name") val name: String = "",
  @Json(name = "numberOfAyahs") val numberOfAyahs: Int = 0,
  @Json(name = "ayahs") val ayahs: List<AlQuranAyahDto> = emptyList(),
  @Json(name = "edition") val edition: AlQuranEditionMetaDto? = null,
)

@JsonClass(generateAdapter = true)
data class AlQuranEditionMetaDto(
  @Json(name = "identifier") val identifier: String = "",
  @Json(name = "name") val name: String = "",
  @Json(name = "type") val type: String = "",
)

@JsonClass(generateAdapter = true)
data class AlQuranAyahDto(
  @Json(name = "number") val number: Int = 0,
  @Json(name = "text") val text: String = "",
  @Json(name = "numberInSurah") val numberInSurah: Int = 0,
  @Json(name = "juz") val juz: Int = 1,
  @Json(name = "page") val page: Int = 1,
)

@JsonClass(generateAdapter = true)
data class CachedMushafSurah(
  @Json(name = "surahNumber") val surahNumber: Int,
  @Json(name = "ayahs") val ayahs: List<CachedMushafAyah> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CachedMushafAyah(
  @Json(name = "numberInSurah") val numberInSurah: Int,
  @Json(name = "globalNumber") val globalNumber: Int,
  @Json(name = "textAr") val textAr: String,
  @Json(name = "tafsirMuyassarAr") val tafsirMuyassarAr: String,
  @Json(name = "juz") val juz: Int,
  @Json(name = "page") val page: Int,
)

interface AlQuranCloudApi {
  @GET("surah/{surahNumber}/editions/quran-uthmani,ar.muyassar")
  suspend fun getSurahWithTafsir(
    @Path("surahNumber") surahNumber: Int
  ): AlQuranMultiEditionResponse

  @GET("surah/{surahNumber}/{edition}")
  suspend fun getSurahEdition(
    @Path("surahNumber") surahNumber: Int,
    @Path("edition") edition: String,
  ): AlQuranSingleEditionResponse
}

data class MushafLoadResult(
  val content: MushafSurahContent,
  val isFromCache: Boolean,
)

class QuranMushafRepository(
  private val context: Context? = null,
  private val apiOverride: AlQuranCloudApi? = null,
) {

  private val mutex = Mutex()
  private val memoryCache = mutableMapOf<Int, MushafSurahContent>()

  private val cacheDir: File? by lazy {
    context?.filesDir?.let { File(it, MUSHAF_CACHE_DIR).apply { mkdirs() } }
  }

  private val moshi: Moshi by lazy {
    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  }

  private val cacheAdapter by lazy { moshi.adapter(CachedMushafSurah::class.java) }

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .build()
  }

  private val primaryApi: AlQuranCloudApi by lazy {
    apiOverride ?: createRetrofitApi("https://api.alquran.cloud/v1/")
  }

  private val fallbackHttpApi: AlQuranCloudApi by lazy {
    createRetrofitApi("http://api.alquran.cloud/v1/")
  }

  private fun createRetrofitApi(baseUrl: String): AlQuranCloudApi {
    return Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(AlQuranCloudApi::class.java)
  }

  fun cachedSurahNumbers(): Set<Int> {
    val dir = cacheDir ?: return memoryCache.keys.toSet()
    val files = dir.listFiles().orEmpty()
    val diskIds =
      files.mapNotNull { file ->
        val name = file.name
        if (name.startsWith("surah_") && name.endsWith(".json")) {
          name.removePrefix("surah_").removeSuffix(".json").toIntOrNull()
        } else {
          null
        }
      }
    return (memoryCache.keys + diskIds).toSet()
  }

  suspend fun loadSurah(
    surahNumber: Int,
    forceRefresh: Boolean = false,
  ): Result<MushafLoadResult> {
    val surah =
      QuranCatalog.findSurah(surahNumber)
        ?: return Result.failure(IllegalArgumentException("رقم السورة غير صحيح: $surahNumber"))

    if (!forceRefresh) {
      mutex.withLock {
        memoryCache[surahNumber]?.let { cached ->
          return Result.success(MushafLoadResult(cached, isFromCache = true))
        }
      }

      val diskContent = readFromDiskCache(surahNumber, surah)
      if (diskContent != null) {
        mutex.withLock { memoryCache[surahNumber] = diskContent }
        return Result.success(MushafLoadResult(diskContent, isFromCache = true))
      }
    }

    return withContext(Dispatchers.IO) {
      val apis =
        if (apiOverride != null) listOf(primaryApi) else listOf(primaryApi, fallbackHttpApi)

      var lastError: Throwable? = null
      for (api in apis) {
        try {
          val ayahs = fetchAndMergeFromApi(api, surahNumber)
          if (ayahs.isNotEmpty()) {
            val content = MushafSurahContent(surah = surah, ayahs = ayahs)
            mutex.withLock { memoryCache[surahNumber] = content }
            writeToDiskCache(surahNumber, ayahs)
            return@withContext Result.success(MushafLoadResult(content, isFromCache = false))
          }
        } catch (t: Throwable) {
          lastError = t
        }
      }

      Result.failure(
        lastError ?: IllegalStateException("تعذر تحميل السورة والتفسير في الوقت الحالي")
      )
    }
  }

  private suspend fun fetchAndMergeFromApi(
    api: AlQuranCloudApi,
    surahNumber: Int,
  ): List<MushafAyah> {
    // Try combined multi-edition endpoint first
    try {
      val multi = api.getSurahWithTafsir(surahNumber)
      val uthmaniData =
        multi.data.firstOrNull { it.edition?.identifier == UTHMANI_EDITION }
          ?: multi.data.getOrNull(0)
      val tafsirData =
        multi.data.firstOrNull { it.edition?.identifier == MUYASSAR_EDITION }
          ?: multi.data.getOrNull(1)
      if (uthmaniData != null && tafsirData != null && uthmaniData.ayahs.isNotEmpty()) {
        return mergeEditions(surahNumber, uthmaniData.ayahs, tafsirData.ayahs)
      }
    } catch (_: Throwable) {
      // Fallback to separate edition requests on the same API host
    }

    return coroutineScope {
      val uthmaniDeferred = async { api.getSurahEdition(surahNumber, UTHMANI_EDITION) }
      val tafsirDeferred = async { api.getSurahEdition(surahNumber, MUYASSAR_EDITION) }
      val uthmaniResp = uthmaniDeferred.await()
      val tafsirResp = tafsirDeferred.await()
      val uthmaniAyahs = uthmaniResp.data?.ayahs.orEmpty()
      val tafsirAyahs = tafsirResp.data?.ayahs.orEmpty()
      mergeEditions(surahNumber, uthmaniAyahs, tafsirAyahs)
    }
  }

  private suspend fun readFromDiskCache(
    surahNumber: Int,
    surah: com.example.model.Surah,
  ): MushafSurahContent? {
    val dir = cacheDir ?: return null
    val file = File(dir, "surah_$surahNumber.json")
    if (!file.exists()) return null

    return withContext(Dispatchers.IO) {
      try {
        val parsed = cacheAdapter.fromJson(file.readText()) ?: return@withContext null
        if (parsed.surahNumber != surahNumber || parsed.ayahs.isEmpty()) return@withContext null
        val mapped =
          parsed.ayahs.map { dto ->
            MushafAyah(
              numberInSurah = dto.numberInSurah,
              globalNumber = dto.globalNumber,
              textAr = dto.textAr.trimStart('\uFEFF').trim(),
              tafsirMuyassarAr = dto.tafsirMuyassarAr,
              juz = dto.juz,
              page = dto.page,
            )
          }
        MushafSurahContent(surah = surah, ayahs = mapped)
      } catch (_: Exception) {
        null
      }
    }
  }

  private suspend fun writeToDiskCache(surahNumber: Int, ayahs: List<MushafAyah>) {
    val dir = cacheDir ?: return
    val file = File(dir, "surah_$surahNumber.json")
    withContext(Dispatchers.IO) {
      try {
        val payload =
          CachedMushafSurah(
            surahNumber = surahNumber,
            ayahs =
              ayahs.map { ayah ->
                CachedMushafAyah(
                  numberInSurah = ayah.numberInSurah,
                  globalNumber = ayah.globalNumber,
                  textAr = ayah.textAr,
                  tafsirMuyassarAr = ayah.tafsirMuyassarAr,
                  juz = ayah.juz,
                  page = ayah.page,
                )
              },
          )
        file.writeText(cacheAdapter.toJson(payload))
      } catch (_: Exception) {
      }
    }
  }

  companion object {
    const val UTHMANI_EDITION = "quran-uthmani"
    const val MUYASSAR_EDITION = "ar.muyassar"
    private const val MUSHAF_CACHE_DIR = "mushaf_cache"
    private val BASMALAH_NORMALIZED_TOKENS = listOf("بسم", "الله", "الرحمن", "الرحيم")
    private val EXTRA_UTHMANI_MARKS = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")

    fun mergeEditions(
      surahNumber: Int,
      uthmaniAyahs: List<AlQuranAyahDto>,
      tafsirAyahs: List<AlQuranAyahDto>,
    ): List<MushafAyah> {
      if (uthmaniAyahs.isEmpty()) return emptyList()
      val tafsirByAyahNumber = tafsirAyahs.associateBy { it.numberInSurah }
      return uthmaniAyahs.mapIndexed { index, uthmani ->
        val tafsirDto = tafsirByAyahNumber[uthmani.numberInSurah] ?: tafsirAyahs.getOrNull(index)
        val cleanText =
          stripLeadingBasmalahIfNeeded(
            surahNumber = surahNumber,
            numberInSurah = uthmani.numberInSurah,
            rawText = uthmani.text,
          )
        MushafAyah(
          numberInSurah = uthmani.numberInSurah,
          globalNumber = uthmani.number,
          textAr = cleanText,
          tafsirMuyassarAr = tafsirDto?.text?.trim().orEmpty().ifBlank { "التفسير الميسر غير متاح لهذه الآية." },
          juz = uthmani.juz,
          page = uthmani.page,
        )
      }
    }

    fun stripLeadingBasmalahIfNeeded(
      surahNumber: Int,
      numberInSurah: Int,
      rawText: String,
    ): String {
      val trimmed = rawText.trimStart('\uFEFF').trim()
      if (surahNumber == 1 || surahNumber == 9 || numberInSurah != 1) {
        return trimmed
      }
      val tokens = trimmed.split(Regex("\\s+"))
      if (tokens.size <= BASMALAH_NORMALIZED_TOKENS.size) {
        return trimmed
      }
      val normalizedPrefix = tokens.take(4).map { normalizeForBasmalahCheck(it) }
      return if (normalizedPrefix == BASMALAH_NORMALIZED_TOKENS) {
        tokens.drop(4).joinToString(" ").trim()
      } else {
        trimmed
      }
    }

    private fun normalizeForBasmalahCheck(token: String): String {
      return token
        .replace(EXTRA_UTHMANI_MARKS, "")
        .replace('ٱ', 'ا')
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .trim()
    }
  }
}
