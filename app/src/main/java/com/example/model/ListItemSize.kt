package com.example.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ListItemSize(
    val labelEn: String,
    val coverSizeDp: Int,
    val titleSp: Int,
    val subtitleSp: Int,
    val verticalPaddingDp: Int
) {
    SMALL("Small", 52, 14, 11, 7),
    MEDIUM("Medium", 62, 16, 13, 10),
    LARGE("Large", 74, 19, 15, 13);

    val coverSize: Dp get() = coverSizeDp.dp
    val titleSize: TextUnit get() = titleSp.sp
    val subtitleSize: TextUnit get() = subtitleSp.sp
    val verticalPadding: Dp get() = verticalPaddingDp.dp

    companion object {
        fun fromName(name: String?): ListItemSize {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SMALL
        }
    }
}