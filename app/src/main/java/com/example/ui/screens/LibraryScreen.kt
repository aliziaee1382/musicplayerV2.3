package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.ui.text.style.TextOverflow
import com.example.model.AudioFolder
import com.example.model.GlassTheme
import com.example.model.ListItemSize
import com.example.model.Playlist
import com.example.model.Track
import com.example.ui.glass.*

@Composable
fun LibraryScreen(
    playlists: List<Playlist>,
    tracks: List<Track>,
    favoriteTracks: List<Track>,
    folders: List<AudioFolder>,
    activeSortTab: String,
    theme: GlassTheme,
    listItemSize: ListItemSize = ListItemSize.SMALL,
    onSortTabChange: (String) -> Unit,
    onOpenCreatePlaylist: () -> Unit,
    onOpenThemeSelector: () -> Unit,
    onPlayTrack: (Track, List<Track>?) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenAddToPlaylist: ((Track) -> Unit)? = null,
    onSelectPlaylist: ((Playlist) -> Unit)? = null,
    onEditPlaylist: ((Playlist) -> Unit)? = null,
    onDeletePlaylist: ((Long) -> Unit)? = null
) {
    val tabs = listOf("Playlists", "Songs", "Albums", "Artists", "Folders")
    var searchFilter by remember { mutableStateOf("") }
    var showSearchInput by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("library_screen_column")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Library",
                color = theme.textColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassIconButton(
                    icon = Icons.Default.Search,
                    contentDescription = "Search Library",
                    onClick = { showSearchInput = !showSearchInput },
                    theme = theme,
                    size = 42.dp,
                    testTag = "library_search_toggle"
                )

                // Prominent "+" to create new playlist
                GlassIconButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Create Playlist",
                    onClick = onOpenCreatePlaylist,
                    isActive = true,
                    theme = theme,
                    size = 42.dp,
                    testTag = "create_playlist_header_button"
                )
            }
        }

        // Optional search input
        AnimatedVisibility(visible = showSearchInput) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = searchFilter,
                    onValueChange = { searchFilter = it },
                    placeholder = { Text("Filter library...", color = theme.subtextColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.glassBorder,
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sorting Tabs Row (Playlists, Songs, Albums, Artists, Folders)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(tabs) { tab ->
                GlassChip(
                    text = tab,
                    isSelected = activeSortTab == tab,
                    onClick = { onSortTabChange(tab) },
                    theme = theme,
                    testTag = "library_sort_tab_$tab"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (activeSortTab) {
                "Playlists" -> PlaylistsTabContent(
                    playlists = playlists,
                    onOpenCreatePlaylist = onOpenCreatePlaylist,
                    onSelectPlaylist = onSelectPlaylist,
                    onEditPlaylist = onEditPlaylist,
                    onDeletePlaylist = onDeletePlaylist,
                    theme = theme
                )
                "Songs" -> SongsTabContent(
                    tracks = if (searchFilter.isBlank()) tracks else tracks.filter { it.title.contains(searchFilter, true) || it.artist.contains(searchFilter, true) },
                    onPlayTrack = onPlayTrack,
                    onToggleFavorite = onToggleFavorite,
                    onOpenAddToPlaylist = onOpenAddToPlaylist,
                    theme = theme,
                    listItemSize = listItemSize
                )
                "Albums" -> AlbumsTabContent(
                    tracks = tracks,
                    onPlayTrack = onPlayTrack,
                    theme = theme
                )
                "Artists" -> ArtistsTabContent(
                    tracks = tracks,
                    onPlayTrack = onPlayTrack,
                    theme = theme
                )
                "Folders" -> FoldersTabContent(
                    folders = folders,
                    theme = theme
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTabContent(
    playlists: List<Playlist>,
    onOpenCreatePlaylist: () -> Unit,
    onSelectPlaylist: ((Playlist) -> Unit)? = null,
    onEditPlaylist: ((Playlist) -> Unit)? = null,
    onDeletePlaylist: ((Long) -> Unit)? = null,
    theme: GlassTheme
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 180.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Essential "Create New Playlist" card
        item {
            GlassCard(
                onClick = onOpenCreatePlaylist,
                theme = theme,
                modifier = Modifier.fillMaxWidth(),
                testTag = "create_new_playlist_card"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.accentColor.copy(alpha = 0.25f))
                            .border(1.dp, theme.accentColor, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create New Playlist",
                            tint = theme.accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Create New Playlist",
                            color = theme.textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Build your custom glass collection",
                            color = theme.subtextColor,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        items(playlists, key = { it.id }) { playlist ->
            var showMenu by remember { mutableStateOf(false) }

            GlassCard(
                onClick = { onSelectPlaylist?.invoke(playlist) },
                theme = theme,
                modifier = Modifier.fillMaxWidth(),
                testTag = "playlist_item_${playlist.id}"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassArtworkCard(
                        gradientIndex = playlist.coverGradientIndex,
                        isPlaying = false,
                        theme = theme,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            color = theme.textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${playlist.songCount} Tracks",
                            color = theme.accentColor,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box {
                        GlassIconButton(
                            icon = Icons.Default.MoreVert,
                            contentDescription = "Playlist Options",
                            onClick = { showMenu = true },
                            theme = theme,
                            size = 36.dp,
                            testTag = "playlist_menu_btn_${playlist.id}"
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(theme.glassFill)
                                .border(1.dp, theme.accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = theme.textColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Edit Tracks", color = theme.textColor, fontSize = 14.sp)
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onEditPlaylist?.invoke(playlist)
                                }
                            )

                            if (!playlist.isSystemPlaylist) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Delete Playlist", color = Color(0xFFEF4444), fontSize = 14.sp)
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeletePlaylist?.invoke(playlist.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongsTabContent(
    tracks: List<Track>,
    onPlayTrack: (Track, List<Track>?) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenAddToPlaylist: ((Track) -> Unit)? = null,
    theme: GlassTheme,
    listItemSize: ListItemSize = ListItemSize.SMALL
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 180.dp)
    ) {
        itemsIndexed(
            items = tracks,
            key = { _, track -> track.id },
            contentType = { _, _ -> "track_item" }
        ) { index, track ->
            val isFirst = index == 0
            val isLast = index == tracks.lastIndex
            val itemShape = when {
                isFirst && isLast -> RoundedCornerShape(16.dp)
                isFirst -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                isLast -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                else -> RectangleShape
            }

            TrackListItem(
                track = track,
                isCurrent = false,
                isPlaying = false,
                listItemSize = listItemSize,
                theme = theme,
                itemShape = itemShape,
                isLastInGroup = isLast,
                showDivider = !isLast,
                onClick = { onPlayTrack(track, tracks) },
                onToggleFavorite = { onToggleFavorite(track) },
                onOpenAddToPlaylist = if (onOpenAddToPlaylist != null) { { onOpenAddToPlaylist(track) } } else null,
                testTag = "library_track_item_${track.id}"
            )
        }
    }
}

@Composable
private fun AlbumsTabContent(
    tracks: List<Track>,
    onPlayTrack: (Track, List<Track>?) -> Unit,
    theme: GlassTheme
) {
    val albums = remember(tracks) {
        tracks.groupBy { it.album }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 180.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(albums.keys.toList()) { albumName ->
            val albumTracks = albums[albumName] ?: emptyList()
            val sampleTrack = albumTracks.firstOrNull()

            GlassCard(
                onClick = { sampleTrack?.let { onPlayTrack(it, albumTracks) } },
                theme = theme,
                modifier = Modifier.fillMaxWidth(),
                testTag = "album_grid_item_$albumName"
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlassArtworkCard(
                        gradientIndex = sampleTrack?.coverGradientIndex ?: 0,
                        isPlaying = false,
                        imageUrl = sampleTrack?.albumArtUri,
                        theme = theme,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = albumName,
                        color = theme.textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${sampleTrack?.artist} • ${albumTracks.size} Tracks",
                        color = theme.subtextColor,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistsTabContent(
    tracks: List<Track>,
    onPlayTrack: (Track, List<Track>?) -> Unit,
    theme: GlassTheme
) {
    val artists = remember(tracks) {
        tracks.groupBy { it.artist }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 180.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(artists.keys.toList()) { artistName ->
            val artistTracks = artists[artistName] ?: emptyList()
            val firstTrack = artistTracks.firstOrNull()

            GlassCard(
                onClick = { firstTrack?.let { onPlayTrack(it, artistTracks) } },
                theme = theme,
                modifier = Modifier.fillMaxWidth(),
                testTag = "artist_item_$artistName"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(theme.accentColor, theme.glowColor)
                                )
                            )
                            .border(1.dp, theme.glassBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = artistName.take(1),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artistName,
                            color = theme.textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${artistTracks.size} Released Tracks",
                            color = theme.accentColor,
                            fontSize = 12.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Artist",
                        tint = theme.accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun FoldersTabContent(
    folders: List<AudioFolder>,
    theme: GlassTheme
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 180.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(folders) { folder ->
            GlassCard(
                onClick = {},
                theme = theme,
                modifier = Modifier.fillMaxWidth(),
                testTag = "folder_item_${folder.name.lowercase().replace(" ", "_")}"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.accentColor.copy(alpha = 0.2f))
                            .border(1.dp, theme.accentColor, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Folder",
                            tint = theme.accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            color = theme.textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = folder.path,
                            color = theme.subtextColor,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "${folder.songCount} Files • ${folder.totalDurationMin} min",
                            color = theme.accentColor,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
