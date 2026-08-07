package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.MusicRepository
import com.example.data.local.UserPreferencesEntity
import com.example.model.*
import com.example.player.AudioPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository
    private val localAudioScanner: com.example.data.local.LocalAudioScanner
    val playerManager: AudioPlayerManager

    // ViewModel UI States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _activeNavTab = MutableStateFlow("Home") // Home, Explore, Library, VIP
    val activeNavTab: StateFlow<String> = _activeNavTab.asStateFlow()

    private val _librarySortTab = MutableStateFlow("Playlists") // Playlists, Songs, Albums, Artists, Folders
    val librarySortTab: StateFlow<String> = _librarySortTab.asStateFlow()

    // Dialog States
    private val _showEqualizer = MutableStateFlow(false)
    val showEqualizer: StateFlow<Boolean> = _showEqualizer.asStateFlow()

    private val _showSleepTimer = MutableStateFlow(false)
    val showSleepTimer: StateFlow<Boolean> = _showSleepTimer.asStateFlow()

    private val _showThemeSelector = MutableStateFlow(false)
    val showThemeSelector: StateFlow<Boolean> = _showThemeSelector.asStateFlow()

    private val _showCreatePlaylist = MutableStateFlow(false)
    val showCreatePlaylist: StateFlow<Boolean> = _showCreatePlaylist.asStateFlow()

    private val _targetTrackForPlaylist = MutableStateFlow<Track?>(null)
    val targetTrackForPlaylist: StateFlow<Track?> = _targetTrackForPlaylist.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    // Data Flows from Repository
    val minDurationFilter: StateFlow<Int>
    val allTracks: StateFlow<List<Track>>
    val hiddenTracks: StateFlow<List<Track>>
    val favoriteTracks: StateFlow<List<Track>>
    val recentlyPlayed: StateFlow<List<Track>>
    val allPlaylists: StateFlow<List<Playlist>>
    val selectedPlaylistTracks: StateFlow<List<Track>>
    val sampleFolders: List<AudioFolder>

    val sortCriterion: StateFlow<TrackSortCriterion>
    val sortOrder: StateFlow<TrackSortOrder>
    val isAutoSystemTheme: StateFlow<Boolean>
    val listItemSize: StateFlow<ListItemSize>

    private val _editingTrack = MutableStateFlow<Track?>(null)
    val editingTrack: StateFlow<Track?> = _editingTrack.asStateFlow()

    init {
        val dao = AppDatabase.getDatabase(application).musicDao()
        repository = MusicRepository(dao)
        localAudioScanner = com.example.data.local.LocalAudioScanner(application)
        playerManager = AudioPlayerManager.getInstance(application)
        playerManager.onFlushListeningTimeListener = { trackId, seconds ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.addListeningTime(trackId, seconds)
            }
        }
        playerManager.onPlaybackStateChanged = { trackId, posMs, queueIds ->
            viewModelScope.launch {
                repository.updatePlaybackState(trackId, posMs, queueIds.joinToString(","))
            }
        }
        sampleFolders = repository.getSampleFolders()

        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }

        val userPrefsFlow = repository.userPreferences.filterNotNull()

        isAutoSystemTheme = userPrefsFlow
            .map { it.isAutoSystemTheme }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                true
            )

        minDurationFilter = userPrefsFlow
            .map { it.minDurationFilterSeconds }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0
            )

        listItemSize = userPrefsFlow
            .map { ListItemSize.fromName(it.listItemSize) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                ListItemSize.SMALL
            )

        allTracks = combine(repository.allTracks, minDurationFilter) { tracks, minSecs ->
            if (minSecs > 0) tracks.filter { it.durationSeconds >= minSecs } else tracks
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        hiddenTracks = repository.hiddenTracks.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        favoriteTracks = combine(repository.favoriteTracks, minDurationFilter) { tracks, minSecs ->
            if (minSecs > 0) tracks.filter { it.durationSeconds >= minSecs } else tracks
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        recentlyPlayed = combine(repository.recentlyPlayed, minDurationFilter) { tracks, minSecs ->
            if (minSecs > 0) tracks.filter { it.durationSeconds >= minSecs } else tracks
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allPlaylists = repository.allPlaylists.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        selectedPlaylistTracks = combine(
            _selectedPlaylist.flatMapLatest { playlist ->
                if (playlist == null) flowOf(emptyList())
                else repository.getTracksForPlaylist(playlist.id)
            },
            minDurationFilter
        ) { tracks, minSecs ->
            if (minSecs > 0) tracks.filter { it.durationSeconds >= minSecs } else tracks
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        sortCriterion = userPrefsFlow.map { prefs ->
            try {
                TrackSortCriterion.valueOf(prefs.sortCriterion)
            } catch (e: Exception) {
                TrackSortCriterion.DATE_ADDED
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            TrackSortCriterion.DATE_ADDED
        )

        sortOrder = userPrefsFlow.map { prefs ->
            try {
                TrackSortOrder.valueOf(prefs.sortOrder)
            } catch (e: Exception) {
                TrackSortOrder.DESCENDING
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            TrackSortOrder.DESCENDING
        )

        // Observe user preferences
        viewModelScope.launch {
            repository.userPreferences.collect { prefs ->
                if (prefs != null) {
                    val theme = GlassTheme.ALL_THEMES.find { it.id == prefs.activeThemeId }
                        ?: GlassTheme.DarkPurple
                    playerManager.setTheme(theme)

                    // Restore equalizer settings
                    val preset = EqualizerPreset.PRESETS.find { !it.name.equals("Custom", ignoreCase = true) && it.name.equals(prefs.eqPresetName, ignoreCase = true) }
                    if (preset != null) {
                        playerManager.setEqualizerPreset(preset)
                    } else if (prefs.eqPresetName.equals("Custom", ignoreCase = true)) {
                        playerManager.setCustomGains(listOf(prefs.eq60Hz, prefs.eq230Hz, prefs.eq910Hz, prefs.eq3600Hz, prefs.eq14000Hz))
                    }

                    // Restore UI selections if default
                    if (_selectedCategory.value == "All" && prefs.lastSelectedCategory.isNotBlank()) {
                        _selectedCategory.value = prefs.lastSelectedCategory
                    }
                    if (_activeNavTab.value == "Home" && prefs.lastActiveNavTab.isNotBlank()) {
                        _activeNavTab.value = prefs.lastActiveNavTab
                    }
                    if (_librarySortTab.value == "Playlists" && prefs.lastLibrarySortTab.isNotBlank()) {
                        _librarySortTab.value = prefs.lastLibrarySortTab
                    }
                }
            }
        }

        // Restore playback state on startup only if no active service/player is running
        viewModelScope.launch {
            var hasRestored = false
            combine(allTracks, repository.userPreferences.filterNotNull()) { tracks, prefs ->
                Pair(tracks, prefs)
            }.collect { (tracks, prefs) ->
                if (!hasRestored && tracks.isNotEmpty() && prefs.lastPlayedTrackId != -1L) {
                    if (playerManager.isPlaying.value || playerManager.currentTrack.value != null) {
                        // Sync with active background service / player instance directly
                        hasRestored = true
                    } else {
                        val trackId = prefs.lastPlayedTrackId
                        val posMs = prefs.lastPlaybackPositionMs.toInt()
                        val queueIds = prefs.lastQueueTrackIds.split(",").mapNotNull { it.trim().toLongOrNull() }

                        val tracksMap = tracks.associateBy { it.id }
                        val restoredQueue = if (queueIds.isNotEmpty()) {
                            queueIds.mapNotNull { tracksMap[it] }.ifEmpty { tracks }
                        } else {
                            tracks
                        }
                        val trackIndex = restoredQueue.indexOfFirst { it.id == trackId }
                        if (trackIndex != -1) {
                            playerManager.restorePlaybackState(restoredQueue, trackIndex, posMs)
                            hasRestored = true
                        }
                    }
                }
            }
        }
    }

    fun setSortPreference(criterion: TrackSortCriterion, order: TrackSortOrder) {
        viewModelScope.launch {
            repository.updateSortPreferences(criterion.name, order.name)
        }
    }

    private var scanJob: Job? = null

    fun scanAndLoadLocalAudio(forceRescanAll: Boolean = false) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            val existingIds = if (forceRescanAll) emptySet() else repository.getExistingTrackIds()
            localAudioScanner.scanLocalTracksFlow(existingTrackIds = existingIds, chunkSize = 15)
                .catch { e -> e.printStackTrace() }
                .collect { chunk ->
                    if (chunk.isNotEmpty()) {
                        repository.insertLocalTracks(chunk)
                    }
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
        viewModelScope.launch {
            repository.updateCategoryPreference(category)
        }
    }

    fun setActiveNavTab(tab: String) {
        _activeNavTab.value = tab
        viewModelScope.launch {
            repository.updateActiveNavTabPreference(tab)
        }
    }

    fun setLibrarySortTab(tab: String) {
        _librarySortTab.value = tab
        viewModelScope.launch {
            repository.updateLibrarySortTabPreference(tab)
        }
    }

    fun toggleNowPlayingExpanded() {
        _isNowPlayingExpanded.value = !_isNowPlayingExpanded.value
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    // Dialog toggles
    fun setShowEqualizer(show: Boolean) { _showEqualizer.value = show }
    fun setShowSleepTimer(show: Boolean) { _showSleepTimer.value = show }
    fun setShowThemeSelector(show: Boolean) { _showThemeSelector.value = show }
    fun setShowCreatePlaylist(show: Boolean) { _showCreatePlaylist.value = show }

    fun openAddToPlaylistForTrack(track: Track?) {
        _targetTrackForPlaylist.value = track
    }

    // Player Actions
    fun playShuffleAll(currentContextList: List<Track>? = null) {
        val tracks = if (!currentContextList.isNullOrEmpty()) currentContextList else allTracks.value
        if (tracks.isEmpty()) return
        playerManager.setShuffle(true)
        val randomIndex = tracks.indices.random()
        val selectedTrack = tracks[randomIndex]
        playerManager.setQueue(tracks, randomIndex)
        viewModelScope.launch {
            repository.recordPlayed(selectedTrack.id)
        }
    }

    fun playTrack(track: Track, currentContextList: List<Track>? = null) {
        val tracks = if (!currentContextList.isNullOrEmpty()) currentContextList else allTracks.value
        val index = tracks.indexOfFirst { it.id == track.id }
        if (index != -1) {
            playerManager.setQueue(tracks, index)
        } else {
            val fallbackIndex = allTracks.value.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            playerManager.setQueue(allTracks.value, fallbackIndex)
        }
        viewModelScope.launch {
            repository.recordPlayed(track.id)
        }
    }

    fun openEditTrack(track: Track?) {
        _editingTrack.value = track
    }

    fun updateTrackInfo(trackId: Long, title: String, artist: String, album: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val track = allTracks.value.find { it.id == trackId }
            if (track != null) {
                updateAudioFileMetadata(track, title, artist, album)
            }
            repository.updateTrackInfo(trackId, title, artist, album)
            if (playerManager.currentTrack.value?.id == trackId) {
                playerManager.updateCurrentTrackInfo(title, artist, album)
            }
            _editingTrack.value = null
        }
    }

    private fun updateAudioFileMetadata(track: Track, title: String, artist: String, album: String) {
        val context = getApplication<Application>().applicationContext
        try {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Audio.Media.TITLE, title)
                put(android.provider.MediaStore.Audio.Media.ARTIST, artist)
                put(android.provider.MediaStore.Audio.Media.ALBUM, album)
            }

            if (track.audioUrl.startsWith("content://")) {
                val uri = android.net.Uri.parse(track.audioUrl)
                context.contentResolver.update(uri, values, null, null)
            } else {
                val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val updated = context.contentResolver.update(
                    uri,
                    values,
                    "${android.provider.MediaStore.Audio.Media.DATA} = ?",
                    arrayOf(track.audioUrl)
                )
                if (updated == 0) {
                    context.contentResolver.update(
                        uri,
                        values,
                        "${android.provider.MediaStore.Audio.Media._ID} = ?",
                        arrayOf(track.id.toString())
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideTrack(trackId: Long) {
        viewModelScope.launch {
            repository.hideTrack(trackId)
            playerManager.removeTrackFromQueue(trackId)
        }
    }

    fun unhideTrack(trackId: Long) {
        viewModelScope.launch {
            repository.unhideTrack(trackId)
        }
    }

    fun unhideAllTracks() {
        viewModelScope.launch {
            repository.unhideAllTracks()
        }
    }

    fun deleteTrack(trackId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val track = allTracks.value.find { it.id == trackId }
            if (track != null) {
                deleteAudioFileFromDevice(track)
            }
            repository.deleteTrack(trackId)
            playerManager.removeTrackFromQueue(trackId)
        }
    }

    private fun deleteAudioFileFromDevice(track: Track) {
        val context = getApplication<Application>().applicationContext
        try {
            if (track.audioUrl.isNotBlank() && !track.audioUrl.startsWith("content://")) {
                val file = java.io.File(track.audioUrl)
                if (file.exists()) {
                    file.delete()
                }
            }

            if (track.audioUrl.startsWith("content://")) {
                val uri = android.net.Uri.parse(track.audioUrl)
                context.contentResolver.delete(uri, null, null)
            } else {
                val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                context.contentResolver.delete(
                    uri,
                    "${android.provider.MediaStore.Audio.Media.DATA} = ? OR ${android.provider.MediaStore.Audio.Media._ID} = ?",
                    arrayOf(track.audioUrl, track.id.toString())
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val newIsFavorite = !track.isFavorite
            repository.toggleFavorite(track)
            if (playerManager.currentTrack.value?.id == track.id) {
                playerManager.updateCurrentTrackFavorite(newIsFavorite)
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    fun setSelectedPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun removeTracksFromPlaylist(playlistId: Long, trackIds: List<Long>) {
        viewModelScope.launch {
            repository.removeTracksFromPlaylist(playlistId, trackIds)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = null
            }
        }
    }

    fun playPlaylistQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        playerManager.setQueue(tracks, startIndex)
        viewModelScope.launch {
            repository.recordPlayed(tracks[startIndex].id)
        }
    }

    fun selectTheme(theme: GlassTheme, isAutoSystemTheme: Boolean = false) {
        playerManager.setTheme(theme)
        viewModelScope.launch {
            repository.updateThemePreference(theme.id, isAutoSystemTheme)
        }
    }

    fun setAutoSystemTheme(isAuto: Boolean) {
        viewModelScope.launch {
            repository.updateAutoSystemThemePreference(isAuto)
        }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        playerManager.setEqualizerPreset(preset)
        viewModelScope.launch {
            if (preset.name.equals("Custom", ignoreCase = true)) {
                repository.updateEqualizerPreferences("Custom", playerManager.eqBandGains.value)
            } else {
                repository.updateEqualizerPreferences(preset.name, preset.gains)
            }
        }
    }

    fun updateCustomEqGain(bandIndex: Int, gain: Float) {
        playerManager.updateCustomBandGain(bandIndex, gain)
        viewModelScope.launch {
            repository.updateEqualizerPreferences("Custom", playerManager.eqBandGains.value)
        }
    }

    fun setMinDurationFilter(seconds: Int) {
        viewModelScope.launch {
            repository.updateMinDurationFilter(seconds)
        }
    }

    fun setListItemSize(size: ListItemSize) {
        viewModelScope.launch {
            repository.updateListItemSize(size)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.triggerPlaybackStateSave()
    }
}
