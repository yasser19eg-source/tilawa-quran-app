package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.imageLoader
import com.example.model.AppRoute
import com.example.model.AppTab
import com.example.ui.components.ExpandedPlayerSheet
import com.example.ui.components.MiniPlayerBar
import com.example.data.QuranCatalog
import com.example.ui.screens.AdhkarScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MushafPageReaderScreen
import com.example.ui.screens.QuranMushafScreen
import com.example.ui.screens.RadioScreen
import com.example.ui.screens.ReciterDetailScreen
import com.example.ui.screens.RecitersScreen
import com.example.ui.screens.SadaqahScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SurahReaderScreen
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.MushafAlTilawahTheme
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val app = application as MushafApplication
    val factory =
      MainViewModel.Factory(
        preferencesRepository = app.preferencesRepository,
        recentSearchesManager = app.recentSearchesManager,
        reciterCatalogRepository = app.reciterCatalogRepository,
        portraitRepository = app.wikipediaPortraitRepository,
        quranMushafRepository = app.quranMushafRepository,
        mushafPageRepository = app.mushafPageRepository,
        playbackManager = app.audioPlaybackManager,
        wirdReminderManager = app.wirdReminderManager,
      )

    setContent {
      val viewModel: MainViewModel = viewModel(factory = factory)
      val preferences by viewModel.preferencesState.collectAsStateWithLifecycle()
      val showSplash by viewModel.showSplash.collectAsStateWithLifecycle()

      LaunchedEffect(preferences.isDarkMode) {
        if (preferences.isDarkMode) {
          enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
          )
        } else {
          enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
          )
        }
      }

      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MushafAlTilawahTheme(
          darkTheme = preferences.isDarkMode,
          fontScaleFactor = preferences.fontScaleOption.scaleFactor,
        ) {
          Crossfade(
            targetState = showSplash,
            animationSpec = tween(durationMillis = 480),
            label = "splash_transition",
          ) { isSplashVisible ->
            if (isSplashVisible) {
              SplashScreen(onSplashFinished = viewModel::dismissSplash)
            } else {
              MushafAppRoot(viewModel = viewModel)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MushafAppRoot(viewModel: MainViewModel) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val preferences by viewModel.preferencesState.collectAsStateWithLifecycle()
  val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
  val reciters by viewModel.reciters.collectAsStateWithLifecycle()
  val isCatalogSyncing by viewModel.isCatalogSyncing.collectAsStateWithLifecycle()
  val navState by viewModel.navState.collectAsStateWithLifecycle()
  val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
  val portraits by viewModel.portraits.collectAsStateWithLifecycle()
  val reciterSearch by viewModel.reciterSearchQuery.collectAsStateWithLifecycle()
  val reciterFilter by viewModel.reciterFilter.collectAsStateWithLifecycle()
  val selectedRiwayahMap by viewModel.selectedRiwayahByReciter.collectAsStateWithLifecycle()
  val surahSearch by viewModel.surahSearchQuery.collectAsStateWithLifecycle()
  val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsStateWithLifecycle()
  val mushafReaderState by viewModel.mushafReaderState.collectAsStateWithLifecycle()
  val cachedMushafSurahs by viewModel.cachedMushafSurahs.collectAsStateWithLifecycle()
  val adhkarCounts by viewModel.adhkarCounts.collectAsStateWithLifecycle()

  var hasPromptedNotificationPermission by remember { mutableStateOf(false) }
  var pendingWirdEnableOnGrant by remember { mutableStateOf(false) }
  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (pendingWirdEnableOnGrant) {
        pendingWirdEnableOnGrant = false
        if (granted) {
          viewModel.setWirdReminderEnabled(true)
          scope.launch {
            snackbarHostState.showSnackbar("تم تفعيل تذكير الورد اليومي بنجاح")
          }
        } else {
          viewModel.setWirdReminderEnabled(false)
          scope.launch {
            snackbarHostState.showSnackbar("يرجى السماح بالإشعارات لتصلك تنبيهات الورد اليومي")
          }
        }
      }
    }

  val requestNotificationPermissionIfNeeded: () -> Unit = {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !hasPromptedNotificationPermission &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
    ) {
      hasPromptedNotificationPermission = true
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  val requestWirdReminderToggle: (Boolean) -> Unit = { enabled ->
    if (!enabled) {
      viewModel.setWirdReminderEnabled(false)
    } else if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
    ) {
      pendingWirdEnableOnGrant = true
      hasPromptedNotificationPermission = true
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      viewModel.setWirdReminderEnabled(true)
      scope.launch { snackbarHostState.showSnackbar("تم تفعيل تذكير الورد اليومي") }
    }
  }

  LaunchedEffect(playbackState.errorMessage) {
    val message = playbackState.errorMessage
    if (!message.isNullOrBlank()) {
      snackbarHostState.showSnackbar(message)
      viewModel.clearPlaybackError()
    }
  }

  val currentRoute = navState.currentRoute
  val topBarTitle =
    when (currentRoute) {
      is AppRoute.Tab ->
        when (currentRoute.tab) {
          AppTab.HOME -> "تلاوة"
          AppTab.RECITERS -> "القراء"
          AppTab.RADIO -> "إذاعة القرآن الكريم"
          AppTab.SADAQAH -> "صدقة جارية"
        }
      is AppRoute.ReciterDetail ->
        reciters.firstOrNull { it.id == currentRoute.reciterId }?.nameAr ?: "القارئ"
      AppRoute.Favorites -> "القراء المفضلون"
      AppRoute.Settings -> "الإعدادات"
      AppRoute.QuranMushaf -> "فهرس السور والأجزاء"
      is AppRoute.SurahReader -> {
        val surahName = QuranCatalog.findSurah(currentRoute.surahNumber)?.nameAr
        if (surahName != null) "سورة $surahName" else "القرآن الكريم"
      }
      is AppRoute.MushafPageReader -> "المصحف الشريف"
      AppRoute.Adhkar -> "الأذكار اليومية"
    }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useNavigationRail) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxHeight(),
        ) {
          Spacer(modifier = Modifier.size(12.dp))
          AppTab.entries.forEach { tab ->
            val selected = currentRoute is AppRoute.Tab && currentRoute.tab == tab
            NavigationRailItem(
              selected = selected,
              onClick = { viewModel.navigateToTab(tab) },
              icon = { TabIcon(tab = tab, selected = selected) },
              label = { Text(tab.labelAr, style = MaterialTheme.typography.labelSmall) },
              colors =
                NavigationRailItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
              modifier = Modifier.testTag("nav_rail_${tab.name}"),
            )
          }
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          if (currentRoute !is AppRoute.MushafPageReader) {
            CenterAlignedTopAppBar(
            title = {
              Text(
                text = topBarTitle,
                style =
                  if (currentRoute == AppRoute.Tab(AppTab.HOME))
                    MaterialTheme.typography.headlineSmall.copy(fontFamily = AmiriFontFamily)
                  else MaterialTheme.typography.titleLarge,
                maxLines = 1,
              )
            },
            navigationIcon = {
              if (navState.canGoBack) {
                TextButton(
                  onClick = { viewModel.navigateBack() },
                  contentPadding = PaddingValues(horizontal = 10.dp),
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("top_bar_previous_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "السابق",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "السابق",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            },
            actions = {
              val isFavorites = currentRoute == AppRoute.Favorites
              val favoritesCount = preferences.favoriteReciterIds.size
              IconButton(
                onClick = {
                  if (isFavorites) viewModel.navigateBack() else viewModel.openFavorites()
                },
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("top_bar_favorites_button"),
              ) {
                BadgedBox(
                  badge = {
                    if (favoritesCount > 0) {
                      Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                      ) {
                        Text(favoritesCount.toString())
                      }
                    }
                  }
                ) {
                  Icon(
                    imageVector =
                      if (isFavorites) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "القراء المفضلون",
                    tint =
                      if (isFavorites) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              val isSettings = currentRoute == AppRoute.Settings
              IconButton(
                onClick = {
                  if (isSettings) viewModel.navigateBack() else viewModel.openSettings()
                },
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("top_bar_settings_button"),
              ) {
                Icon(
                  imageVector =
                    if (isSettings) Icons.Rounded.Settings else Icons.Outlined.Settings,
                  contentDescription = "الإعدادات",
                  tint =
                    if (isSettings) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            },
            colors =
              TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
              ),
          )
          }
        },
        bottomBar = {
          Column(
            modifier =
              if (useNavigationRail) Modifier.navigationBarsPadding() else Modifier
          ) {
            if (playbackState.activeTarget != null) {
              MiniPlayerBar(
                playbackState = playbackState,
                portraits = portraits,
                onExpand = { viewModel.setPlayerExpanded(true) },
                onTogglePlayPause = {
                  requestNotificationPermissionIfNeeded()
                  viewModel.togglePlayPause()
                },
                onStop = { viewModel.stopPlayback() },
              )
            }

            if (
              !useNavigationRail &&
                currentRoute !is AppRoute.SurahReader &&
                currentRoute !is AppRoute.MushafPageReader
            ) {
              NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
              ) {
                AppTab.entries.forEach { tab ->
                  val selected = currentRoute is AppRoute.Tab && currentRoute.tab == tab
                  NavigationBarItem(
                    selected = selected,
                    onClick = { viewModel.navigateToTab(tab) },
                    icon = { TabIcon(tab = tab, selected = selected) },
                    label = {
                      Text(
                        text = tab.labelAr,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                      )
                    },
                    colors =
                      NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      ),
                    modifier = Modifier.testTag("bottom_nav_${tab.name}"),
                  )
                }
              }
            }
          }
        },
      ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          when (currentRoute) {
            is AppRoute.Tab -> {
              when (currentRoute.tab) {
                AppTab.HOME -> {
                  HomeScreen(
                    reciters = reciters,
                    portraits = portraits,
                    preferences = preferences,
                    onRequestPortrait = viewModel::ensurePortraitLoaded,
                    onToggleFavorite = viewModel::toggleFavoriteReciter,
                    onOpenFavorites = viewModel::openFavorites,
                    onToggleWirdCompleted = {
                      val wasDone = preferences.isTodayWirdCompleted()
                      viewModel.toggleTodayWirdCompleted()
                      if (!wasDone) {
                        scope.launch {
                          snackbarHostState.showSnackbar("تقبل الله تلاوتك، تم تسجيل إنجاز ورد اليوم")
                        }
                      }
                    },
                    onOpenWirdSettings = viewModel::openSettings,
                    onPlayTodayRecitation = {
                      requestNotificationPermissionIfNeeded()
                      viewModel.playTodayRecitation()
                    },
                    onResumeLastPlayed = {
                      requestNotificationPermissionIfNeeded()
                      viewModel.playOrResumeLastPlayed()
                    },
                    onReciterClick = { reciter -> viewModel.openReciterDetail(reciter.id) },
                    onSeeAllReciters = { viewModel.navigateToTab(AppTab.RECITERS) },
                    onOpenQuranMushaf = viewModel::openQuranMushaf,
                    onOpenAdhkar = viewModel::openAdhkar,
                  )
                }

                AppTab.RECITERS -> {
                  RecitersScreen(
                    reciters = reciters,
                    isCatalogSyncing = isCatalogSyncing,
                    searchQuery = reciterSearch,
                    selectedFilter = reciterFilter,
                    favoriteReciterIds = preferences.favoriteReciterIds,
                    recentSearches = recentSearches,
                    portraits = portraits,
                    onRequestPortrait = viewModel::ensurePortraitLoaded,
                    onSearchQueryChange = viewModel::updateReciterSearch,
                    onSubmitSearch = viewModel::submitReciterSearch,
                    onSelectFilter = viewModel::selectReciterFilter,
                    onApplyRecentSearch = viewModel::applyRecentSearch,
                    onRemoveRecentSearch = viewModel::removeRecentSearch,
                    onClearRecentSearches = viewModel::clearRecentSearches,
                    onToggleFavorite = viewModel::toggleFavoriteReciter,
                    onReciterClick = { reciter -> viewModel.openReciterFromDirectory(reciter.id) },
                  )
                }

                AppTab.RADIO -> {
                  RadioScreen(
                    playbackState = playbackState,
                    onToggleRadio = {
                      requestNotificationPermissionIfNeeded()
                      viewModel.toggleCairoRadio()
                    },
                  )
                }

                AppTab.SADAQAH -> {
                  SadaqahScreen(
                    duaCounter = preferences.duaCounter,
                    onIncrementDuaCounter = viewModel::incrementDuaCounter,
                    onShowFeedback = { msg ->
                      scope.launch { snackbarHostState.showSnackbar(msg) }
                    },
                  )
                }
              }
            }

            is AppRoute.ReciterDetail -> {
              val reciter = reciters.firstOrNull { it.id == currentRoute.reciterId }
              if (reciter != null) {
                val riwayahIndex =
                  (selectedRiwayahMap[reciter.id] ?: 0).coerceIn(0, reciter.riwayat.lastIndex)
                ReciterDetailScreen(
                  reciter = reciter,
                  portraitUrl = portraits[reciter.id],
                  portraits = portraits,
                  isFavorite = preferences.favoriteReciterIds.contains(reciter.id),
                  selectedRiwayahIndex = riwayahIndex,
                  surahSearchQuery = surahSearch,
                  playbackState = playbackState,
                  onRequestPortrait = viewModel::ensurePortraitLoaded,
                  onBackClick = { viewModel.navigateBack() },
                  onToggleFavorite = { viewModel.toggleFavoriteReciter(reciter.id) },
                  onSelectRiwayah = { idx -> viewModel.selectRiwayah(reciter.id, idx) },
                  onSurahSearchChange = viewModel::updateSurahSearch,
                  onPlaySurah = { riwayah, surah ->
                    requestNotificationPermissionIfNeeded()
                    viewModel.playSurah(reciter, riwayah, surah)
                  },
                  onTogglePlayPause = {
                    requestNotificationPermissionIfNeeded()
                    viewModel.togglePlayPause()
                  },
                  onSeekTo = viewModel::seekTo,
                  onSeekBy = viewModel::seekBy,
                  onPreviousSurah = viewModel::playPreviousSurah,
                  onNextSurah = viewModel::playNextSurah,
                  onExpandPlayer = { viewModel.setPlayerExpanded(true) },
                  onStopPlayback = viewModel::stopPlayback,
                )
              }
            }

            AppRoute.Favorites -> {
              FavoritesScreen(
                reciters = reciters,
                favoriteReciterIds = preferences.favoriteReciterIds,
                portraits = portraits,
                onRequestPortrait = viewModel::ensurePortraitLoaded,
                onBackClick = { viewModel.navigateBack() },
                onToggleFavorite = viewModel::toggleFavoriteReciter,
                onReciterClick = { reciter -> viewModel.openReciterDetail(reciter.id) },
                onBrowseRecitersClick = { viewModel.navigateToTab(AppTab.RECITERS) },
              )
            }

            AppRoute.Settings -> {
              SettingsScreen(
                preferences = preferences,
                activeSleepTimer = playbackState.sleepTimerOption,
                onBackClick = { viewModel.navigateBack() },
                onToggleDarkMode = viewModel::setDarkMode,
                onSelectFontScale = viewModel::setFontScale,
                onToggleAutoPlayNext = viewModel::setAutoPlayNext,
                onSelectSleepTimer = viewModel::setSleepTimer,
                onToggleWirdReminder = requestWirdReminderToggle,
                onSelectWirdTime = viewModel::setWirdReminderTime,
                onSelectWirdGoal = viewModel::setWirdGoal,
                onSendTestWirdNotification = {
                  val sent = viewModel.sendTestWirdNotification()
                  if (sent) {
                    scope.launch {
                      snackbarHostState.showSnackbar("تم إرسال إشعار تجريبي للورد اليومي")
                    }
                  } else {
                    requestWirdReminderToggle(true)
                  }
                },
              )
            }

            AppRoute.QuranMushaf -> {
              QuranMushafScreen(
                preferences = preferences,
                cachedSurahNumbers = cachedMushafSurahs,
                onOpenSurah = { surahNum -> viewModel.openSurahReader(surahNum) },
                onOpenMushafPage = { pageNum -> viewModel.openMushafPage(pageNum) },
                onBackClick = { viewModel.navigateBack() },
              )
            }

            is AppRoute.MushafPageReader -> {
              MushafPageReaderScreen(
                initialPage = currentRoute.initialPage,
                bookmarkedPage = preferences.lastReadMushafPage,
                hasBookmark = preferences.hasMushafBookmark,
                getPageMeta = viewModel::getMushafPageMeta,
                getPageBounds = viewModel::getMushafPageBounds,
                onPageChanged = { pageNum ->
                  viewModel.onMushafPageViewed(
                    pageNumber = pageNum,
                    imageLoader = context.imageLoader,
                    context = context,
                  )
                },
                onResolveSelectedAyahs = viewModel::resolveSelectedMushafAyahs,
                onLoadSurahAyahs = viewModel::loadSurahForShare,
                onSavePageBookmark = viewModel::saveMushafPageBookmark,
                onShowFeedback = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                onOpenFullIndex = { viewModel.navigateBack() },
                onBackClick = { viewModel.navigateBack() },
              )
            }

            is AppRoute.SurahReader -> {
              SurahReaderScreen(
                surahNumber = currentRoute.surahNumber,
                readerState = mushafReaderState,
                fontScale = preferences.mushafReaderFontScale,
                bookmarkedSurahNumber = preferences.lastReadSurahNumber,
                bookmarkedAyahNumber = preferences.lastReadAyahNumber,
                hasBookmark = preferences.hasMushafBookmark,
                onChangeFontScale = viewModel::setMushafReaderFontScale,
                onSaveBookmark = viewModel::saveMushafBookmark,
                onNavigateSurah = { nextNum -> viewModel.openSurahReader(nextNum) },
                onLoadSurahAyahs = viewModel::loadSurahForShare,
                onRetry = {
                  viewModel.loadSurahForReading(currentRoute.surahNumber, forceRefresh = true)
                },
                onShowFeedback = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                onBackClick = { viewModel.navigateBack() },
              )
            }

            AppRoute.Adhkar -> {
              AdhkarScreen(
                adhkarCounts = adhkarCounts,
                onIncrementDhikr = viewModel::incrementDhikrCount,
                onResetCategory = viewModel::resetAdhkarCategory,
                onShowFeedback = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                onBackClick = { viewModel.navigateBack() },
              )
            }
          }
        }
      }
    }
  }

  if (isPlayerExpanded && playbackState.activeTarget != null) {
    ExpandedPlayerSheet(
      playbackState = playbackState,
      portraits = portraits,
      autoPlayNext = preferences.autoPlayNext,
      onDismiss = { viewModel.setPlayerExpanded(false) },
      onTogglePlayPause = {
        requestNotificationPermissionIfNeeded()
        viewModel.togglePlayPause()
      },
      onStop = { viewModel.stopPlayback() },
      onSeekTo = viewModel::seekTo,
      onSeekBy = viewModel::seekBy,
      onNextSurah = viewModel::playNextSurah,
      onPreviousSurah = viewModel::playPreviousSurah,
      onToggleAutoPlayNext = viewModel::setAutoPlayNext,
      onSelectSleepTimer = viewModel::setSleepTimer,
    )
  }
}

@Composable
private fun TabIcon(tab: AppTab, selected: Boolean) {
  val icon =
    when (tab) {
      AppTab.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
      AppTab.RECITERS -> if (selected) Icons.Rounded.People else Icons.Outlined.People
      AppTab.RADIO -> if (selected) Icons.Rounded.Radio else Icons.Outlined.Radio
      AppTab.SADAQAH ->
        if (selected) Icons.Rounded.VolunteerActivism else Icons.Outlined.VolunteerActivism
    }
  Icon(imageVector = icon, contentDescription = tab.labelAr)
}
