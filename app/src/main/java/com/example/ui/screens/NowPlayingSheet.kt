package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GlassTheme
import com.example.model.Track
import com.example.player.RepeatMode
import com.example.ui.glass.*

@Composable
fun NowPlayingSheet(
    track: Track?,
    isPlaying: Boolean,
    currentPositionMs: Int,
    durationMs: Int,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    theme: GlassTheme,
    sleepTimerSeconds: Int? = null,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onCyclePlaybackMode: () -> Unit = {},
    onToggleFavorite: (Track) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenAddToPlaylist: (Track) -> Unit,
    onEditTrack: (Track) -> Unit = {},
    onHideTrack: (Long) -> Unit = {},
    onDeleteTrack: (Long) -> Unit = {},
    onCollapse: () -> Unit
) {
    if (track == null) return

    var showOptionsMenu by remember { mutableStateOf(false) }

    var isUserSeeking by remember { mutableStateOf(false) }
    var userSeekPercent by remember { mutableStateOf(0f) }

    val progressFloat = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val activeProgress = if (isUserSeeking) userSeekPercent else progressFloat

    val displayedPositionMs = if (isUserSeeking) (userSeekPercent * durationMs).toInt() else currentPositionMs

    val formattedPos = remember(displayedPositionMs) {
        val sec = (displayedPositionMs / 1000).coerceAtLeast(0)
        "%d:%02d".format(sec / 60, sec % 60)
    }

    val formattedDur = remember(durationMs) {
        val sec = (durationMs / 1000).coerceAtLeast(0)
        "%d:%02d".format(sec / 60, sec % 60)
    }

    val sleepTimerFormatted = remember(sleepTimerSeconds) {
        if (sleepTimerSeconds != null && sleepTimerSeconds > 0) {
            val hrs = sleepTimerSeconds / 3600
            val min = (sleepTimerSeconds % 3600) / 60
            val sec = sleepTimerSeconds % 60
            if (hrs > 0) {
                "%d:%02d:%02d".format(hrs, min, sec)
            } else {
                "%02d:%02d".format(min, sec)
            }
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { }
            }
            .clickable(
                enabled = true,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = theme.bgGradient
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("now_playing_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Collapse & Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    onClick = onCollapse,
                    theme = theme,
                    size = 44.dp,
                    testTag = "collapse_now_playing_button"
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM PLAYLIST",
                        color = theme.subtextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = track.album,
                        color = theme.textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box {
                    GlassIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        onClick = { showOptionsMenu = true },
                        theme = theme,
                        size = 44.dp,
                        testTag = "now_playing_options_button"
                    )

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        modifier = Modifier
                            .background(theme.glassFill)
                            .border(1.dp, theme.accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistAdd,
                                        contentDescription = null,
                                        tint = theme.textColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Add to Playlist", color = theme.textColor, fontSize = 14.sp)
                                }
                            },
                            onClick = {
                                showOptionsMenu = false
                                onOpenAddToPlaylist(track)
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = theme.textColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Edit Track Info", color = theme.textColor, fontSize = 14.sp)
                                }
                            },
                            onClick = {
                                showOptionsMenu = false
                                onEditTrack(track)
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = theme.textColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Hide Track", color = theme.textColor, fontSize = 14.sp)
                                }
                            },
                            onClick = {
                                showOptionsMenu = false
                                onHideTrack(track.id)
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = theme.textColor.copy(alpha = 0.15f)
                        )

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Delete Track", color = Color(0xFFEF4444), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = {
                                showOptionsMenu = false
                                onDeleteTrack(track.id)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Large Album Art
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                GlassArtworkCard(
                    gradientIndex = track.coverGradientIndex,
                    isPlaying = isPlaying,
                    imageUrl = track.albumArtUri,
                    theme = theme,
                    titleText = "",
                    subtitleText = "",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Track Title & Artist Info
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = track.title,
                    color = theme.textColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.artist,
                    color = theme.accentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                if (sleepTimerFormatted != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.accentColor.copy(alpha = 0.2f))
                            .border(1.dp, theme.accentColor, RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenSleepTimer)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Sleep Timer Active",
                            tint = theme.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Timer: $sleepTimerFormatted",
                            color = theme.textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val context = LocalContext.current

            // Contextual Glass Control Buttons (Share, EQ, Timer, Add To Playlist)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(theme.glassFill)
                    .border(1.dp, theme.glassBorder, RoundedCornerShape(24.dp))
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share Button (Native Android Intent)
                GlassIconButton(
                    icon = Icons.Default.Share,
                    contentDescription = "Share Track Audio File",
                    onClick = {
                        shareTrackAudioFile(context, track)
                    },
                    theme = theme,
                    size = 42.dp,
                    testTag = "now_playing_share_button"
                )

                // Equalizer (Sliders)
                GlassIconButton(
                    icon = Icons.Default.Tune,
                    contentDescription = "Equalizer",
                    onClick = onOpenEqualizer,
                    theme = theme,
                    size = 42.dp,
                    testTag = "now_playing_equalizer_button"
                )

                // Timer (Clock)
                GlassIconButton(
                    icon = Icons.Default.Timer,
                    contentDescription = "Sleep Timer",
                    onClick = onOpenSleepTimer,
                    isActive = sleepTimerFormatted != null,
                    theme = theme,
                    size = 42.dp,
                    testTag = "now_playing_timer_button"
                )

                // Add to Playlist (+)
                GlassIconButton(
                    icon = Icons.Default.PlaylistAdd,
                    contentDescription = "Add to Playlist",
                    onClick = { onOpenAddToPlaylist(track) },
                    theme = theme,
                    size = 42.dp,
                    testTag = "now_playing_add_playlist_button"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timeline Progress Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                GlassSlider(
                    value = activeProgress,
                    onValueChange = { percent ->
                        isUserSeeking = true
                        userSeekPercent = percent
                    },
                    onValueChangeFinished = {
                        onSeek(userSeekPercent * durationMs)
                        isUserSeeking = false
                    },
                    theme = theme,
                    testTag = "now_playing_progress_slider"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formattedPos,
                        color = theme.subtextColor,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formattedDur,
                        color = theme.subtextColor,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Player Controls (Playback Mode, Previous [Left], Play/Pause, Next [Right], Favorite)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Combined Playback Mode (1. Sequential -> 2. Repeat Track -> 3. Shuffle)
                val (modeIcon, modeDescription) = when {
                    isShuffle -> Pair(Icons.Default.Shuffle, "Shuffle Play")
                    repeatMode == RepeatMode.ONE -> Pair(Icons.Default.RepeatOne, "Repeat Track")
                    else -> Pair(Icons.Default.FormatListNumbered, "Sequential Play")
                }

                GlassIconButton(
                    icon = modeIcon,
                    contentDescription = modeDescription,
                    onClick = onCyclePlaybackMode,
                    isActive = false,
                    theme = theme,
                    size = 46.dp,
                    testTag = "now_playing_playback_mode_button"
                )

                // Previous Button
                GlassIconButton(
                    icon = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    onClick = onPrevious,
                    theme = theme,
                    size = 52.dp,
                    testTag = "now_playing_previous_button"
                )

                // Play / Pause (Large Center Glow Button)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(theme.accentColor, theme.glowColor)
                            )
                        )
                        .border(2.dp, Color.White, CircleShape)
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("now_playing_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next Button
                GlassIconButton(
                    icon = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    onClick = onNext,
                    theme = theme,
                    size = 52.dp,
                    testTag = "now_playing_next_button"
                )

                // Favorite Button
                GlassIconButton(
                    icon = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    onClick = { onToggleFavorite(track) },
                    isActive = track.isFavorite,
                    theme = theme,
                    size = 46.dp,
                    testTag = "now_playing_favorite_action_button"
                )
            }
        }
    }
}

