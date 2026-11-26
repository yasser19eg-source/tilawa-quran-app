package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.DailyWirdGoal
import com.example.model.FontScaleOption
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "mushaf_altilawah_prefs")

data class LastPlayedRecord(
  val reciterId: String,
  val riwayahName: String,
  val surahNumber: Int,
  val positionMs: Long,
)

data class UserPreferencesState(
  val isDarkMode: Boolean = false,
  val fontScaleOption: FontScaleOption = FontScaleOption.MEDIUM,
  val autoPlayNext: Boolean = true,
  val favoriteReciterIds: Set<String> = emptySet(),
  val lastPlayed: LastPlayedRecord? = null,
  val duaCounter: Int = 0,
  val wirdReminderEnabled: Boolean = false,
  val wirdReminderHour: Int = 8,
  val wirdReminderMinute: Int = 0,
  val wirdGoal: DailyWirdGoal = DailyWirdGoal.PAGE,
  val lastCompletedWirdEpochDay: Long? = null,
  val wirdStreakDays: Int = 0,
  val lastReadSurahNumber: Int = 1,
  val lastReadAyahNumber: Int = 1,
  val lastReadMushafPage: Int = 1,
  val hasMushafBookmark: Boolean = false,
  val mushafReaderFontScale: Float = 1.0f,
) {
  fun isTodayWirdCompleted(
    todayEpochDay: Long = UserPreferencesRepository.localEpochDay()
  ): Boolean = lastCompletedWirdEpochDay == todayEpochDay

  fun effectiveWirdStreakDays(
    todayEpochDay: Long = UserPreferencesRepository.localEpochDay()
  ): Int =
    when (lastCompletedWirdEpochDay) {
      todayEpochDay,
      todayEpochDay - 1L -> wirdStreakDays.coerceAtLeast(0)
      else -> 0
    }
}

class UserPreferencesRepository(private val context: Context) {

  private object Keys {
    val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    val FONT_SCALE = stringPreferencesKey("font_scale_option")
    val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
    val FAVORITE_RECITERS = stringSetPreferencesKey("favorite_reciter_ids")
    val LAST_RECITER_ID = stringPreferencesKey("last_reciter_id")
    val LAST_RIWAYAH_NAME = stringPreferencesKey("last_riwayah_name")
    val LAST_SURAH_NUMBER = intPreferencesKey("last_surah_number")
    val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
    val DUA_COUNTER = intPreferencesKey("dua_counter")
    val WIRD_REMINDER_ENABLED = booleanPreferencesKey("wird_reminder_enabled")
    val WIRD_REMINDER_HOUR = intPreferencesKey("wird_reminder_hour")
    val WIRD_REMINDER_MINUTE = intPreferencesKey("wird_reminder_minute")
    val WIRD_GOAL = stringPreferencesKey("wird_goal")
    val LAST_COMPLETED_WIRD_EPOCH_DAY = longPreferencesKey("last_completed_wird_epoch_day")
    val WIRD_STREAK_DAYS = intPreferencesKey("wird_streak_days")
    val LAST_READ_SURAH_NUMBER = intPreferencesKey("last_read_surah_number")
    val LAST_READ_AYAH_NUMBER = intPreferencesKey("last_read_ayah_number")
    val LAST_READ_MUSHAF_PAGE = intPreferencesKey("last_read_mushaf_page")
    val HAS_MUSHAF_BOOKMARK = booleanPreferencesKey("has_mushaf_bookmark")
    val MUSHAF_READER_FONT_SCALE = floatPreferencesKey("mushaf_reader_font_scale")
  }

