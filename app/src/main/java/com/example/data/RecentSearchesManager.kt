package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.recentSearchesDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "reciters_recent_searches")

class RecentSearchesManager(private val context: Context) {

  private object Keys {
    val RECENT_SEARCH_QUERIES = stringPreferencesKey("recent_search_queries")
  }

  val recentSearchesFlow: Flow<List<String>> =
    context.recentSearchesDataStore.data.map { prefs -> decodeQueries(prefs[Keys.RECENT_SEARCH_QUERIES]) }

  suspend fun getRecentSearches(): List<String> = recentSearchesFlow.first()

  suspend fun saveSearchQuery(query: String) {
    val sanitized = query.replace("\n", " ").trim()
    if (sanitized.length < 2) return
    val normalizedNew = QuranCatalog.normalizeArabic(sanitized)
    context.recentSearchesDataStore.edit { prefs ->
      val existing = decodeQueries(prefs[Keys.RECENT_SEARCH_QUERIES])
      val updated =
        (listOf(sanitized) +
          existing.filterNot { QuranCatalog.normalizeArabic(it) == normalizedNew })
          .take(MAX_RECENT_SEARCHES)
      prefs[Keys.RECENT_SEARCH_QUERIES] = updated.joinToString(DELIMITER)
    }
  }

  suspend fun removeSearchQuery(query: String) {
    context.recentSearchesDataStore.edit { prefs ->
      val existing = decodeQueries(prefs[Keys.RECENT_SEARCH_QUERIES])
      val updated = existing.filterNot { it == query }
      if (updated.isEmpty()) {
        prefs.remove(Keys.RECENT_SEARCH_QUERIES)
      } else {
        prefs[Keys.RECENT_SEARCH_QUERIES] = updated.joinToString(DELIMITER)
      }
    }
  }

  suspend fun clearAll() {
    context.recentSearchesDataStore.edit { prefs ->
      prefs.remove(Keys.RECENT_SEARCH_QUERIES)
    }
  }

  private fun decodeQueries(raw: String?): List<String> =
    raw
      ?.split(DELIMITER)
      ?.map { it.trim() }
      ?.filter { it.isNotEmpty() }
      ?: emptyList()

  companion object {
    const val MAX_RECENT_SEARCHES = 10
    private const val DELIMITER = "\n"
  }
}
