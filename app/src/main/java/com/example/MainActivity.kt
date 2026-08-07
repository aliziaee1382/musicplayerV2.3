package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.glass.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private var openEqOnStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openEqOnStart = intent?.getBooleanExtra("OPEN_EQUALIZER", false) == true
        setContent {
            MyApplicationTheme {
                GlassAudioApp(openEqOnStart = openEqOnStart)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("OPEN_EQUALIZER", false)) {
            // Signal to open equalizer
            openEqOnStart = true
        }
    }
}

private fun checkAudioPermissionGranted(context: Context): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun GlassAudioApp(
    viewModel: MainViewModel = viewModel(),
    openEqOnStart: Boolean = false
) {
    val playerManager = viewModel.playerManager
    val context = LocalContext.current

    LaunchedEffect(openEqOnStart) {
        if (openEqOnStart) {
            viewModel.setShowEqualizer(true)
        }
    }

    var hasAudioPermission by remember { mutableStateOf(checkAudioPermissionGranted(context)) }
    var hasRequestedPermissionOnce by remember { mutableStateOf(false) }

    val audioPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    val permissionsToRequest = remember {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.READ_MEDIA_AUDIO)
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        list.toTypedArray()
    }

    val openAppSettings = {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val isGranted = checkAudioPermissionGranted(context)
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.scanAndLoadLocalAudio()
        }
    }

    val onRequestAudioPermission = {
        val isGranted = checkAudioPermissionGranted(context)
        hasAudioPermission = isGranted
        if (!isGranted) {
            val activity = context as? ComponentActivity
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, audioPermission)
            } ?: false

            if (hasRequestedPermissionOnce && !shouldShowRationale) {
                openAppSettings()
            } else {
                hasRequestedPermissionOnce = true
                permissionLauncher.launch(permissionsToRequest)
            }
        } else {
            viewModel.scanAndLoadLocalAudio()
        }
    }

    LaunchedEffect(Unit) {
        val isGranted = checkAudioPermissionGranted(context)
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.scanAndLoadLocalAudio()
        } else {
            hasRequestedPermissionOnce = true
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val isGranted = checkAudioPermissionGranted(context)
                    hasAudioPermission = isGranted
                    if (isGranted) {
                        viewModel.scanAndLoadLocalAudio()
                    }
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    try {
                        playerManager.triggerPlaybackStateSave()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Collect States
    val baseTheme by playerManager.currentTheme.collectAsStateWithLifecycle()
    val isAutoSystemTheme by viewModel.isAutoSystemTheme.collectAsStateWithLifecycle()
    val systemInDark = isSystemInDarkTheme()

    val currentTheme = remember(baseTheme, isAutoSystemTheme, systemInDark) {
        if (isAutoSystemTheme) {
            com.example.model.GlassTheme.getThemeForModeAndColor(!systemInDark, baseTheme.colorKey)
        } else {
            baseTheme
        }
    }
    val currentTrack by playerManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by playerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by playerManager.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by playerManager.durationMs.collectAsStateWithLifecycle()
    val isShuffle by playerManager.isShuffle.collectAsStateWithLifecycle()
    val repeatMode by playerManager.repeatMode.collectAsStateWithLifecycle()
    val sleepTimerSeconds by playerManager.sleepTimerSeconds.collectAsStateWithLifecycle()

    val activeEqPreset by playerManager.activeEqPreset.collectAsStateWithLifecycle()
    val eqBandGains by playerManager.eqBandGains.collectAsStateWithLifecycle()
    val minDurationFilterSeconds by viewModel.minDurationFilter.collectAsStateWithLifecycle()
    val listItemSize by viewModel.listItemSize.collectAsStateWithLifecycle()

    val tracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val hiddenTracks by viewModel.hiddenTracks.collectAsStateWithLifecycle()
    val editingTrack by viewModel.editingTrack.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val activeNavTab by viewModel.activeNavTab.collectAsStateWithLifecycle()
    val librarySortTab by viewModel.librarySortTab.collectAsStateWithLifecycle()

    val sortCriterion by viewModel.sortCriterion.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    val showEqualizer by viewModel.showEqualizer.collectAsStateWithLifecycle()
    val showSleepTimer by viewModel.showSleepTimer.collectAsStateWithLifecycle()
    val showThemeSelector by viewModel.showThemeSelector.collectAsStateWithLifecycle()
    val showCreatePlaylist by viewModel.showCreatePlaylist.collectAsStateWithLifecycle()
    val targetTrackForPlaylist by viewModel.targetTrackForPlaylist.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val selectedPlaylistTracks by viewModel.selectedPlaylistTracks.collectAsStateWithLifecycle()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsStateWithLifecycle()
    var selectedPlaylistInitialEditMode by remember { mutableStateOf(false) }
    var showHiddenTracksDialog by remember { mutableStateOf(false) }

    // Handle system back button step-by-step
    val isBackHandlerEnabled = isNowPlayingExpanded ||
            showEqualizer ||
            showSleepTimer ||
            showThemeSelector ||
            showCreatePlaylist ||
            showHiddenTracksDialog ||
            editingTrack != null ||
            targetTrackForPlaylist != null ||
            selectedPlaylist != null ||
            activeNavTab != "Home" ||
            selectedCategory != "All"

    BackHandler(enabled = isBackHandlerEnabled) {
        when {
            isNowPlayingExpanded -> viewModel.setNowPlayingExpanded(false)
            showEqualizer -> viewModel.setShowEqualizer(false)
            showSleepTimer -> viewModel.setShowSleepTimer(false)
            showThemeSelector -> viewModel.setShowThemeSelector(false)
            showCreatePlaylist -> viewModel.setShowCreatePlaylist(false)
            showHiddenTracksDialog -> showHiddenTracksDialog = false
            editingTrack != null -> viewModel.openEditTrack(null)
            targetTrackForPlaylist != null -> viewModel.openAddToPlaylistForTrack(null)
            selectedPlaylist != null -> viewModel.setSelectedPlaylist(null)
            activeNavTab != "Home" -> viewModel.setActiveNavTab("Home")
            selectedCategory != "All" -> viewModel.setSelectedCategory("All")
        }
    }

    GlassBackgroundContainer(
        theme = currentTheme,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Screen Content based on bottom navigation
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AnimatedContent(
                        targetState = activeNavTab,
                        transitionSpec = {
                            val tabOrder = listOf("Home", "Explore", "Library", "Settings")
                            val initialIndex = tabOrder.indexOf(initialState).coerceAtLeast(0)
                            val targetIndex = tabOrder.indexOf(targetState).coerceAtLeast(0)

                            if (targetIndex > initialIndex) {
                                (slideInHorizontally(
                                    initialOffsetX = { width -> (width * 0.20f).toInt() },
                                    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)
                                ) + fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f)) togetherWith
                                (slideOutHorizontally(
                                    targetOffsetX = { width -> -(width * 0.20f).toInt() },
                                    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)
                                ) + fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.98f))
                            } else {
                                (slideInHorizontally(
                                    initialOffsetX = { width -> -(width * 0.20f).toInt() },
                                    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)
                                ) + fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f)) togetherWith
                                (slideOutHorizontally(
                                    targetOffsetX = { width -> (width * 0.20f).toInt() },
                                    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)
                                ) + fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.98f))
                            }
                        },
                        label = "main_screen_transition"
                    ) { targetTab ->
                        when (targetTab) {
                            "Home" -> HomeScreen(
                                tracks = tracks,
                                recentlyPlayed = recentlyPlayed,
                                selectedCategory = selectedCategory,
                                searchQuery = searchQuery,
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                theme = currentTheme,
                                listItemSize = listItemSize,
                                hasAudioPermission = hasAudioPermission,
                                onRequestPermission = onRequestAudioPermission,
                                sortCriterion = sortCriterion,
                                sortOrder = sortOrder,
                                onSelectCategory = { viewModel.setSelectedCategory(it) },
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onSortCriterionChange = { criterion -> viewModel.setSortPreference(criterion, sortOrder) },
                                onSortOrderChange = { order -> viewModel.setSortPreference(sortCriterion, order) },
                                onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                                onShufflePlay = { queue -> viewModel.playShuffleAll(queue) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onOpenThemeSelector = { viewModel.setShowThemeSelector(true) },
                                onOpenEqualizer = { viewModel.setShowEqualizer(true) },
                                onOpenAddToPlaylist = { viewModel.openAddToPlaylistForTrack(it) },
                                onScanLocalMusic = { permissionLauncher.launch(permissionsToRequest) }
                            )

                            "Explore" -> ExploreScreen(
                                tracks = tracks,
                                theme = currentTheme,
                                listItemSize = listItemSize,
                                onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onOpenAddToPlaylist = { viewModel.openAddToPlaylistForTrack(it) }
                            )

                            "Library" -> LibraryScreen(
                                playlists = playlists,
                                tracks = tracks,
                                favoriteTracks = favoriteTracks,
                                folders = viewModel.sampleFolders,
                                activeSortTab = librarySortTab,
                                theme = currentTheme,
                                listItemSize = listItemSize,
                                onSortTabChange = { viewModel.setLibrarySortTab(it) },
                                onOpenCreatePlaylist = { viewModel.setShowCreatePlaylist(true) },
                                onOpenThemeSelector = { viewModel.setShowThemeSelector(true) },
                                onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onOpenAddToPlaylist = { viewModel.openAddToPlaylistForTrack(it) },
                                onSelectPlaylist = {
                                    selectedPlaylistInitialEditMode = false
                                    viewModel.setSelectedPlaylist(it)
                                },
                                onEditPlaylist = {
                                    selectedPlaylistInitialEditMode = true
                                    viewModel.setSelectedPlaylist(it)
                                },
                                onDeletePlaylist = { playlistId -> viewModel.deletePlaylist(playlistId) }
                            )

                            "Settings" -> SettingsScreen(
                                theme = currentTheme,
                                minDurationFilterSeconds = minDurationFilterSeconds,
                                onMinDurationFilterChange = { viewModel.setMinDurationFilter(it) },
                                listItemSize = listItemSize,
                                onListItemSizeChange = { viewModel.setListItemSize(it) },
                                onOpenThemeSelector = { viewModel.setShowThemeSelector(true) },
                                onOpenEqualizer = { viewModel.setShowEqualizer(true) },
                                onScanLocalMusic = { permissionLauncher.launch(permissionsToRequest) },
                                onOpenHiddenTracks = { showHiddenTracksDialog = true },
                                hiddenCount = hiddenTracks.size
                            )
                        }
                    }
                }
            }

            // Bottom Navigation & Mini Player Column
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // Mini Player Bar with smooth spring entrance/exit
                AnimatedVisibility(
                    visible = currentTrack != null && !isNowPlayingExpanded,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    ) + fadeIn(animationSpec = tween(220)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeOut(animationSpec = tween(180))
                ) {
                    currentTrack?.let { track ->
                        MiniPlayerBar(
                            track = track,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            theme = currentTheme,
                            onTogglePlayPause = { playerManager.togglePlayPause() },
                            onNext = { playerManager.nextTrack() },
                            onExpandNowPlaying = { viewModel.setNowPlayingExpanded(true) }
                        )
                    }
                }

                // Glass Bottom Navigation Bar
                GlassBottomNavBar(
                    activeTab = activeNavTab,
                    onTabSelected = { viewModel.setActiveNavTab(it) },
                    theme = currentTheme
                )
            }

            // Full Expanded Now Playing Screen Overlay
            AnimatedVisibility(
                visible = isNowPlayingExpanded,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                ) + fadeIn(animationSpec = tween(250)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                NowPlayingSheet(
                    track = currentTrack ?: tracks.firstOrNull(),
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    theme = currentTheme,
                    sleepTimerSeconds = sleepTimerSeconds,
                    onTogglePlayPause = { playerManager.togglePlayPause() },
                    onNext = { playerManager.nextTrack() },
                    onPrevious = { playerManager.previousTrack() },
                    onSeek = { playerManager.seekTo(it.toInt()) },
                    onToggleShuffle = { playerManager.toggleShuffle() },
                    onToggleRepeat = { playerManager.toggleRepeat() },
                    onCyclePlaybackMode = { playerManager.cyclePlaybackMode() },
                    onToggleFavorite = { track -> track?.let { viewModel.toggleFavorite(it) } },
                    onOpenEqualizer = { viewModel.setShowEqualizer(true) },
                    onOpenSleepTimer = { viewModel.setShowSleepTimer(true) },
                    onOpenAddToPlaylist = { track -> viewModel.openAddToPlaylistForTrack(track) },
                    onEditTrack = { track -> viewModel.openEditTrack(track) },
                    onHideTrack = { trackId -> viewModel.hideTrack(trackId) },
                    onDeleteTrack = { trackId -> viewModel.deleteTrack(trackId) },
                    onCollapse = { viewModel.setNowPlayingExpanded(false) }
                )
            }

            // Interactive Glass Dialogs
            if (showEqualizer) {
                EqualizerDialog(
                    activePreset = activeEqPreset,
                    bandGains = eqBandGains,
                    onPresetSelected = { preset -> viewModel.setEqualizerPreset(preset) },
                    onGainChanged = { bandIdx, gain -> viewModel.updateCustomEqGain(bandIdx, gain) },
                    onDismiss = { viewModel.setShowEqualizer(false) },
                    theme = currentTheme
                )
            }

            if (showSleepTimer) {
                SleepTimerDialog(
                    currentTimerSec = sleepTimerSeconds,
                    onSetTimerMinutes = { mins -> playerManager.setSleepTimerMinutes(mins) },
                    onDismiss = { viewModel.setShowSleepTimer(false) },
                    theme = currentTheme
                )
            }

            if (showThemeSelector) {
                ThemeSelectorDialog(
                    activeTheme = currentTheme,
                    isAutoSystemTheme = isAutoSystemTheme,
                    onSelectTheme = { theme, isAuto -> viewModel.selectTheme(theme, isAuto) },
                    onDismiss = { viewModel.setShowThemeSelector(false) }
                )
            }

            if (showCreatePlaylist) {
                CreatePlaylistDialog(
                    onCreate = { name -> viewModel.createPlaylist(name) },
                    onDismiss = { viewModel.setShowCreatePlaylist(false) },
                    theme = currentTheme
                )
            }

            if (targetTrackForPlaylist != null) {
                AddToPlaylistDialog(
                    playlists = playlists,
                    track = targetTrackForPlaylist!!,
                    onAddToPlaylist = { playlistId ->
                        viewModel.addTrackToPlaylist(playlistId, targetTrackForPlaylist!!.id)
                        viewModel.openAddToPlaylistForTrack(null)
                    },
                    onCreateNewPlaylist = {
                        viewModel.openAddToPlaylistForTrack(null)
                        viewModel.setShowCreatePlaylist(true)
                    },
                    onDismiss = { viewModel.openAddToPlaylistForTrack(null) },
                    theme = currentTheme
                )
            }

            if (selectedPlaylist != null) {
                PlaylistDetailsDialog(
                    playlist = selectedPlaylist!!,
                    tracks = selectedPlaylistTracks,
                    initialEditMode = selectedPlaylistInitialEditMode,
                    onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                    onPlayAll = { viewModel.playPlaylistQueue(selectedPlaylistTracks) },
                    onRemoveTrack = { trackId -> viewModel.removeTrackFromPlaylist(selectedPlaylist!!.id, trackId) },
                    onRemoveTracks = { trackIds -> viewModel.removeTracksFromPlaylist(selectedPlaylist!!.id, trackIds) },
                    onDeletePlaylist = { playlistId -> viewModel.deletePlaylist(playlistId) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onDismiss = {
                        selectedPlaylistInitialEditMode = false
                        viewModel.setSelectedPlaylist(null)
                    },
                    theme = currentTheme
                )
            }

            if (editingTrack != null) {
                EditTrackInfoDialog(
                    track = editingTrack!!,
                    onSave = { title, artist, album -> viewModel.updateTrackInfo(editingTrack!!.id, title, artist, album) },
                    onDismiss = { viewModel.openEditTrack(null) },
                    theme = currentTheme
                )
            }

            if (showHiddenTracksDialog) {
                HiddenTracksDialog(
                    hiddenTracks = hiddenTracks,
                    onUnhideTrack = { trackId -> viewModel.unhideTrack(trackId) },
                    onUnhideAll = { viewModel.unhideAllTracks() },
                    onDismiss = { showHiddenTracksDialog = false },
                    theme = currentTheme
                )
            }
        }
    }
}
