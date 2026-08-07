package com.example.model

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val coverGradientIndex: Int = 0,
    val isSystemPlaylist: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
