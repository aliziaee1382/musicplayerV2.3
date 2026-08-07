package com.example.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GlassTheme
import com.example.model.ListItemSize
import com.example.model.Track

@Composable
fun TrackListItem(
    track: Track,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    listItemSize: ListItemSize = ListItemSize.SMALL,
    theme: GlassTheme,
    itemShape: Shape = RoundedCornerShape(12.dp),
    isLastInGroup: Boolean = false,
    showDivider: Boolean = true,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    onOpenAddToPlaylist: (() -> Unit)? = null,
    rankText: String? = null,
    testTag: String = "track_list_item_${track.id}"
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(itemShape)
                .background(theme.glassFill)
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = listItemSize.verticalPadding)
                .testTag(testTag)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (rankText != null) {
                    Text(
                        text = rankText,
                        color = if (rankText == "#1") theme.accentColor else theme.subtextColor,
                        fontSize = (listItemSize.titleSp - 1).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp)
                    )
                }

                GlassArtworkCard(
                    gradientIndex = track.coverGradientIndex,
                    isPlaying = isCurrent && isPlaying,
                    imageUrl = track.albumArtUri,
                    theme = theme,
                    modifier = Modifier.size(listItemSize.coverSize)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = if (isCurrent) theme.accentColor else theme.textColor,
                        fontSize = listItemSize.titleSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${track.artist} • ${track.album}",
                        color = theme.subtextColor,
                        fontSize = listItemSize.subtitleSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = track.formattedDuration(),
                    color = theme.subtextColor,
                    fontSize = listItemSize.subtitleSize
                )

                Spacer(modifier = Modifier.width(6.dp))

                if (onOpenAddToPlaylist != null) {
                    GlassIconButton(
                        icon = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to Playlist",
                        onClick = onOpenAddToPlaylist,
                        theme = theme,
                        size = (listItemSize.coverSizeDp * 0.58f).dp.coerceAtLeast(32.dp),
                        testTag = "add_playlist_button_${track.id}"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (onToggleFavorite != null) {
                    GlassIconButton(
                        icon = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        onClick = onToggleFavorite,
                        isActive = track.isFavorite,
                        tint = if (track.isFavorite) Color(0xFFEF4444) else theme.subtextColor,
                        theme = theme,
                        size = (listItemSize.coverSizeDp * 0.58f).dp.coerceAtLeast(32.dp),
                        testTag = "favorite_button_${track.id}"
                    )
                }
            }
        }

        if (showDivider && !isLastInGroup) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = theme.textColor.copy(alpha = 0.15f)
            )
        }
    }
}