private fun shareTrackAudioFile(context: Context, track: Track) {
    try {
        val fileUri: Uri? = when {
            track.audioUrl.startsWith("content://") -> {
                Uri.parse(track.audioUrl)
            }
            track.audioUrl.isNotBlank() -> {
                val cleanPath = track.audioUrl.removePrefix("file://")
                val file = File(cleanPath)
                if (file.exists()) {
                    try {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } catch (e: Exception) {
                        Uri.fromFile(file)
                    }
                } else null
            }
            else -> null
        }

        if (fileUri != null) {
            val extension = if (track.audioUrl.contains('.')) {
                track.audioUrl.substringAfterLast('.').lowercase()
            } else "mp3"

            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "audio/*"

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "${track.title} - ${track.artist}")
                putExtra(Intent.EXTRA_TEXT, "🎵 ${track.title} - ${track.artist}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, "Share Audio File")
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val resInfoList = context.packageManager.queryIntentActivities(
                chooser,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName,
                    fileUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            context.startActivity(chooser)
        } else {
            // Fallback text intent if local audio file URI is unavailable
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, track.title)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "🎵 Listening to \"${track.title}\" by \"${track.artist}\"\n💿 Album: ${track.album}"
                )
            }
            context.startActivity(Intent.createChooser(textIntent, "Share Track"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

