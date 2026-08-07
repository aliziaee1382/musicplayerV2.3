package com.example.data.local

import com.example.model.AudioFolder
import com.example.model.Playlist
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(private val dao: MusicDao) {

    val allTracks: Flow<List<Track>> = dao.getAllTracks().map { entities ->
        entities.map { it.toTrack() }
    }

    val hiddenTracks: Flow<List<Track>> = dao.getHiddenTracks().map { entities ->
        entities.map { it.toTrack() }
    }

    val favoriteTracks: Flow<List<Track>> = dao.getFavoriteTracks().map { entities ->
        entities.map { it.toTrack() }
    }

    val recentlyPlayed: Flow<List<Track>> = dao.getRecentlyPlayedTracks().map { entities ->
        entities.map { it.toTrack() }
    }

    val allPlaylists: Flow<List<Playlist>> = dao.getAllPlaylists().map { entities ->
        entities.map { it.toPlaylist() }
    }

    val userPreferences: Flow<UserPreferencesEntity?> = dao.getUserPreferences()

    suspend fun toggleFavorite(track: Track) = withContext(Dispatchers.IO) {
        dao.updateFavoriteStatus(track.id, !track.isFavorite)
    }

    suspend fun hideTrack(trackId: Long) = withContext(Dispatchers.IO) {
        dao.updateHiddenStatus(trackId, true)
    }

    suspend fun unhideTrack(trackId: Long) = withContext(Dispatchers.IO) {
        dao.updateHiddenStatus(trackId, false)
    }

    suspend fun unhideAllTracks() = withContext(Dispatchers.IO) {
        dao.unhideAllTracks()
    }

    suspend fun updateTrackInfo(trackId: Long, title: String, artist: String, album: String) = withContext(Dispatchers.IO) {
        dao.updateTrackInfo(trackId, title, artist, album)
    }

    suspend fun deleteTrack(trackId: Long) = withContext(Dispatchers.IO) {
        dao.deleteTrackFromCrossRefs(trackId)
        dao.deleteTrack(trackId)
    }

    suspend fun recordPlayed(trackId: Long) = withContext(Dispatchers.IO) {
        dao.recordTrackPlayed(trackId, System.currentTimeMillis())
    }

    suspend fun addListeningTime(trackId: Long, seconds: Long) = withContext(Dispatchers.IO) {
        dao.addListeningTime(trackId, seconds)
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        val newPlaylist = PlaylistEntity(
            name = name,
            songCount = 0,
            coverGradientIndex = (name.hashCode() % 5).let { if (it < 0) -it else it },
            isSystemPlaylist = false
        )
        dao.insertPlaylist(newPlaylist)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        dao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId, trackId))
        dao.updatePlaylistSongCount(playlistId)
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        dao.removeTrackFromPlaylist(playlistId, trackId)
        dao.updatePlaylistSongCount(playlistId)
    }

    suspend fun removeTracksFromPlaylist(playlistId: Long, trackIds: List<Long>) = withContext(Dispatchers.IO) {
        if (trackIds.isNotEmpty()) {
            dao.removeTracksFromPlaylist(playlistId, trackIds)
            dao.updatePlaylistSongCount(playlistId)
        }
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        dao.deletePlaylistTracks(playlistId)
        dao.deletePlaylist(playlistId)
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        return if (playlistId == 1L) {
            favoriteTracks
        } else {
            dao.getTracksForPlaylist(playlistId).map { entities -> entities.map { it.toTrack() } }
        }
    }

    suspend fun savePreferences(prefs: UserPreferencesEntity) {
        dao.saveUserPreferences(prefs)
    }

    suspend fun updateThemePreference(themeId: String, isAutoSystemTheme: Boolean = false) {
        val current = dao.getUserPreferencesDirect() ?: UserPreferencesEntity()
        dao.saveUserPreferences(current.copy(activeThemeId = themeId, isAutoSystemTheme = isAutoSystemTheme))
    }

    suspend fun updateAutoSystemThemePreference(isAuto: Boolean) {
        val current = dao.getUserPreferencesDirect() ?: UserPreferencesEntity()
        dao.saveUserPreferences(current.copy(isAutoSystemTheme = isAuto))
    }

    suspend fun updateSortPreferences(criterion: String, order: String) {
        val current = dao.getUserPreferencesDirect() ?: UserPreferencesEntity()
        dao.saveUserPreferences(current.copy(sortCriterion = criterion, sortOrder = order))
    }

    suspend fun updateCategoryPreference(category: String) {
        val current = dao.getUserPreferencesDirect() ?: UserPreferencesEntity()
        dao.saveUserPreferences(current.copy(lastSelectedCategory = category))
    }

    suspend fun updateActiveNavTabPreference(tab: String) {
        val current = dao.getUserPreferencesDirect() ?: UserPreferencesEntity()
        dao.saveUserPreferences(current.copy(lastActiveNavTab = tab))
    }

    suspend fun updateLibrarySortTabPreference(tab: String) {
        val current = dao.getUserPreferencesDirect() ?: UserPreferencesEntity()
        dao.saveUserPreferences(current.copy(lastLibrarySortTab = tab))
    }

    suspend fun updateEqualizerPreferences(presetName: String, gains: List<Float>) {
        val current = dao.getUserPreferencesDirect() ?: UserPreferencesEntity()
        val g0 = gains.getOrElse(0) { 0f }
        val g1 = gains.getOrElse(1) { 0f }
        val g2 = gains.getOrElse(2) { 0f }
        val g3 = gains.getOrElse(3) { 0f }
        val g4 = gains.getOrElse(4) { 0f }
        dao.saveUserPreferences(
            current.copy(
                eqPresetName = presetName,
                eq60Hz = g0,
                eq230Hz = g1,
                eq910Hz = g2,
                eq3600Hz = g3,
                eq14000Hz = g4
            )
        )
    }

    suspend fun updateMinDurationFilter(minSeconds: Int) {
        val current = dao.getUserPreferencesDirect() ?: UserPreferencesEntity()
        dao.saveUserPreferences(current.copy(minDurationFilterSeconds = minSeconds))
    }

    suspend fun updateListItemSize(size: com.example.model.ListItemSize) {
        val current = dao.getUserPreferencesDirect() ?: UserPreferencesEntity()
        dao.saveUserPreferences(current.copy(listItemSize = size.name))
    }

    suspend fun updatePlaybackState(trackId: Long, positionMs: Long, queueTrackIds: String) = withContext(Dispatchers.IO) {
        val current = dao.getUserPreferencesDirect() ?: UserPreferencesEntity()
        dao.saveUserPreferences(
            current.copy(
                lastPlayedTrackId = trackId,
                lastPlaybackPositionMs = positionMs,
                lastQueueTrackIds = queueTrackIds
            )
        )
    }

    suspend fun insertLocalTracks(tracks: List<Track>) = withContext(Dispatchers.IO) {
        if (tracks.isNotEmpty()) {
            dao.insertTracks(tracks.map { TrackEntity.fromTrack(it) })
        }
    }

    suspend fun getExistingTrackIds(): Set<Long> = withContext(Dispatchers.IO) {
        dao.getAllTrackIds().toSet()
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        // Ensure only Favorites system playlist is installed by default
        dao.insertPlaylist(
            PlaylistEntity(id = 1, name = "Favorites", songCount = 0, coverGradientIndex = 0, isSystemPlaylist = true)
        )
        // Cleanup old sample non-system playlists if present from previous builds
        dao.deletePlaylist(2)
        dao.deletePlaylist(3)
        dao.deletePlaylist(4)
        dao.deletePlaylistTracks(2)
        dao.deletePlaylistTracks(3)
        dao.deletePlaylistTracks(4)

        // Remove default sample tracks
        dao.deleteDefaultSampleTracks()
        dao.deleteOrphanedCrossRefs()

        // Initialize default user preferences only if none exist yet
        if (dao.getUserPreferencesDirect() == null) {
            dao.saveUserPreferences(UserPreferencesEntity())
        }
    }

    fun getSampleFolders(): List<AudioFolder> {
        return listOf(
            AudioFolder(name = "Music Download", path = "/storage/emulated/0/Download/Music", songCount = 48, totalDurationMin = 186),
            AudioFolder(name = "WhatsApp Audio", path = "/storage/emulated/0/WhatsApp/Media/Audio", songCount = 12, totalDurationMin = 42),
            AudioFolder(name = "User Uploads", path = "/storage/emulated/0/Music/Uploads", songCount = 24, totalDurationMin = 98),
            AudioFolder(name = "Studio Renders", path = "/storage/emulated/0/Music/Studio", songCount = 8, totalDurationMin = 36),
            AudioFolder(name = "Podcasts", path = "/storage/emulated/0/Podcasts", songCount = 15, totalDurationMin = 420)
        )
    }
}
