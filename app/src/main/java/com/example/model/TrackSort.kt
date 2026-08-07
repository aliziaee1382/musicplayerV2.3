package com.example.model

enum class TrackSortCriterion(val labelFa: String, val labelEn: String) {
    DATE_ADDED("Date Added", "Date Added"),
    FILE_DATE("Track Date", "Track Date"),
    TITLE("Title", "Title"),
    DURATION("Duration", "Duration")
}

enum class TrackSortOrder(val labelFa: String, val labelEn: String) {
    ASCENDING("Ascending ↑", "Ascending ↑"),
    DESCENDING("Descending ↓", "Descending ↓")
}
