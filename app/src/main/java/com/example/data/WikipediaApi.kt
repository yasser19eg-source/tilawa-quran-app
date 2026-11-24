package com.example.data

import android.content.Context
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class WikipediaSummaryResponse(
  @Json(name = "title") val title: String? = null,
  @Json(name = "thumbnail") val thumbnail: WikipediaThumbnail? = null,
  @Json(name = "originalimage") val originalImage: WikipediaThumbnail? = null,
)

@JsonClass(generateAdapter = true)
data class WikipediaThumbnail(
  @Json(name = "source") val source: String? = null,
  @Json(name = "width") val width: Int? = null,
  @Json(name = "height") val height: Int? = null,
)

@JsonClass(generateAdapter = true)
data class MediaWikiSearchResponse(
  @Json(name = "query") val query: MediaWikiQueryResult? = null,
)

@JsonClass(generateAdapter = true)
data class MediaWikiQueryResult(
  @Json(name = "pages") val pages: Map<String, MediaWikiPage>? = null,
)

@JsonClass(generateAdapter = true)
data class MediaWikiPage(
  @Json(name = "title") val title: String? = null,
  @Json(name = "thumbnail") val thumbnail: WikipediaThumbnail? = null,
)

@JsonClass(generateAdapter = true)
data class PortraitDiskCache(
  @Json(name = "portraits") val portraits: Map<String, String> = emptyMap(),
)

interface WikipediaRestApi {
  @GET("api/rest_v1/page/summary/{title}")
  suspend fun getPageSummary(@Path("title") title: String): WikipediaSummaryResponse

  @GET("w/api.php?action=query&generator=search&gsrlimit=5&prop=pageimages&pithumbsize=500&format=json")
  suspend fun searchPageImages(@Query("gsrsearch") searchQuery: String): MediaWikiSearchResponse
}

