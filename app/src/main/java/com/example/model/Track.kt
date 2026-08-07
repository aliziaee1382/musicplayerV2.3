package com.example.model

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val audioUrl: String,
    val category: String,
    val coverGradientIndex: Int = 0,
    val albumArtUri: String? = null,
    val isLocal: Boolean = false,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
    val folderName: String = "User Uploads",
    val isHidden: Boolean = false,
    val listeningSeconds: Long = 0L,
    val dateAddedTimestamp: Long = System.currentTimeMillis(),
    val dateModifiedTimestamp: Long = System.currentTimeMillis()
) {
    fun formattedDuration(): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    fun effectiveListeningSeconds(): Long {
        return listeningSeconds
    }
}

