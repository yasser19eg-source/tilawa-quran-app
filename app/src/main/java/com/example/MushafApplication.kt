package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.RecentSearchesManager
import com.example.data.ReciterCatalogRepository
import com.example.data.UserPreferencesRepository
import com.example.data.WikipediaPortraitRepository
import com.example.notifications.DailyWirdReminderManager
import com.example.notifications.DailyWirdReminderScheduler
import com.example.playback.AudioPlaybackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MushafApplication : Application(), ImageLoaderFactory {

  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  val preferencesRepository: UserPreferencesRepository by lazy {
    UserPreferencesRepository(applicationContext)
  }

  val recentSearchesManager: RecentSearchesManager by lazy {
    RecentSearchesManager(applicationContext)
  }

  val reciterCatalogRepository: ReciterCatalogRepository by lazy {
    ReciterCatalogRepository(applicationContext).apply { initialize() }
  }

  val wikipediaPortraitRepository: WikipediaPortraitRepository by lazy {
    WikipediaPortraitRepository(applicationContext)
  }

  val wirdReminderManager: DailyWirdReminderManager by lazy {
    DailyWirdReminderManager(applicationContext)
  }

  val audioPlaybackManager: AudioPlaybackManager by lazy {
    AudioPlaybackManager(applicationContext, preferencesRepository, reciterCatalogRepository)
  }

  override fun onCreate() {
    super.onCreate()
    DailyWirdReminderScheduler.ensureNotificationChannel(this)
    appScope.launch {
      val prefs = preferencesRepository.preferencesFlow.first()
      if (prefs.wirdReminderEnabled) {
        DailyWirdReminderScheduler.scheduleDailyReminder(
          context = applicationContext,
          hour = prefs.wirdReminderHour,
          minute = prefs.wirdReminderMinute,
        )
      }
    }
  }

  override fun newImageLoader(): ImageLoader {
    val userAgentInterceptor = Interceptor { chain ->
      val request =
        chain
          .request()
          .newBuilder()
          .header(
            "User-Agent",
            "MushafAlTilawahAndroid/1.0 (https://ar.wikipedia.org; Quran Audio App)",
          )
          .build()
      chain.proceed(request)
    }

    val okHttpClient =
      OkHttpClient.Builder()
        .addInterceptor(userAgentInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    return ImageLoader.Builder(this)
      .okHttpClient(okHttpClient)
      .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
      .diskCache {
        DiskCache.Builder().directory(cacheDir.resolve("image_cache")).maxSizeBytes(40L * 1024 * 1024).build()
      }
      .crossfade(true)
      .build()
  }
}