class WikipediaPortraitRepository(
  private val context: Context? = null,
) {

  private val cacheMutex = Mutex()
  private val networkSemaphore = Semaphore(4)
  private val cachedThumbnails = mutableMapOf<String, String?>()
  private var diskCacheLoaded = false

  private val cacheFile: File? by lazy {
    context?.filesDir?.let { File(it, PORTRAIT_CACHE_FILE) }
  }

  private val sharedClient: OkHttpClient by lazy {
    val userAgentInterceptor = Interceptor { chain ->
      val request =
        chain
          .request()
          .newBuilder()
          .header(
            "User-Agent",
            "MushafAlTilawahAndroid/1.0 (https://ar.wikipedia.org; Quran Audio App)",
          )
          .header("Accept", "application/json")
          .build()
      chain.proceed(request)
    }

    OkHttpClient.Builder()
      .addInterceptor(userAgentInterceptor)
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(12, TimeUnit.SECONDS)
      .build()
  }

  private val moshi: Moshi by lazy {
    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  }

  private val diskCacheAdapter by lazy {
    moshi.adapter(PortraitDiskCache::class.java)
  }

  private val arabicApi: WikipediaRestApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://ar.wikipedia.org/")
      .client(sharedClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(WikipediaRestApi::class.java)
  }

  private val englishApi: WikipediaRestApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://en.wikipedia.org/")
      .client(sharedClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(WikipediaRestApi::class.java)
  }

  suspend fun loadCachedPortraits(): Map<String, String> {
    ensureDiskCacheLoaded()
    return cacheMutex.withLock {
      cachedThumbnails.mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
    }
  }

  private suspend fun ensureDiskCacheLoaded() {
    val alreadyLoaded = cacheMutex.withLock { diskCacheLoaded }
    if (alreadyLoaded) return

    val loadedMap =
      withContext(Dispatchers.IO) {
        try {
          val file = cacheFile ?: return@withContext emptyMap()
          if (!file.exists()) return@withContext emptyMap()
          val parsed = diskCacheAdapter.fromJson(file.readText())
          parsed?.portraits.orEmpty()
        } catch (_: Exception) {
          emptyMap()
        }
      }

    cacheMutex.withLock {
      if (!diskCacheLoaded) {
        loadedMap.forEach { (id, url) ->
          if (isLikelyPortraitImage(url)) {
            cachedThumbnails.putIfAbsent(id, url)
          }
        }
        diskCacheLoaded = true
      }
    }
  }

  private suspend fun persistDiskCacheLocked() {
    val file = cacheFile ?: return
    val snapshot =
      cachedThumbnails.mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
    withContext(Dispatchers.IO) {
      try {
        file.writeText(diskCacheAdapter.toJson(PortraitDiskCache(snapshot)))
      } catch (_: Exception) {
      }
    }
  }

  suspend fun resolvePortraitUrl(
    reciterId: String,
    arabicTitles: List<String>,
    englishTitles: List<String> = emptyList(),
  ): String? {
    ensureDiskCacheLoaded()

    cacheMutex.withLock {
      if (cachedThumbnails.containsKey(reciterId)) {
        return cachedThumbnails[reciterId]
      }
    }

    val expandedArabicTitles = expandArabicCandidates(arabicTitles)
    val resolvedUrl =
      withContext(Dispatchers.IO) {
        networkSemaphore.withPermit {
          // 1. Arabic Wikipedia REST summary API
          for (title in expandedArabicTitles) {
            try {
              val response = arabicApi.getPageSummary(title.replace(' ', '_'))
              val thumb = response.thumbnail?.source ?: response.originalImage?.source
              if (isLikelyPortraitImage(thumb)) {
                return@withPermit thumb
              }
            } catch (_: Exception) {
            }
          }

          // 2. English Wikipedia REST summary API fallback
          for (title in englishTitles) {
            try {
              val response = englishApi.getPageSummary(title.replace(' ', '_'))
              val thumb = response.thumbnail?.source ?: response.originalImage?.source
              if (isLikelyPortraitImage(thumb)) {
                return@withPermit thumb
              }
            } catch (_: Exception) {
            }
          }

          // 3. Arabic Wikipedia search + pageimages fallback (verified by title match)
          val primaryQuery = expandedArabicTitles.firstOrNull()
          if (!primaryQuery.isNullOrBlank()) {
            val queries = listOf("$primaryQuery قارئ", primaryQuery)
            for (query in queries) {
              try {
                val searchResp = arabicApi.searchPageImages(query)
                val matchedThumb = findMatchingSearchThumbnail(primaryQuery, searchResp)
                if (!matchedThumb.isNullOrBlank()) {
                  return@withPermit matchedThumb
                }
              } catch (_: Exception) {
              }
            }
          }
          null
        }
      }

    cacheMutex.withLock {
      cachedThumbnails[reciterId] = resolvedUrl
      if (!resolvedUrl.isNullOrBlank()) {
        persistDiskCacheLocked()
      }
    }
    return resolvedUrl
  }

  private fun expandArabicCandidates(rawTitles: List<String>): List<String> {
    val expanded = mutableListOf<String>()
    for (raw in rawTitles) {
      val clean = raw.trim().replace(Regex("\\s+"), " ")
      if (clean.isBlank()) continue
      val stripped =
        clean
          .removePrefix("الشيخ ")
          .removePrefix("القارئ ")
          .removePrefix("الدكتور ")
          .removePrefix("د. ")
          .trim()
      if (stripped.isNotBlank()) expanded.add(stripped)
      expanded.add(clean)
    }
    return expanded.distinct()
  }

  private fun findMatchingSearchThumbnail(
    reciterName: String,
    response: MediaWikiSearchResponse,
  ): String? {
    val pages = response.query?.pages?.values ?: return null
    val nameTokens =
      QuranCatalog.normalizeArabic(reciterName)
        .split(" ")
        .map { it.trim() }
        .filter { it.length >= 2 && it !in COMMON_ARABIC_FILLERS }

    if (nameTokens.isEmpty()) return null
    val requiredMatches = if (nameTokens.size >= 2) 2 else 1

    for (page in pages) {
      val source = page.thumbnail?.source ?: continue
      if (!isLikelyPortraitImage(source)) continue
      val normalizedPageTitle = QuranCatalog.normalizeArabic(page.title.orEmpty())
      val matchedCount = nameTokens.count { token -> normalizedPageTitle.contains(token) }
      if (matchedCount >= requiredMatches) {
        return source
      }
    }
    return null
  }

  companion object {
    private const val PORTRAIT_CACHE_FILE = "reciter_portraits_cache.json"
    private val COMMON_ARABIC_FILLERS = setOf("بن", "ابو", "أبو", "الشيخ", "القارئ", "الدكتور")

    fun isLikelyPortraitImage(url: String?): Boolean {
      if (url.isNullOrBlank()) return false
      val lower = url.lowercase()
      if (lower.endsWith(".svg") || lower.contains(".svg/")) return false
      val blockedKeywords =
        listOf(
          "flag_of_",
          "commons-logo",
          "disambig",
          "ambox",
          "question_book",
          "edit-clear",
          "wikisource",
          "crystal_",
          "nuvola_",
          "symbol_",
          "coat_of_arms",
          "emblem_of_",
        )
      return blockedKeywords.none { lower.contains(it) }
    }
  }
}
