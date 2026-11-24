package com.example.data

import android.content.Context
import com.example.model.Reciter
import com.example.model.Riwayah
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Mp3QuranRecitersResponse(
  @Json(name = "reciters") val reciters: List<Mp3QuranReciterDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class Mp3QuranReciterDto(
  @Json(name = "id") val id: Int = 0,
  @Json(name = "name") val name: String = "",
  @Json(name = "letter") val letter: String? = null,
  @Json(name = "moshaf") val moshaf: List<Mp3QuranMoshafDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class Mp3QuranMoshafDto(
  @Json(name = "id") val id: Int = 0,
  @Json(name = "name") val name: String = "",
  @Json(name = "server") val server: String = "",
  @Json(name = "surah_total") val surahTotal: Int = 0,
  @Json(name = "surah_list") val surahList: String = "",
)

interface Mp3QuranApi {
  @GET("api/v3/reciters")
  suspend fun getReciters(@Query("language") language: String = "ar"): Mp3QuranRecitersResponse
}

class ReciterCatalogRepository(
  private val context: Context,
  private val apiOverride: Mp3QuranApi? = null,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val refreshMutex = Mutex()

  private val _recitersFlow = MutableStateFlow(QuranCatalog.reciters)
  val recitersFlow: StateFlow<List<Reciter>> = _recitersFlow.asStateFlow()

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

  private val cacheFile: File by lazy { File(context.filesDir, CACHE_FILE_NAME) }

  private val moshi: Moshi by lazy {
    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  }

  private val responseAdapter by lazy {
    moshi.adapter(Mp3QuranRecitersResponse::class.java)
  }

  private val api: Mp3QuranApi by lazy {
    apiOverride ?: run {
      val headerInterceptor = Interceptor { chain ->
        val req =
          chain
            .request()
            .newBuilder()
            .header("Accept", "application/json")
            .header("User-Agent", "MushafAlTilawahAndroid/1.0")
            .build()
        chain.proceed(req)
      }
      val client =
        OkHttpClient.Builder()
          .addInterceptor(headerInterceptor)
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(15, TimeUnit.SECONDS)
          .build()

      Retrofit.Builder()
        .baseUrl(MP3QURAN_BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(Mp3QuranApi::class.java)
    }
  }

  fun initialize() {
    scope.launch {
      loadFromDiskCache()
      refreshCatalog()
    }
  }

  suspend fun loadFromDiskCache(): Boolean =
    withContext(Dispatchers.IO) {
      try {
        if (!cacheFile.exists()) return@withContext false
        val json = cacheFile.readText()
        val parsed = responseAdapter.fromJson(json) ?: return@withContext false
        if (parsed.reciters.isEmpty()) return@withContext false
        val merged = mergeCatalog(QuranCatalog.reciters, parsed.reciters)
        _recitersFlow.value = merged
        true
      } catch (_: Exception) {
        false
      }
    }

  suspend fun refreshCatalog(): List<Reciter> =
    refreshMutex.withLock {
      _isSyncing.value = true
      try {
        val response = withContext(Dispatchers.IO) { api.getReciters("ar") }
        if (response.reciters.isNotEmpty()) {
          val merged = mergeCatalog(QuranCatalog.reciters, response.reciters)
          _recitersFlow.value = merged
          saveToDiskCache(response)
        }
      } catch (_: Exception) {
        // Keep seeded or disk-cached reciters silently when offline
      } finally {
        _isSyncing.value = false
      }
      _recitersFlow.value
    }

  fun findReciter(reciterId: String): Reciter? {
    return _recitersFlow.value.firstOrNull { it.id == reciterId }
      ?: QuranCatalog.findReciter(reciterId)
  }

  fun findUpdatedRiwayah(reciterId: String, riwayahNameAr: String, previousBaseUrl: String): Riwayah? {
    val reciter = findReciter(reciterId) ?: return null
    val normalizedName = compactArabic(riwayahNameAr)
    return reciter.riwayat.firstOrNull {
      compactArabic(it.nameAr) == normalizedName ||
        it.baseUrl == previousBaseUrl ||
        it.fallbackBaseUrls.contains(previousBaseUrl)
    } ?: reciter.riwayat.firstOrNull()
  }

  private suspend fun saveToDiskCache(response: Mp3QuranRecitersResponse) =
    withContext(Dispatchers.IO) {
      try {
        val json = responseAdapter.toJson(response)
        cacheFile.writeText(json)
      } catch (_: Exception) {
      }
    }

  companion object {
    const val MP3QURAN_BASE_URL = "https://www.mp3quran.net/"
    private const val CACHE_FILE_NAME = "mp3quran_reciters_cache.json"

    fun compactArabic(input: String): String {
      return QuranCatalog.normalizeArabic(input).replace(" ", "")
    }

    fun normalizeServerUrl(rawUrl: String): String? {
      val trimmed = rawUrl.trim()
      if (trimmed.isBlank()) return null
      val withScheme =
        when {
          trimmed.startsWith("https://", ignoreCase = true) -> trimmed
          trimmed.startsWith("http://", ignoreCase = true) ->
            "https://" + trimmed.removePrefix("http://").removePrefix("HTTP://")
          else -> return null
        }
      return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }

    fun parseSurahList(rawList: String): Set<Int> {
      if (rawList.isBlank()) return emptySet()
      val result = mutableSetOf<Int>()
      rawList.split(',').forEach { token ->
        val num = token.trim().toIntOrNull()
        if (num != null && num in 1..114) {
          result.add(num)
        }
      }
      return result
    }

    fun buildArabicWikiTitleCandidates(rawName: String): List<String> {
      val trimmed = rawName.trim().replace(Regex("\\s+"), " ")
      if (trimmed.isBlank()) return emptyList()
      val stripped =
        trimmed
          .removePrefix("الشيخ ")
          .removePrefix("القارئ ")
          .removePrefix("الدكتور ")
          .removePrefix("د. ")
          .trim()
      return listOf(stripped, trimmed, "$stripped (قارئ)").filter { it.isNotBlank() }.distinct()
    }

    fun mergeCatalog(curatedSeeds: List<Reciter>, apiReciters: List<Mp3QuranReciterDto>): List<Reciter> {
      // Index API reciters by compact Arabic name and by server slug for reliable matching
      val apiByCompactName = mutableMapOf<String, Mp3QuranReciterDto>()
      val apiByServerSlug = mutableMapOf<String, Mp3QuranReciterDto>()

      val validApiReciters =
        apiReciters.mapNotNull { dto ->
          val cleanName = dto.name.trim()
          if (cleanName.isBlank()) return@mapNotNull null
          val validMoshafs = dto.moshaf.filter { isValidMoshaf(it) }
          if (validMoshafs.isEmpty()) return@mapNotNull null
          dto.copy(name = cleanName, moshaf = validMoshafs)
        }

      validApiReciters.forEach { dto ->
        apiByCompactName[compactArabic(dto.name)] = dto
        dto.moshaf.forEach { moshaf ->
          extractServerSlug(moshaf.server)?.let { slug -> apiByServerSlug.putIfAbsent(slug, dto) }
        }
      }

      val matchedApiIds = mutableSetOf<Int>()

      // 1. Update curated seeds with live server URLs & surah_list while preserving curated fallbacks & metadata
      val updatedCurated =
        curatedSeeds.map { seed ->
          val matchedApi = findMatchingApiReciter(seed, apiByCompactName, apiByServerSlug)
          if (matchedApi == null) {
            seed
          } else {
            matchedApiIds.add(matchedApi.id)
            mergeCuratedReciter(seed, matchedApi)
          }
        }

      // 2. Append all additional verified MP3Quran reciters
      val additionalReciters =
        validApiReciters
          .filter { !matchedApiIds.contains(it.id) }
          .map { dto ->
            val riwayat =
              dto.moshaf
                .mapNotNull { toRiwayah(it) }
                .distinctBy { it.baseUrl }
            Reciter(
              id = "mp3q_${dto.id}",
              nameAr = dto.name,
              isMostListened = false,
              wikipediaTitles = buildArabicWikiTitleCandidates(dto.name),
              enWikipediaTitles = emptyList(),
              riwayat = riwayat,
            )
          }
          .filter { it.riwayat.isNotEmpty() }

      return updatedCurated + additionalReciters
    }

    private fun isValidMoshaf(moshaf: Mp3QuranMoshafDto): Boolean {
      val server = normalizeServerUrl(moshaf.server) ?: return false
      if (!server.contains(".")) return false
      val surahs = parseSurahList(moshaf.surahList)
      return surahs.isNotEmpty()
    }

    private fun toRiwayah(moshaf: Mp3QuranMoshafDto): Riwayah? {
      val server = normalizeServerUrl(moshaf.server) ?: return null
      val surahs = parseSurahList(moshaf.surahList)
      if (surahs.isEmpty()) return null
      val name = moshaf.name.trim().ifBlank { "حفص عن عاصم" }
      return Riwayah(
        nameAr = name,
        baseUrl = server,
        fallbackBaseUrls = emptyList(),
        availableSurahs = surahs,
      )
    }

    private fun mergeCuratedReciter(seed: Reciter, apiReciter: Mp3QuranReciterDto): Reciter {
      val apiRiwayat = apiReciter.moshaf.mapNotNull { toRiwayah(it) }
      val usedApiUrls = mutableSetOf<String>()

      val updatedSeedRiwayat =
        seed.riwayat.map { curatedRiwayah ->
          val curatedSlug = extractFullPathSlug(curatedRiwayah.baseUrl)
          val matchedApiRiwayah =
            apiRiwayat.firstOrNull { apiR ->
              extractFullPathSlug(apiR.baseUrl) == curatedSlug
            } ?: apiRiwayat.firstOrNull { apiR ->
              compactArabic(apiR.nameAr).contains(compactArabic(curatedRiwayah.nameAr)) ||
                compactArabic(curatedRiwayah.nameAr).contains(compactArabic(apiR.nameAr))
            }

          if (matchedApiRiwayah != null) {
            usedApiUrls.add(matchedApiRiwayah.baseUrl)
            val fallbacks =
              (listOf(curatedRiwayah.baseUrl) + curatedRiwayah.fallbackBaseUrls)
                .mapNotNull { normalizeServerUrl(it) }
                .filter { it != matchedApiRiwayah.baseUrl }
                .distinct()
            curatedRiwayah.copy(
              baseUrl = matchedApiRiwayah.baseUrl,
              fallbackBaseUrls = fallbacks,
              availableSurahs = matchedApiRiwayah.availableSurahs,
            )
          } else {
            curatedRiwayah
          }
        }

      val extraApiRiwayat = apiRiwayat.filter { !usedApiUrls.contains(it.baseUrl) }
      val combinedRiwayat = (updatedSeedRiwayat + extraApiRiwayat).distinctBy { it.baseUrl }

      return seed.copy(riwayat = combinedRiwayat)
    }

    private fun findMatchingApiReciter(
      seed: Reciter,
      apiByCompactName: Map<String, Mp3QuranReciterDto>,
      apiByServerSlug: Map<String, Mp3QuranReciterDto>,
    ): Mp3QuranReciterDto? {
      val nameCandidates =
        (listOf(seed.nameAr) + seed.wikipediaTitles).map { compactArabic(it) }.distinct()
      for (candidate in nameCandidates) {
        apiByCompactName[candidate]?.let { return it }
      }
      for (riwayah in seed.riwayat) {
        extractServerSlug(riwayah.baseUrl)?.let { slug ->
          apiByServerSlug[slug]?.let { return it }
        }
      }
      return null
    }

    private fun extractServerSlug(serverUrl: String): String? {
      val clean = serverUrl.trim().trimEnd('/')
      val afterHost = clean.substringAfter("://", clean).substringAfter('/', "")
      val firstSegment = afterHost.substringBefore('/').lowercase()
      return firstSegment.takeIf { it.isNotBlank() }
    }

    private fun extractFullPathSlug(serverUrl: String): String {
      val clean = serverUrl.trim().trimEnd('/')
      return clean.substringAfter("://", clean).substringAfter('/', "").lowercase()
    }
  }
}
