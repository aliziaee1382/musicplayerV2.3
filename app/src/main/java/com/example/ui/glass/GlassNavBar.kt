package com.example.ui.glass

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GlassTheme
import com.example.model.Track

@Composable
fun MiniPlayerBar(
    track: Track?,
    isPlaying: Boolean,
    currentPositionMs: Int,
    durationMs: Int,
    theme: GlassTheme,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpandNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (track == null) return

    val progressFloat = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(theme.glassFill)
            .border(1.dp, theme.glassBorder.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            .pointerInput(Unit) {
                detectTapGestures { }
            }
            .clickable(onClick = onExpandNowPlaying)
            .testTag("mini_player_bar")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassArtworkCard(
                    gradientIndex = track.coverGradientIndex,
                    isPlaying = isPlaying,
                    imageUrl = track.albumArtUri,
                    theme = theme,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = theme.textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = theme.accentColor,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        onClick = onTogglePlayPause,
                        isActive = true,
                        theme = theme,
                        size = 38.dp,
                        testTag = "mini_player_play_pause"
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    GlassIconButton(
                        icon = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        onClick = onNext,
                        theme = theme,
                        size = 38.dp,
                        testTag = "mini_player_next"
                    )
                }
            }

            // Slim timeline progress indicator along bottom of mini player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFloat)
                        .background(theme.accentColor)
                )
            }
        }
    }
}

data class NavItem(
    val id: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun GlassBottomNavBar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    theme: GlassTheme,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem("Home", "Home", Icons.Default.Home),
        NavItem("Explore", "Explore", Icons.Default.Explore),
        NavItem("Library", "Library", Icons.Default.QueueMusic),
        NavItem("Settings", "Settings", Icons.Default.Settings)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(theme.glassFill)
            .border(1.dp, theme.glassBorder.copy(alpha = 0.6f), RoundedCornerShape(26.dp))
            .pointerInput(Unit) {
                detectTapGestures { }
            }
            .clickable(
                enabled = true,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            )
            .padding(vertical = 8.dp, horizontal = 12.dp)
            .testTag("glass_bottom_nav_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = activeTab == item.id
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.90f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "tabScale"
                )

                val animatedBgColor by animateColorAsState(
                    targetValue = if (isSelected) theme.accentColor.copy(alpha = 0.28f) else Color.Transparent,
                    animationSpec = tween(280),
                    label = "tabBgColor"
                )

                val animatedIconTint by animateColorAsState(
                    targetValue = if (isSelected) theme.accentColor else theme.subtextColor,
                    animationSpec = tween(280),
                    label = "tabIconTint"
                )

                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(animatedBgColor)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(color = theme.accentColor),
                            onClick = { onTabSelected(item.id) }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("nav_tab_${item.id.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = animatedIconTint,
                            modifier = Modifier.size(28.8.dp)
                        )
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = expandHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(animationSpec = tween(220)),
                            exit = shrinkHorizontally(animationSpec = tween(180)) + fadeOut(animationSpec = tween(150))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.title,
                                    color = theme.textColor,
                                    fontSize = 14.4.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
