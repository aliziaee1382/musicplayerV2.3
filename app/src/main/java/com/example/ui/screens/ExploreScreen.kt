package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GlassTheme
import com.example.model.ListItemSize
import com.example.model.Track
import com.example.ui.glass.GlassArtworkCard
import com.example.ui.glass.GlassCard
import com.example.ui.glass.GlassIconButton
import com.example.ui.glass.TrackListItem

@Composable
fun ExploreScreen(
    tracks: List<Track>,
    theme: GlassTheme,
    listItemSize: ListItemSize = ListItemSize.SMALL,
    onPlayTrack: (Track, List<Track>?) -> Unit,
    onToggleFavorite: ((Track) -> Unit)? = null,
    onOpenAddToPlaylist: ((Track) -> Unit)? = null
) {
    // Dynamic Listening Statistics calculation based on real listening time in DB
    val totalListeningSeconds = remember(tracks) { tracks.sumOf { it.effectiveListeningSeconds() } }

    val (listeningTimeDisplay, listeningTimeSubtext) = remember(totalListeningSeconds) {
        val totalHoursFloat = totalListeningSeconds / 3600f
        val totalHours = totalListeningSeconds / 3600
        val totalMins = (totalListeningSeconds % 3600) / 60

        val display = when {
            totalHours > 0 -> "%.1f hrs".format(totalHoursFloat)
            totalMins > 0 -> "%.2f hrs".format(totalHoursFloat)
            else -> "%.3f hrs".format(totalHoursFloat)
        }

        val subtext = when {
            totalHours > 0 -> "$totalHours hrs $totalMins mins"
            totalMins > 0 -> "$totalMins mins playback"
            else -> "$totalListeningSeconds sec recorded"
        }
        display to subtext
    }

    // Top Artist calculated strictly by total playback time spent on their songs
    val (topArtistName, topArtistSubtext) = remember(tracks) {
        val artistTimeMap = tracks.filter { it.artist.isNotBlank() && it.artist != "<unknown>" }
            .groupBy { it.artist }
            .mapValues { entry -> entry.value.sumOf { track -> track.effectiveListeningSeconds() } }

        val topArtistEntry = artistTimeMap.maxByOrNull { it.value }
        val name = topArtistEntry?.key ?: "Unknown Artist"
        val seconds = topArtistEntry?.value ?: 0L
        val hoursFloat = seconds / 3600f
        val subtext = when {
            seconds >= 3600 -> "%.1f hrs played".format(hoursFloat)
            seconds >= 60 -> "${seconds / 60} mins played"
            else -> "$seconds sec played"
        }
        name to subtext
    }

    // Top 10 Songs sorted by cumulative listening time and playCount
    val topSongs = remember(tracks) {
        tracks.sortedWith(
            compareByDescending<Track> { it.effectiveListeningSeconds() }
                .thenByDescending { it.playCount }
                .thenByDescending { it.lastPlayedTimestamp }
        ).take(10)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("explore_screen_column"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 180.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title & Header
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "Explore & Playback Stats",
                    color = theme.textColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Smart analysis of your listening time & top tracks",
                    color = theme.subtextColor,
                    fontSize = 13.sp
                )
            }
        }

        // 2 Equal Stat Summary Cards (Listening Time & Top Artist)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Listening Time Stat Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(theme.glassFill)
                        .border(1.dp, theme.glassBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(theme.accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = theme.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Listening Time",
                                color = theme.subtextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = listeningTimeDisplay,
                                color = theme.textColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = listeningTimeSubtext,
                                color = theme.subtextColor,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Top Artist Stat Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(theme.glassFill)
                        .border(1.dp, theme.glassBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(theme.accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = theme.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Top Artist",
                                color = theme.subtextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = topArtistName,
                                color = theme.textColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = topArtistSubtext,
                                color = theme.subtextColor,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 🔥 Top Played / Popular Songs
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top 10 Popular Songs",
                        color = theme.textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Top 10",
                        color = theme.accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topSongs.forEachIndexed { index, track ->
                        TrackListItem(
                            track = track,
                            isCurrent = false,
                            isPlaying = false,
                            listItemSize = listItemSize,
                            theme = theme,
                            rankText = "#${index + 1}",
                            showDivider = false,
                            onClick = { onPlayTrack(track, topSongs) },
                            onToggleFavorite = if (onToggleFavorite != null) { { onToggleFavorite(track) } } else null,
                            onOpenAddToPlaylist = if (onOpenAddToPlaylist != null) { { onOpenAddToPlaylist(track) } } else null,
                            testTag = "top_song_${track.id}"
                        )
                    }
                }
            }
        }
    }
}