  val preferencesFlow: Flow<UserPreferencesState> =
    context.userPreferencesDataStore.data.map { prefs ->
      val isDark = prefs[Keys.IS_DARK_MODE] ?: false
      val fontScaleName = prefs[Keys.FONT_SCALE]
      val fontScale =
        FontScaleOption.entries.firstOrNull { it.name == fontScaleName } ?: FontScaleOption.MEDIUM
      val autoPlay = prefs[Keys.AUTO_PLAY_NEXT] ?: true
      val favorites = prefs[Keys.FAVORITE_RECITERS] ?: emptySet()
      val lastReciterId = prefs[Keys.LAST_RECITER_ID]
      val lastRiwayahName = prefs[Keys.LAST_RIWAYAH_NAME]
      val lastSurahNum = prefs[Keys.LAST_SURAH_NUMBER]
      val lastPosMs = prefs[Keys.LAST_POSITION_MS] ?: 0L
      val lastPlayed =
        if (!lastReciterId.isNullOrBlank() && !lastRiwayahName.isNullOrBlank() && lastSurahNum != null) {
          LastPlayedRecord(
            reciterId = lastReciterId,
            riwayahName = lastRiwayahName,
            surahNumber = lastSurahNum,
            positionMs = lastPosMs,
          )
        } else {
          null
        }
      val duaCounter = prefs[Keys.DUA_COUNTER] ?: 0
      val wirdEnabled = prefs[Keys.WIRD_REMINDER_ENABLED] ?: false
      val wirdHour = (prefs[Keys.WIRD_REMINDER_HOUR] ?: 8).coerceIn(0, 23)
      val wirdMinute = (prefs[Keys.WIRD_REMINDER_MINUTE] ?: 0).coerceIn(0, 59)
      val wirdGoalName = prefs[Keys.WIRD_GOAL]
      val wirdGoal =
        DailyWirdGoal.entries.firstOrNull { it.name == wirdGoalName } ?: DailyWirdGoal.PAGE
      val lastWirdDay = prefs[Keys.LAST_COMPLETED_WIRD_EPOCH_DAY]
      val wirdStreak = (prefs[Keys.WIRD_STREAK_DAYS] ?: 0).coerceAtLeast(0)
      val lastReadSurah = (prefs[Keys.LAST_READ_SURAH_NUMBER] ?: 1).coerceIn(1, 114)
      val lastReadAyah = (prefs[Keys.LAST_READ_AYAH_NUMBER] ?: 1).coerceAtLeast(1)
      val lastReadPage = (prefs[Keys.LAST_READ_MUSHAF_PAGE] ?: 1).coerceIn(1, 604)
      val hasBookmark = prefs[Keys.HAS_MUSHAF_BOOKMARK] ?: false
      val readerFontScale = (prefs[Keys.MUSHAF_READER_FONT_SCALE] ?: 1.0f).coerceIn(0.8f, 1.8f)

      UserPreferencesState(
        isDarkMode = isDark,
        fontScaleOption = fontScale,
        autoPlayNext = autoPlay,
        favoriteReciterIds = favorites,
        lastPlayed = lastPlayed,
        duaCounter = duaCounter,
        wirdReminderEnabled = wirdEnabled,
        wirdReminderHour = wirdHour,
        wirdReminderMinute = wirdMinute,
        wirdGoal = wirdGoal,
        lastCompletedWirdEpochDay = lastWirdDay,
        wirdStreakDays = wirdStreak,
        lastReadSurahNumber = lastReadSurah,
        lastReadAyahNumber = lastReadAyah,
        lastReadMushafPage = lastReadPage,
        hasMushafBookmark = hasBookmark,
        mushafReaderFontScale = readerFontScale,
      )
    }

  suspend fun setDarkMode(enabled: Boolean) {
    context.userPreferencesDataStore.edit { it[Keys.IS_DARK_MODE] = enabled }
  }

  suspend fun setFontScale(option: FontScaleOption) {
    context.userPreferencesDataStore.edit { it[Keys.FONT_SCALE] = option.name }
  }

  suspend fun setAutoPlayNext(enabled: Boolean) {
    context.userPreferencesDataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
  }

  suspend fun toggleFavoriteReciter(reciterId: String) {
    context.userPreferencesDataStore.edit { prefs ->
      val current = prefs[Keys.FAVORITE_RECITERS] ?: emptySet()
      prefs[Keys.FAVORITE_RECITERS] =
        if (current.contains(reciterId)) current - reciterId else current + reciterId
    }
  }

