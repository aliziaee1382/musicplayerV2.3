package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val songCount: Int,
    val coverGradientIndex: Int,
    val isSystemPlaylist: Boolean,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toPlaylist(): Playlist = Playlist(
        id = id,
        name = name,
        songCount = songCount,
        coverGradientIndex = coverGradientIndex,
        isSystemPlaylist = isSystemPlaylist,
        createdAt = createdAt
    )

    companion object {
        fun fromPlaylist(playlist: Playlist): PlaylistEntity = PlaylistEntity(
            id = playlist.id,
            name = playlist.name,
            songCount = playlist.songCount,
            coverGradientIndex = playlist.coverGradientIndex,
            isSystemPlaylist = playlist.isSystemPlaylist,
            createdAt = playlist.createdAt
        )
    }
}
