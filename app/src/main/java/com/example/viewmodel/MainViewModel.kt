package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.QuranCatalog
import com.example.data.RecentSearchesManager
import com.example.data.ReciterCatalogRepository
import com.example.data.UserPreferencesRepository
import com.example.data.UserPreferencesState
import com.example.data.WikipediaPortraitRepository
import com.example.model.ActiveAudioTarget
import com.example.model.AppRoute
import com.example.model.AppTab
import com.example.model.DailyWirdGoal
import com.example.model.FontScaleOption
import com.example.model.PlaybackUiState
import com.example.model.Reciter
import com.example.model.ReciterFilter
import com.example.model.Riwayah
import com.example.model.SleepTimerOption
import com.example.model.Surah
import com.example.notifications.DailyWirdReminderManager
import com.example.playback.AudioPlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NavigationState(
  val backStack: List<AppRoute> = listOf(AppRoute.Tab(AppTab.HOME)),
) {
  val currentRoute: AppRoute
    get() = backStack.lastOrNull() ?: AppRoute.Tab(AppTab.HOME)

  val canGoBack: Boolean
    get() = backStack.size > 1
}

class MainViewModel(
  private val preferencesRepository: UserPreferencesRepository,
  private val recentSearchesManager: RecentSearchesManager,
  private val reciterCatalogRepository: ReciterCatalogRepository,
  private val portraitRepository: WikipediaPortraitRepository,
  private val playbackManager: AudioPlaybackManager,
  private val wirdReminderManager: DailyWirdReminderManager,
) : ViewModel() {

  val preferencesState: StateFlow<UserPreferencesState> =
    preferencesRepository.preferencesFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = UserPreferencesState(),
    )

  val recentSearches: StateFlow<List<String>> =
    recentSearchesManager.recentSearchesFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList(),
    )

  val reciters: StateFlow<List<Reciter>> = reciterCatalogRepository.recitersFlow
  val isCatalogSyncing: StateFlow<Boolean> = reciterCatalogRepository.isSyncing

  val playbackState: StateFlow<PlaybackUiState> = playbackManager.uiState

  private val _navState = MutableStateFlow(NavigationState())
  val navState: StateFlow<NavigationState> = _navState.asStateFlow()

  private val _portraits = MutableStateFlow<Map<String, String?>>(emptyMap())
  val portraits: StateFlow<Map<String, String?>> = _portraits.asStateFlow()

  private val requestedPortraitIds = mutableSetOf<String>()

  private val _reciterSearchQuery = MutableStateFlow("")
  val reciterSearchQuery: StateFlow<String> = _reciterSearchQuery.asStateFlow()

  private val _reciterFilter = MutableStateFlow(ReciterFilter.ALL)
  val reciterFilter: StateFlow<ReciterFilter> = _reciterFilter.asStateFlow()

  private val _selectedRiwayahByReciter = MutableStateFlow<Map<String, Int>>(emptyMap())
  val selectedRiwayahByReciter: StateFlow<Map<String, Int>> =
    _selectedRiwayahByReciter.asStateFlow()

  private val _surahSearchQuery = MutableStateFlow("")
  val surahSearchQuery: StateFlow<String> = _surahSearchQuery.asStateFlow()

  private val _isPlayerExpanded = MutableStateFlow(false)
  val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

  init {
    preloadFeaturedReciterPortraits()
  }

  private fun preloadFeaturedReciterPortraits() {
    viewModelScope.launch {
      val cached = portraitRepository.loadCachedPortraits()
      if (cached.isNotEmpty()) {
        _portraits.update { current -> cached + current }
      }
      QuranCatalog.reciters.forEach { reciter -> ensurePortraitLoaded(reciter) }
    }
  }

  fun ensurePortraitLoaded(reciter: Reciter) {
    if (_portraits.value.containsKey(reciter.id) || !requestedPortraitIds.add(reciter.id)) {
      return
    }
    viewModelScope.launch {
      val url =
        portraitRepository.resolvePortraitUrl(
          reciterId = reciter.id,
          arabicTitles = reciter.wikipediaTitles,
          englishTitles = reciter.enWikipediaTitles,
        )
      _portraits.update { current -> current + (reciter.id to url) }
    }
  }

  fun refreshCatalog() {
    viewModelScope.launch { reciterCatalogRepository.refreshCatalog() }
  }

  fun navigateToTab(tab: AppTab) {
    val target = AppRoute.Tab(tab)
    _navState.update { state ->
      if (state.currentRoute == target) state
      else state.copy(backStack = (state.backStack + target).takeLast(24))
    }
  }

  fun openReciterDetail(reciterId: String) {
    _surahSearchQuery.value = ""
    val target = AppRoute.ReciterDetail(reciterId)
    _navState.update { state ->
      if (state.currentRoute == target) state
      else state.copy(backStack = (state.backStack + target).takeLast(24))
    }
  }

  fun openFavorites() {
    val target = AppRoute.Favorites
    _navState.update { state ->
      if (state.currentRoute == target) state
      else state.copy(backStack = (state.backStack + target).takeLast(24))
    }
  }

  fun openSettings() {
    val target = AppRoute.Settings
    _navState.update { state ->
      if (state.currentRoute == target) state
      else state.copy(backStack = (state.backStack + target).takeLast(24))
    }
  }

  fun navigateBack(): Boolean {
    val stack = _navState.value.backStack
    if (stack.size <= 1) return false
    _navState.update { it.copy(backStack = stack.dropLast(1)) }
    return true
  }

  fun updateReciterSearch(query: String) {
    _reciterSearchQuery.value = query
  }

  fun submitReciterSearch() {
    val query = _reciterSearchQuery.value.trim()
    if (query.length >= 2) {
      viewModelScope.launch { recentSearchesManager.saveSearchQuery(query) }
    }
  }

  fun selectReciterFilter(filter: ReciterFilter) {
    _reciterFilter.value = filter
    if (filter != ReciterFilter.ALL && filter != ReciterFilter.FAVORITES) {
      viewModelScope.launch { recentSearchesManager.saveSearchQuery(filter.labelAr) }
    }
  }

  fun openReciterFromDirectory(reciterId: String) {
    val activeQuery = _reciterSearchQuery.value.trim()
    if (activeQuery.length >= 2) {
      viewModelScope.launch { recentSearchesManager.saveSearchQuery(activeQuery) }
    }
    openReciterDetail(reciterId)
  }

  fun applyRecentSearch(term: String) {
    val matchingFilter =
      ReciterFilter.entries.firstOrNull {
        it != ReciterFilter.ALL && it != ReciterFilter.FAVORITES && it.labelAr == term
      }
    if (matchingFilter != null) {
      _reciterFilter.value = matchingFilter
      _reciterSearchQuery.value = ""
    } else {
      _reciterFilter.value = ReciterFilter.ALL
      _reciterSearchQuery.value = term
    }
    viewModelScope.launch { recentSearchesManager.saveSearchQuery(term) }
  }

  fun removeRecentSearch(term: String) {
    viewModelScope.launch { recentSearchesManager.removeSearchQuery(term) }
  }

  fun clearRecentSearches() {
    viewModelScope.launch { recentSearchesManager.clearAll() }
  }

  fun updateSurahSearch(query: String) {
    _surahSearchQuery.value = query
  }

  fun selectRiwayah(reciterId: String, riwayahIndex: Int) {
    _selectedRiwayahByReciter.update { it + (reciterId to riwayahIndex) }
  }

  fun playSurah(reciter: Reciter, riwayah: Riwayah, surah: Surah, startPositionMs: Long = 0L) {
    val portrait = _portraits.value[reciter.id]
    playbackManager.playSurah(
      reciter = reciter,
      riwayah = riwayah,
      surah = surah,
      startPositionMs = startPositionMs,
      portraitUrl = portrait,
    )
  }

  fun playOrResumeLastPlayed() {
    val last = preferencesState.value.lastPlayed ?: return
    val reciter = reciterCatalogRepository.findReciter(last.reciterId) ?: return
    val riwayahIndex = reciter.riwayat.indexOfFirst { it.nameAr == last.riwayahName }.coerceAtLeast(0)
    val riwayah = reciter.riwayat.getOrNull(riwayahIndex) ?: return
    val surah = QuranCatalog.findSurah(last.surahNumber) ?: return
    selectRiwayah(reciter.id, riwayahIndex)
    playSurah(reciter, riwayah, surah, startPositionMs = last.positionMs)
  }

  fun playTodayRecitation() {
    val today = QuranCatalog.todayRecitation()
    val reciter = reciterCatalogRepository.findReciter(today.reciterId) ?: return
    val riwayah = reciter.riwayat.getOrElse(today.riwayahIndex) { reciter.riwayat.first() }
    val surah = QuranCatalog.findSurah(today.surahNumber) ?: return
    playSurah(reciter, riwayah, surah)
  }

  fun toggleCairoRadio() {
    val current = playbackState.value
    if (current.activeTarget == ActiveAudioTarget.CairoRadio) {
      if (current.isPlaying || current.isBuffering) {
        playbackManager.stopPlayback()
      } else {
        playbackManager.playCairoRadio()
      }
    } else {
      playbackManager.playCairoRadio()
    }
  }

  fun togglePlayPause() = playbackManager.togglePlayPause()

  fun stopPlayback() {
    _isPlayerExpanded.value = false
    playbackManager.stopPlayback()
  }

  fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)

  fun seekBy(deltaMs: Long) = playbackManager.seekBy(deltaMs)

  fun seekToFraction(fraction: Float) = playbackManager.seekToFraction(fraction)

  fun playNextSurah() = playbackManager.playNextSurah()

  fun playPreviousSurah() = playbackManager.playPreviousSurah()

  fun setSleepTimer(option: SleepTimerOption) = playbackManager.setSleepTimer(option)

  fun clearPlaybackError() = playbackManager.clearErrorMessage()

  fun setPlayerExpanded(expanded: Boolean) {
    _isPlayerExpanded.value = expanded
  }

  fun setDarkMode(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setDarkMode(enabled) }
  }

  fun setFontScale(option: FontScaleOption) {
    viewModelScope.launch { preferencesRepository.setFontScale(option) }
  }

  fun setAutoPlayNext(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setAutoPlayNext(enabled) }
  }

  fun toggleFavoriteReciter(reciterId: String) {
    viewModelScope.launch { preferencesRepository.toggleFavoriteReciter(reciterId) }
  }

  fun incrementDuaCounter() {
    viewModelScope.launch { preferencesRepository.incrementDuaCounter() }
  }

  fun setWirdReminderEnabled(enabled: Boolean) {
    val current = preferencesState.value
    viewModelScope.launch {
      preferencesRepository.setWirdReminderEnabled(enabled)
      if (enabled) {
        wirdReminderManager.schedule(current.wirdReminderHour, current.wirdReminderMinute)
      } else {
        wirdReminderManager.cancel()
      }
    }
  }

  fun setWirdReminderTime(hour: Int, minute: Int) {
    val safeHour = hour.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    val enabled = preferencesState.value.wirdReminderEnabled
    viewModelScope.launch {
      preferencesRepository.setWirdReminderTime(safeHour, safeMinute)
      if (enabled) {
        wirdReminderManager.schedule(safeHour, safeMinute)
      }
    }
  }

  fun setWirdGoal(goal: DailyWirdGoal) {
    viewModelScope.launch { preferencesRepository.setWirdGoal(goal) }
  }

  fun toggleTodayWirdCompleted() {
    viewModelScope.launch { preferencesRepository.toggleTodayWirdCompleted() }
  }

  fun sendTestWirdNotification(): Boolean {
    return wirdReminderManager.showTestNotification(preferencesState.value.wirdGoal)
  }

  class Factory(
    private val preferencesRepository: UserPreferencesRepository,
    private val recentSearchesManager: RecentSearchesManager,
    private val reciterCatalogRepository: ReciterCatalogRepository,
    private val portraitRepository: WikipediaPortraitRepository,
    private val playbackManager: AudioPlaybackManager,
    private val wirdReminderManager: DailyWirdReminderManager,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return MainViewModel(
        preferencesRepository = preferencesRepository,
        recentSearchesManager = recentSearchesManager,
        reciterCatalogRepository = reciterCatalogRepository,
        portraitRepository = portraitRepository,
        playbackManager = playbackManager,
        wirdReminderManager = wirdReminderManager,
      ) as T
    }
  }
}