  suspend fun saveLastPlayed(
    reciterId: String,
    riwayahName: String,
    surahNumber: Int,
    positionMs: Long,
  ) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.LAST_RECITER_ID] = reciterId
      prefs[Keys.LAST_RIWAYAH_NAME] = riwayahName
      prefs[Keys.LAST_SURAH_NUMBER] = surahNumber
      prefs[Keys.LAST_POSITION_MS] = positionMs
    }
  }

  suspend fun incrementDuaCounter() {
    context.userPreferencesDataStore.edit { prefs ->
      val current = prefs[Keys.DUA_COUNTER] ?: 0
      prefs[Keys.DUA_COUNTER] = current + 1
    }
  }

  suspend fun setWirdReminderEnabled(enabled: Boolean) {
    context.userPreferencesDataStore.edit { it[Keys.WIRD_REMINDER_ENABLED] = enabled }
  }

  suspend fun setWirdReminderTime(hour: Int, minute: Int) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.WIRD_REMINDER_HOUR] = hour.coerceIn(0, 23)
      prefs[Keys.WIRD_REMINDER_MINUTE] = minute.coerceIn(0, 59)
    }
  }

  suspend fun setWirdGoal(goal: DailyWirdGoal) {
    context.userPreferencesDataStore.edit { it[Keys.WIRD_GOAL] = goal.name }
  }

  suspend fun toggleTodayWirdCompleted(todayEpochDay: Long = localEpochDay()) {
    context.userPreferencesDataStore.edit { prefs ->
      val previousDay = prefs[Keys.LAST_COMPLETED_WIRD_EPOCH_DAY]
      val currentStreak = (prefs[Keys.WIRD_STREAK_DAYS] ?: 0).coerceAtLeast(0)
      val (nextDay, nextStreak) =
        computeWirdToggleResult(
          lastCompletedEpochDay = previousDay,
          currentStreak = currentStreak,
          todayEpochDay = todayEpochDay,
        )
      if (nextDay == null) {
        prefs.remove(Keys.LAST_COMPLETED_WIRD_EPOCH_DAY)
      } else {
        prefs[Keys.LAST_COMPLETED_WIRD_EPOCH_DAY] = nextDay
      }
      prefs[Keys.WIRD_STREAK_DAYS] = nextStreak
    }
  }

  suspend fun saveMushafBookmark(surahNumber: Int, ayahNumber: Int) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.LAST_READ_SURAH_NUMBER] = surahNumber.coerceIn(1, 114)
      prefs[Keys.LAST_READ_AYAH_NUMBER] = ayahNumber.coerceAtLeast(1)
      prefs[Keys.HAS_MUSHAF_BOOKMARK] = true
    }
  }

  suspend fun saveMushafPageBookmark(pageNumber: Int, surahNumber: Int, ayahNumber: Int) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.LAST_READ_MUSHAF_PAGE] = pageNumber.coerceIn(1, 604)
      prefs[Keys.LAST_READ_SURAH_NUMBER] = surahNumber.coerceIn(1, 114)
      prefs[Keys.LAST_READ_AYAH_NUMBER] = ayahNumber.coerceAtLeast(1)
      prefs[Keys.HAS_MUSHAF_BOOKMARK] = true
    }
  }

  suspend fun setMushafReaderFontScale(scale: Float) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.MUSHAF_READER_FONT_SCALE] = scale.coerceIn(0.8f, 1.8f)
    }
  }

  companion object {
    fun localEpochDay(nowMillis: Long = System.currentTimeMillis()): Long {
      val cal =
        Calendar.getInstance().apply {
          timeInMillis = nowMillis
          set(Calendar.HOUR_OF_DAY, 12)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
      val offsetMs = cal.timeZone.getOffset(cal.timeInMillis)
      return TimeUnit.MILLISECONDS.toDays(cal.timeInMillis + offsetMs)
    }

    fun computeWirdToggleResult(
      lastCompletedEpochDay: Long?,
      currentStreak: Int,
      todayEpochDay: Long,
    ): Pair<Long?, Int> {
      return if (lastCompletedEpochDay == todayEpochDay) {
        val updatedStreak = (currentStreak - 1).coerceAtLeast(0)
        val restoredDay = if (updatedStreak > 0) todayEpochDay - 1L else null
        restoredDay to updatedStreak
      } else {
        val updatedStreak =
          if (lastCompletedEpochDay == todayEpochDay - 1L) {
            currentStreak.coerceAtLeast(0) + 1
          } else {
            1
          }
        todayEpochDay to updatedStreak
      }
    }
  }
}
