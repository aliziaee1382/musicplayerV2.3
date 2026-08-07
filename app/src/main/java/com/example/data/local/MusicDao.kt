package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Query("SELECT * FROM tracks WHERE isHidden = 0 ORDER BY title ASC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isHidden = 1 ORDER BY title ASC")
    fun getHiddenTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 AND isHidden = 0 ORDER BY title ASC")
    fun getFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE lastPlayedTimestamp > 0 AND isHidden = 0 ORDER BY lastPlayedTimestamp DESC LIMIT 20")
    fun getRecentlyPlayedTracks(): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun updateFavoriteStatus(trackId: Long, isFavorite: Boolean)

    @Query("UPDATE tracks SET isHidden = :isHidden WHERE id = :trackId")
    suspend fun updateHiddenStatus(trackId: Long, isHidden: Boolean)

    @Query("UPDATE tracks SET isHidden = 0")
    suspend fun unhideAllTracks()

    @Query("UPDATE tracks SET title = :title, artist = :artist, album = :album WHERE id = :trackId")
    suspend fun updateTrackInfo(trackId: Long, title: String, artist: String, album: String)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrack(trackId: Long)

    @Query("DELETE FROM tracks WHERE isLocal = 0 OR audioUrl LIKE '%soundhelix%'")
    suspend fun deleteDefaultSampleTracks()

    @Query("DELETE FROM playlist_track_cross_ref WHERE trackId NOT IN (SELECT id FROM tracks)")
    suspend fun deleteOrphanedCrossRefs()

    @Query("DELETE FROM playlist_track_cross_ref WHERE trackId = :trackId")
    suspend fun deleteTrackFromCrossRefs(trackId: Long)

    @Query("UPDATE tracks SET lastPlayedTimestamp = :timestamp, playCount = playCount + 1 WHERE id = :trackId")
    suspend fun recordTrackPlayed(trackId: Long, timestamp: Long)

    @Query("UPDATE tracks SET listeningSeconds = listeningSeconds + :addedSeconds WHERE id = :trackId")
    suspend fun addListeningTime(trackId: Long, addedSeconds: Long)

    @Query("""
        SELECT 
            p.id, 
            p.name, 
            CASE 
                WHEN p.id = 1 THEN (SELECT COUNT(*) FROM tracks WHERE isFavorite = 1 AND isHidden = 0)
                ELSE (SELECT COUNT(*) FROM playlist_track_cross_ref ref INNER JOIN tracks t ON ref.trackId = t.id WHERE ref.playlistId = p.id AND t.isHidden = 0)
            END AS songCount,
            p.coverGradientIndex, 
            p.isSystemPlaylist, 
            p.createdAt 
        FROM playlists p 
        ORDER BY p.isSystemPlaylist DESC, p.name ASC
    """)
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("DELETE FROM playlists WHERE id = :playlistId AND isSystemPlaylist = 0")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTracks(playlistId: Long)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId IN (:trackIds)")
    suspend fun removeTracksFromPlaylist(playlistId: Long, trackIds: List<Long>)

    @Query("UPDATE playlists SET songCount = (SELECT COUNT(*) FROM playlist_track_cross_ref WHERE playlistId = :playlistId) WHERE id = :playlistId")
    suspend fun updatePlaylistSongCount(playlistId: Long)

    @Query("SELECT t.* FROM tracks t INNER JOIN playlist_track_cross_ref ref ON t.id = ref.trackId WHERE ref.playlistId = :playlistId AND t.isHidden = 0")
    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int

    @Query("SELECT id FROM tracks")
    suspend fun getAllTrackIds(): List<Long>

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getUserPreferences(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    suspend fun getUserPreferencesDirect(): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserPreferences(prefs: UserPreferencesEntity)
}
