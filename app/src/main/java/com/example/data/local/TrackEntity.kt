package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val audioUrl: String,
    val category: String,
    val coverGradientIndex: Int,
    val albumArtUri: String? = null,
    val isLocal: Boolean = false,
    val isFavorite: Boolean,
    val playCount: Int,
    val lastPlayedTimestamp: Long,
    val folderName: String,
    val isHidden: Boolean = false,
    val listeningSeconds: Long = 0L,
    val dateAddedTimestamp: Long = 0L,
    val dateModifiedTimestamp: Long = 0L
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationSeconds = durationSeconds,
        audioUrl = audioUrl,
        category = category,
        coverGradientIndex = coverGradientIndex,
        albumArtUri = albumArtUri,
        isLocal = isLocal,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedTimestamp = lastPlayedTimestamp,
        folderName = folderName,
        isHidden = isHidden,
        listeningSeconds = listeningSeconds,
        dateAddedTimestamp = if (dateAddedTimestamp > 0) dateAddedTimestamp else System.currentTimeMillis(),
        dateModifiedTimestamp = if (dateModifiedTimestamp > 0) dateModifiedTimestamp else System.currentTimeMillis()
    )

    companion object {
        fun fromTrack(track: Track): TrackEntity = TrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationSeconds = track.durationSeconds,
            audioUrl = track.audioUrl,
            category = track.category,
            coverGradientIndex = track.coverGradientIndex,
            albumArtUri = track.albumArtUri,
            isLocal = track.isLocal,
            isFavorite = track.isFavorite,
            playCount = track.playCount,
            lastPlayedTimestamp = track.lastPlayedTimestamp,
            folderName = track.folderName,
            isHidden = track.isHidden,
            listeningSeconds = track.listeningSeconds,
            dateAddedTimestamp = track.dateAddedTimestamp,
            dateModifiedTimestamp = track.dateModifiedTimestamp
        )
    }
}
