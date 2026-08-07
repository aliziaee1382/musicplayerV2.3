package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GlassTheme
import com.example.model.ListItemSize
import com.example.ui.glass.GlassButton
import com.example.ui.glass.GlassCard
import com.example.ui.glass.GlassSlider

@Composable
fun SettingsScreen(
    theme: GlassTheme,
    minDurationFilterSeconds: Int = 0,
    onMinDurationFilterChange: (Int) -> Unit = {},
    listItemSize: ListItemSize = ListItemSize.SMALL,
    onListItemSizeChange: (ListItemSize) -> Unit = {},
    onOpenThemeSelector: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onScanLocalMusic: (() -> Unit)? = null,
    onOpenHiddenTracks: (() -> Unit)? = null,
    hiddenCount: Int = 0
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_column"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 180.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "Settings & Appearance",
                    color = theme.textColor,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Customize themes, sound effects, equalizer & audio settings",
                    color = theme.subtextColor,
                    fontSize = 13.sp
                )
            }
        }

        // Section 1: Visual Theme & Appearance
        item {
            Text(
                text = "APPEARANCE & THEME",
                color = theme.subtextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(
                onClick = onOpenThemeSelector,
                theme = theme,
                modifier = Modifier.fillMaxWidth(),
                testTag = "settings_theme_selector_card"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(theme.accentColor, theme.glowColor)
                                    )
                                )
                                .border(1.5.dp, theme.glassBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Glass Theme Customization",
                                color = theme.textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${theme.colorNameEn} • ${if (theme.isLight) "Light Mode" else "Dark Mode"}",
                                color = theme.accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Select Theme",
                        tint = theme.subtextColor
                    )
                }
            }
        }

        // Section 1.5: Song List Display Size (Grid/List Item Density)
        item {
            Text(
                text = "SONG LIST DISPLAY SIZE",
                color = theme.subtextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(
                onClick = {},
                theme = theme,
                modifier = Modifier.fillMaxWidth(),
                testTag = "list_item_density_card"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(theme.accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = null,
                                tint = theme.accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Track Card Density",
                                color = theme.textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Current: ${listItemSize.labelEn} • Cover ${listItemSize.coverSizeDp}dp",
                                color = theme.subtextColor,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.textColor.copy(alpha = 0.08f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ListItemSize.entries.forEach { size ->
                            val isSelected = listItemSize == size
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) theme.accentColor else Color.Transparent
                                    )
                                    .clickable { onListItemSizeChange(size) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = size.labelEn,
                                    color = if (isSelected) Color.White else theme.textColor,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Equalizer & Sound Customization
        item {
            Text(
                text = "SOUND & EQUALIZER",
                color = theme.subtextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(
                onClick = onOpenEqualizer,
                theme = theme,
                modifier = Modifier.fillMaxWidth(),
                testTag = "settings_equalizer_card"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(theme.accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = theme.accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Audio Equalizer & Presets",
                                color = theme.textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Bass Boost, Treble & 5-Band EQ",
                                color = theme.subtextColor,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Equalizer",
                        tint = theme.subtextColor
                    )
                }
            }
        }

        // Section 3.5: Music Duration Filter
        item {
            Text(
                text = "MUSIC FILTERING",
                color = theme.subtextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(
                onClick = {},
                theme = theme,
                modifier = Modifier.fillMaxWidth(),
                testTag = "music_duration_filter_card"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = theme.accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Minimum Track Duration",
                                    color = theme.textColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (minDurationFilterSeconds > 0)
                                        "Filter tracks shorter than $minDurationFilterSeconds seconds"
                                    else
                                        "No duration filter applied",
                                    color = theme.subtextColor,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Text(
                            text = "${minDurationFilterSeconds}s",
                            color = theme.accentColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    GlassSlider(
                        value = minDurationFilterSeconds.toFloat(),
                        onValueChange = { newValue ->
                            onMinDurationFilterChange(newValue.toInt().coerceIn(1, 100))
                        },
                        valueRange = 1f..100f,
                        theme = theme,
                        testTag = "duration_filter_slider"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1s",
                            color = theme.subtextColor,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "100s",
                            color = theme.subtextColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Section 4: Device Storage & Scan
        if (onScanLocalMusic != null) {
            item {
                Text(
                    text = "STORAGE & LOCAL MEDIA",
                    color = theme.subtextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassCard(
                        onClick = { onScanLocalMusic.invoke() },
                        theme = theme,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_scan_storage_card"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = theme.accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Smart Auto-Scan Active",
                                        color = theme.textColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "New tracks are scanned automatically every app launch",
                                        color = theme.subtextColor,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Scan",
                                tint = theme.subtextColor
                            )
                        }
                    }

                    if (onOpenHiddenTracks != null) {
                        GlassCard(
                            onClick = { onOpenHiddenTracks.invoke() },
                            theme = theme,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "settings_hidden_tracks_card"
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = theme.accentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "Hidden Tracks",
                                            color = theme.textColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (hiddenCount > 0) "$hiddenCount track(s) hidden" else "No hidden tracks",
                                            color = theme.subtextColor,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open",
                                    tint = theme.subtextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
