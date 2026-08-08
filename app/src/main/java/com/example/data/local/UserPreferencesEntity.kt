package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val activeThemeId: String = "dark_green",
    val sortCriterion: String = "DATE_ADDED",
    val sortOrder: String = "DESCENDING",
    val lastSelectedCategory: String = "All",
    val lastActiveNavTab: String = "Home",
    val lastLibrarySortTab: String = "Playlists",
    val eqPresetName: String = "Flat",
    val eq60Hz: Float = 0f,
    val eq230Hz: Float = 0f,
    val eq910Hz: Float = 0f,
    val eq3600Hz: Float = 0f,
    val eq14000Hz: Float = 0f,
    val spatialAudioEnabled: Boolean = true,
    val flacModeEnabled: Boolean = true,
    val minDurationFilterSeconds: Int = 0,
    val lastPlayedTrackId: Long = -1L,
    val lastPlaybackPositionMs: Long = 0L,
    val lastQueueTrackIds: String = "",
    val isAutoSystemTheme: Boolean = true,
    val listItemSize: String = "SMALL"
)
