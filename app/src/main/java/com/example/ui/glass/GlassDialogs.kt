package com.example.ui.glass

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.model.EqualizerPreset
import com.example.model.GlassTheme
import com.example.model.Playlist
import com.example.model.Track
import com.example.model.TrackSortCriterion
import com.example.model.TrackSortOrder

@Composable
fun AnimatedGlassDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateIn = true
    }

    val scale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dialogScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(220),
        label = "dialogAlpha"
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .clickable(
                    enabled = true,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                )
        ) {
            content()
        }
    }
}

@Composable
fun EqualizerDialog(
    activePreset: EqualizerPreset,
    bandGains: List<Float>,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onGainChanged: (Int, Float) -> Unit,
    onDismiss: () -> Unit,
    theme: GlassTheme
) {
    val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    AnimatedGlassDialog(onDismissRequest = onDismiss) {
        GlassBox(
            theme = theme,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("equalizer_dialog")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Equalizer",
                        tint = theme.accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "5-Band Glass Equalizer",
                        color = theme.textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                GlassIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    onClick = onDismiss,
                    theme = theme,
                    size = 36.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frequency spectrum visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, theme.glassBorder, RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = size.width / (bandGains.size * 2f)
                    bandGains.forEachIndexed { idx, gain ->
                        val normalizedHeight = ((gain + 12f) / 24f).coerceIn(0.1f, 1f)
                        val barHeight = size.height * normalizedHeight
                        val x = idx * (barWidth * 2f) + barWidth / 2f
                        val y = size.height - barHeight

                        drawRoundRect(
                            color = theme.accentColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Equalizer Presets Row
            Text(
                text = "PRESETS",
                color = theme.subtextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(EqualizerPreset.PRESETS) { preset ->
                    val isSelected = activePreset.name == preset.name
                    GlassChip(
                        text = preset.name,
                        isSelected = isSelected,
                        onClick = { onPresetSelected(preset) },
                        theme = theme,
                        testTag = "eq_preset_${preset.name.lowercase().replace(" ", "_")}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5 Gain Sliders
            bandGains.forEachIndexed { index, gain ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = bandLabels.getOrElse(index) { "Band ${index + 1}" },
                            color = theme.textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "%+.1f dB".format(gain),
                            color = theme.accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    GlassSlider(
                        value = gain,
                        onValueChange = { onGainChanged(index, it) },
                        valueRange = -12f..12f,
                        theme = theme,
                        testTag = "eq_slider_$index"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassButton(
                text = "Apply Settings",
                onClick = onDismiss,
                isHighlighted = true,
                theme = theme,
                modifier = Modifier.fillMaxWidth(),
                testTag = "apply_eq_button"
            )
        }
    }
}

@Composable
fun SleepTimerDialog(
    currentTimerSec: Int?,
    onSetTimerMinutes: (Int?) -> Unit,
    onDismiss: () -> Unit,
    theme: GlassTheme
) {
    var customMinutesText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val applyCustomMinutes = {
        val mins = customMinutesText.toIntOrNull()
        if (mins != null && mins > 0) {
            onSetTimerMinutes(mins)
            onDismiss()
        } else {
            errorMessage = "Please enter a valid number of minutes"
        }
    }

    AnimatedGlassDialog(onDismissRequest = onDismiss) {
        GlassBox(
            theme = theme,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("sleep_timer_dialog")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Sleep Timer",
                        tint = theme.accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sleep Timer",
                        color = theme.textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                GlassIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    onClick = onDismiss,
                    theme = theme,
                    size = 36.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentTimerSec != null && currentTimerSec > 0) {
                val min = currentTimerSec / 60
                val sec = currentTimerSec % 60
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.accentColor.copy(alpha = 0.2f))
                        .border(1.dp, theme.accentColor, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Timer Active",
                            color = theme.subtextColor,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "%02d:%02d".format(min, sec),
                            color = theme.accentColor,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "QUICK PRESETS",
                color = theme.subtextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 10, 20, 30, 60 minutes presets grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 20).forEach { minutes ->
                        GlassButton(
                            text = "$minutes Min",
                            onClick = {
                                onSetTimerMinutes(minutes)
                                onDismiss()
                            },
                            icon = Icons.Default.Alarm,
                            theme = theme,
                            modifier = Modifier.weight(1f),
                            testTag = "timer_preset_${minutes}_min"
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30, 60).forEach { minutes ->
                        GlassButton(
                            text = "$minutes Min",
                            onClick = {
                                onSetTimerMinutes(minutes)
                                onDismiss()
                            },
                            icon = Icons.Default.Alarm,
                            theme = theme,
                            modifier = Modifier.weight(1f),
                            testTag = "timer_preset_${minutes}_min"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "CUSTOM DURATION (MINUTES)",
                color = theme.subtextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customMinutesText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 4) {
                            customMinutesText = input
                            errorMessage = null
                        }
                    },
                    placeholder = {
                        Text(
                            text = "e.g. 45",
                            color = theme.subtextColor.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { applyCustomMinutes() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.textColor.copy(alpha = 0.2f),
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor,
                        cursorColor = theme.accentColor,
                        focusedContainerColor = theme.glassFill,
                        unfocusedContainerColor = theme.glassFill.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_timer_input")
                )

                GlassButton(
                    text = "Confirm",
                    onClick = { applyCustomMinutes() },
                    icon = Icons.Default.Check,
                    theme = theme,
                    isHighlighted = customMinutesText.isNotBlank(),
                    testTag = "custom_timer_confirm_button"
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMessage!!,
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (currentTimerSec != null && currentTimerSec > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                GlassButton(
                    text = "Turn Off Timer",
                    onClick = {
                        onSetTimerMinutes(null)
                        onDismiss()
                    },
                    icon = Icons.Default.TimerOff,
                    theme = theme,
                    isHighlighted = false,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "turn_off_timer_button"
                )
            }
        }
    }
}

@Composable
fun ThemeSelectorDialog(
    activeTheme: GlassTheme,
    isAutoSystemTheme: Boolean = true,
    onSelectTheme: (GlassTheme, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var currentIsAuto by remember(isAutoSystemTheme) { mutableStateOf(isAutoSystemTheme) }
    var selectedIsLight by remember(activeTheme) { mutableStateOf(activeTheme.isLight) }
    var selectedColorKey by remember(activeTheme) { mutableStateOf(activeTheme.colorKey.ifEmpty { "green" }) }

    AnimatedGlassDialog(onDismissRequest = onDismiss) {
        GlassBox(
            theme = activeTheme,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("theme_selector_dialog")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Theme Color",
                        tint = activeTheme.accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Theme Settings",
                        color = activeTheme.textColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                GlassIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    onClick = onDismiss,
                    theme = activeTheme,
                    size = 36.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector: Dark / Light / Auto (System)
            Text(
                text = "THEME MODE",
                color = activeTheme.subtextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Dark Mode Card
                val isDarkSelected = !currentIsAuto && !selectedIsLight
                GlassCard(
                    onClick = {
                        currentIsAuto = false
                        selectedIsLight = false
                        val newTheme = GlassTheme.getThemeForModeAndColor(false, selectedColorKey)
                        onSelectTheme(newTheme, false)
                    },
                    theme = activeTheme,
                    modifier = Modifier.weight(1f),
                    testTag = "theme_mode_dark"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isDarkSelected) activeTheme.accentColor.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = "Dark Mode",
                            tint = if (isDarkSelected) activeTheme.accentColor else activeTheme.subtextColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Dark",
                            color = activeTheme.textColor,
                            fontSize = 12.sp,
                            fontWeight = if (isDarkSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }

                // Light Mode Card
                val isLightSelected = !currentIsAuto && selectedIsLight
                GlassCard(
                    onClick = {
                        currentIsAuto = false
                        selectedIsLight = true
                        val newTheme = GlassTheme.getThemeForModeAndColor(true, selectedColorKey)
                        onSelectTheme(newTheme, false)
                    },
                    theme = activeTheme,
                    modifier = Modifier.weight(1f),
                    testTag = "theme_mode_light"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isLightSelected) activeTheme.accentColor.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LightMode,
                            contentDescription = "Light Mode",
                            tint = if (isLightSelected) activeTheme.accentColor else activeTheme.subtextColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Light",
                            color = activeTheme.textColor,
                            fontSize = 12.sp,
                            fontWeight = if (isLightSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }

                // Auto (System) Mode Card
                val isAutoSelected = currentIsAuto
                GlassCard(
                    onClick = {
                        currentIsAuto = true
                        val newTheme = GlassTheme.getThemeForModeAndColor(selectedIsLight, selectedColorKey)
                        onSelectTheme(newTheme, true)
                    },
                    theme = activeTheme,
                    modifier = Modifier.weight(1f),
                    testTag = "theme_mode_auto"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isAutoSelected) activeTheme.accentColor.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsSuggest,
                            contentDescription = "Auto System Theme",
                            tint = if (isAutoSelected) activeTheme.accentColor else activeTheme.subtextColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Auto",
                            color = activeTheme.textColor,
                            fontSize = 12.sp,
                            fontWeight = if (isAutoSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Accent Color Selector (6 colors)
            Text(
                text = "ACCENT COLOR",
                color = activeTheme.subtextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassTheme.COLOR_OPTIONS.chunked(2).forEach { colorRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        colorRow.forEach { colorOpt ->
                            val isColorSelected = selectedColorKey == colorOpt.key
                            GlassCard(
                                onClick = {
                                    selectedColorKey = colorOpt.key
                                    val newTheme = GlassTheme.getThemeForModeAndColor(selectedIsLight, colorOpt.key)
                                    onSelectTheme(newTheme, currentIsAuto)
                                },
                                theme = activeTheme,
                                modifier = Modifier.weight(1f),
                                testTag = "color_opt_${colorOpt.key}"
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isColorSelected) colorOpt.color.copy(alpha = 0.25f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = if (isColorSelected) 1.5.dp else 0.dp,
                                            color = if (isColorSelected) colorOpt.color else Color.Transparent,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(colorOpt.color)
                                            .border(1.dp, activeTheme.glassBorder, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = colorOpt.nameEn,
                                            color = activeTheme.textColor,
                                            fontSize = 14.sp,
                                            fontWeight = if (isColorSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = colorOpt.nameEn,
                                            color = activeTheme.subtextColor,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isColorSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = colorOpt.color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassButton(
                text = "Confirm & Apply",
                onClick = onDismiss,
                isHighlighted = true,
                theme = activeTheme,
                modifier = Modifier.fillMaxWidth(),
                testTag = "confirm_theme_button"
            )
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
    theme: GlassTheme
) {
    var playlistName by remember { mutableStateOf("") }

    AnimatedGlassDialog(onDismissRequest = onDismiss) {
        GlassBox(
            theme = theme,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("create_playlist_dialog")
        ) {
            Text(
                text = "Create New Playlist",
                color = theme.textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                placeholder = { Text("Playlist Name", color = theme.subtextColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accentColor,
                    unfocusedBorderColor = theme.glassBorder,
                    focusedTextColor = theme.textColor,
                    unfocusedTextColor = theme.textColor
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playlist_name_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = theme.subtextColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
                GlassButton(
                    text = "Create",
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            onCreate(playlistName.trim())
                            onDismiss()
                        }
                    },
                    isHighlighted = true,
                    theme = theme,
                    testTag = "submit_create_playlist_button"
                )
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    track: Track,
    onAddToPlaylist: (Long) -> Unit,
    onCreateNewPlaylist: () -> Unit,
    onDismiss: () -> Unit,
    theme: GlassTheme
) {
    AnimatedGlassDialog(onDismissRequest = onDismiss) {
        GlassBox(
            theme = theme,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_to_playlist_dialog")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add to Playlist",
                    color = theme.textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                GlassIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    onClick = onDismiss,
                    theme = theme,
                    size = 32.dp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${track.title} • ${track.artist}",
                color = theme.accentColor,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassButton(
                text = "New Playlist",
                icon = Icons.Default.Add,
                onClick = {
                    onDismiss()
                    onCreateNewPlaylist()
                },
                isHighlighted = true,
                theme = theme,
                modifier = Modifier.fillMaxWidth(),
                testTag = "new_playlist_in_add_dialog"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                playlists.forEach { playlist ->
                    GlassCard(
                        onClick = {
                            onAddToPlaylist(playlist.id)
                            onDismiss()
                        },
                        theme = theme,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "select_playlist_${playlist.id}"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = theme.accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = playlist.name,
                                    color = theme.textColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${playlist.songCount} songs",
                                    color = theme.subtextColor,
                                    fontSize = 12.sp
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
fun PlaylistDetailsDialog(
    playlist: Playlist,
    tracks: List<Track>,
    initialEditMode: Boolean = false,
    onPlayTrack: (Track, List<Track>?) -> Unit,
    onPlayAll: () -> Unit,
    onRemoveTrack: (Long) -> Unit,
    onRemoveTracks: (List<Long>) -> Unit = {},
    onDeletePlaylist: ((Long) -> Unit)? = null,
    onToggleFavorite: (Track) -> Unit,
    onDismiss: () -> Unit,
    theme: GlassTheme
) {
    var isEditMode by remember { mutableStateOf(initialEditMode) }
    val selectedTrackIds = remember { mutableStateListOf<Long>() }
    var showMenu by remember { mutableStateOf(false) }

    AnimatedGlassDialog(onDismissRequest = onDismiss) {
        GlassBox(
            theme = theme,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .padding(8.dp)
                .testTag("playlist_details_dialog")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    GlassArtworkCard(
                        gradientIndex = playlist.coverGradientIndex,
                        isPlaying = false,
                        theme = theme,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            color = theme.textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isEditMode) "Editing (${selectedTrackIds.size} selected)" else "${tracks.size} Tracks",
                            color = theme.accentColor,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        GlassIconButton(
                            icon = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            onClick = { showMenu = true },
                            theme = theme,
                            size = 36.dp,
                            testTag = "playlist_details_menu_btn"
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(theme.glassFill)
                                .border(1.dp, theme.accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        ) {
                            if (tracks.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = theme.textColor, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(if (isEditMode) "Exit Edit Mode" else "Edit Tracks", color = theme.textColor, fontSize = 14.sp)
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        isEditMode = !isEditMode
                                        if (!isEditMode) selectedTrackIds.clear()
                                    }
                                )
                            }

                            if (!playlist.isSystemPlaylist) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Delete Playlist", color = Color(0xFFEF4444), fontSize = 14.sp)
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeletePlaylist?.invoke(playlist.id)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    GlassIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close",
                        onClick = onDismiss,
                        theme = theme,
                        size = 36.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isEditMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (selectedTrackIds.size == tracks.size) {
                                selectedTrackIds.clear()
                            } else {
                                selectedTrackIds.clear()
                                selectedTrackIds.addAll(tracks.map { it.id })
                            }
                        }
                    ) {
                        Text(
                            text = if (selectedTrackIds.size == tracks.size) "Deselect All" else "Select All",
                            color = theme.accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        if (selectedTrackIds.isNotEmpty()) {
                            GlassButton(
                                text = "Delete (${selectedTrackIds.size})",
                                icon = Icons.Default.Delete,
                                onClick = {
                                    onRemoveTracks(selectedTrackIds.toList())
                                    selectedTrackIds.clear()
                                    isEditMode = false
                                },
                                isHighlighted = true,
                                theme = theme,
                                testTag = "delete_selected_tracks_btn"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        GlassButton(
                            text = "Done",
                            onClick = {
                                isEditMode = false
                                selectedTrackIds.clear()
                            },
                            theme = theme,
                            testTag = "finish_edit_tracks_btn"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else if (tracks.isNotEmpty()) {
                GlassButton(
                    text = "Play All (${tracks.size})",
                    icon = Icons.Default.PlayArrow,
                    onClick = {
                        onPlayAll()
                        onDismiss()
                    },
                    isHighlighted = true,
                    theme = theme,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "playlist_play_all_btn"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No songs in this playlist.",
                        color = theme.subtextColor,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    itemsIndexed(
                        items = tracks,
                        key = { _, track -> track.id }
                    ) { index, track ->
                        val isFirst = index == 0
                        val isLast = index == tracks.lastIndex
                        val isSelected = track.id in selectedTrackIds
                        val itemShape = when {
                            isFirst && isLast -> RoundedCornerShape(12.dp)
                            isFirst -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                            isLast -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                            else -> RectangleShape
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(itemShape)
                                    .background(if (isSelected) theme.accentColor.copy(alpha = 0.2f) else theme.glassFill)
                                    .clickable {
                                        if (isEditMode) {
                                            if (isSelected) selectedTrackIds.remove(track.id)
                                            else selectedTrackIds.add(track.id)
                                        } else {
                                            onPlayTrack(track, tracks)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isEditMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                if (checked) selectedTrackIds.add(track.id)
                                                else selectedTrackIds.remove(track.id)
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = theme.accentColor,
                                                uncheckedColor = theme.subtextColor
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    GlassArtworkCard(
                                        gradientIndex = track.coverGradientIndex,
                                        isPlaying = false,
                                        imageUrl = track.albumArtUri,
                                        theme = theme,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            color = theme.textColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.artist,
                                            color = theme.subtextColor,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (!isEditMode) {
                                        GlassIconButton(
                                            icon = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            onClick = { onToggleFavorite(track) },
                                            tint = if (track.isFavorite) Color(0xFFEF4444) else theme.subtextColor,
                                            theme = theme,
                                            size = 30.dp
                                        )

                                        if (!playlist.isSystemPlaylist) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            GlassIconButton(
                                                icon = Icons.Default.DeleteOutline,
                                                contentDescription = "Remove from Playlist",
                                                onClick = { onRemoveTrack(track.id) },
                                                tint = theme.subtextColor,
                                                theme = theme,
                                                size = 30.dp
                                            )
                                        }
                                    }
                                }
                            }

                            if (!isLast) {
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    thickness = 0.5.dp,
                                    color = theme.textColor.copy(alpha = 0.12f)
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
fun SortTracksDialog(
    selectedCriterion: TrackSortCriterion,
    selectedOrder: TrackSortOrder,
    onSelectCriterion: (TrackSortCriterion) -> Unit,
    onSelectOrder: (TrackSortOrder) -> Unit,
    onDismiss: () -> Unit,
    theme: GlassTheme
) {
    AnimatedGlassDialog(onDismissRequest = onDismiss) {
        GlassBox(
            theme = theme,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("sort_tracks_dialog")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        tint = theme.accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sort Songs",
                        color = theme.textColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                GlassIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    onClick = onDismiss,
                    theme = theme,
                    size = 36.dp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section 1: Criterion
            Text(
                text = "SORT BY",
                color = theme.subtextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TrackSortCriterion.values().forEach { criterion ->
                    val isSelected = selectedCriterion == criterion
                    GlassCard(
                        onClick = { onSelectCriterion(criterion) },
                        theme = theme,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "sort_criterion_${criterion.name}"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) theme.accentColor.copy(alpha = 0.25f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = criterion.labelEn,
                                    color = theme.textColor,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = theme.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Order (Ascending / Descending)
            Text(
                text = "SORT ORDER",
                color = theme.subtextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrackSortOrder.values().forEach { order ->
                    val isSelected = selectedOrder == order
                    GlassCard(
                        onClick = { onSelectOrder(order) },
                        theme = theme,
                        modifier = Modifier.weight(1f),
                        testTag = "sort_order_${order.name}"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) theme.accentColor.copy(alpha = 0.25f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (order == TrackSortOrder.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = order.labelEn,
                                tint = if (isSelected) theme.accentColor else theme.subtextColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = order.labelEn,
                                color = theme.textColor,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            GlassButton(
                text = "Confirm & Apply",
                onClick = onDismiss,
                isHighlighted = true,
                theme = theme,
                modifier = Modifier.fillMaxWidth(),
                testTag = "confirm_sort_button"
            )
        }
    }
}

@Composable
fun SearchTracksDialog(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onPlayTrack: (Track, List<Track>?) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onDismiss: () -> Unit,
    theme: GlassTheme
) {
    AnimatedGlassDialog(onDismissRequest = onDismiss) {
        GlassBox(
            theme = theme,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with title & close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = theme.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Search Songs & Artists",
                            color = theme.textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    GlassIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close Search",
                        onClick = onDismiss,
                        theme = theme,
                        size = 36.dp,
                        testTag = "close_search_dialog_button"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            "Search by title, artist, or album...",
                            color = theme.subtextColor,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = theme.accentColor
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = theme.subtextColor
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.glassBorder,
                        focusedContainerColor = theme.glassFill,
                        unfocusedContainerColor = theme.glassFill,
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("popup_search_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                val filteredResults = remember(tracks, searchQuery) {
                    if (searchQuery.isBlank()) {
                        tracks
                    } else {
                        tracks.filter { track ->
                            track.title.contains(searchQuery, ignoreCase = true) ||
                                    track.artist.contains(searchQuery, ignoreCase = true) ||
                                    track.album.contains(searchQuery, ignoreCase = true) ||
                                    track.folderName.contains(searchQuery, ignoreCase = true)
                        }
                    }
                }

                Text(
                    text = if (searchQuery.isBlank()) "All Songs (${filteredResults.size})" else "Found (${filteredResults.size})",
                    color = theme.subtextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = theme.subtextColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No songs or artists found",
                                color = theme.subtextColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 10.dp)
                    ) {
                        itemsIndexed(
                            items = filteredResults,
                            key = { _, track -> track.id }
                        ) { index, track ->
                            val isCurrent = currentTrack?.id == track.id
                            val isFirst = index == 0
                            val isLast = index == filteredResults.lastIndex
                            val itemShape = when {
                                isFirst && isLast -> RoundedCornerShape(12.dp)
                                isFirst -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                                isLast -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                                else -> RectangleShape
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(itemShape)
                                        .background(theme.glassFill)
                                        .clickable {
                                            onPlayTrack(track, filteredResults)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        GlassArtworkCard(
                                            gradientIndex = track.coverGradientIndex,
                                            isPlaying = isCurrent && isPlaying,
                                            imageUrl = track.albumArtUri,
                                            theme = theme,
                                            modifier = Modifier.size(42.dp)
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.title,
                                                color = if (isCurrent) theme.accentColor else theme.textColor,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${track.artist} • ${track.album}",
                                                color = theme.subtextColor,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = track.formattedDuration(),
                                            color = theme.subtextColor,
                                            fontSize = 11.sp
                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        GlassIconButton(
                                            icon = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            onClick = { onToggleFavorite(track) },
                                            isActive = track.isFavorite,
                                            theme = theme,
                                            size = 30.dp,
                                            testTag = "search_fav_button_${track.id}"
                                        )
                                    }
                                }

                                if (!isLast) {
                                    HorizontalDivider(
                                        modifier = Modifier.fillMaxWidth(),
                                        thickness = 0.5.dp,
                                        color = theme.textColor.copy(alpha = 0.12f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditTrackInfoDialog(
    track: Track,
    onSave: (title: String, artist: String, album: String) -> Unit,
    onDismiss: () -> Unit,
    theme: GlassTheme
) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    var album by remember { mutableStateOf(track.album) }

    AnimatedGlassDialog(onDismissRequest = onDismiss) {
        GlassBox(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            theme = theme,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = theme.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Edit Track",
                            color = theme.textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    GlassIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close",
                        onClick = onDismiss,
                        theme = theme,
                        size = 32.dp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = theme.subtextColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor,
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.textColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_track_title_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist", color = theme.subtextColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor,
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.textColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_track_artist_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album", color = theme.subtextColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor,
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.textColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_track_album_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        theme = theme,
                        modifier = Modifier.weight(1f)
                    )

                    GlassButton(
                        text = "Save",
                        onClick = {
                            if (title.isNotBlank() && artist.isNotBlank()) {
                                onSave(title.trim(), artist.trim(), album.trim().ifEmpty { "Local Album" })
                            }
                        },
                        isHighlighted = true,
                        theme = theme,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun HiddenTracksDialog(
    hiddenTracks: List<Track>,
    onUnhideTrack: (Long) -> Unit,
    onUnhideAll: () -> Unit,
    onDismiss: () -> Unit,
    theme: GlassTheme
) {
    AnimatedGlassDialog(onDismissRequest = onDismiss) {
        GlassBox(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f),
            theme = theme,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = theme.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Hidden Tracks",
                            color = theme.textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    GlassIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close",
                        onClick = onDismiss,
                        theme = theme,
                        size = 32.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (hiddenTracks.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${hiddenTracks.size} track(s) hidden",
                            color = theme.subtextColor,
                            fontSize = 13.sp
                        )

                        GlassButton(
                            text = "Unhide All",
                            onClick = onUnhideAll,
                            theme = theme,
                            modifier = Modifier.height(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(hiddenTracks, key = { it.id }) { track ->
                            GlassCard(
                                onClick = {},
                                theme = theme,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Song Cover Artwork
                                    GlassArtworkCard(
                                        gradientIndex = track.coverGradientIndex,
                                        isPlaying = false,
                                        imageUrl = track.albumArtUri,
                                        theme = theme,
                                        titleText = track.title,
                                        modifier = Modifier.size(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // 2. Track Title & Artist Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            color = theme.textColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = track.artist,
                                                color = theme.subtextColor,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Text(
                                                text = " • ${track.formattedDuration()}",
                                                color = theme.subtextColor.copy(alpha = 0.7f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // 3. Unhide / Restore Icon Button
                                    IconButton(
                                        onClick = { onUnhideTrack(track.id) },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(theme.accentColor.copy(alpha = 0.15f))
                                            .border(1.dp, theme.accentColor.copy(alpha = 0.35f), CircleShape)
                                            .size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "Unhide track",
                                            tint = theme.accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = theme.subtextColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No hidden tracks",
                                color = theme.subtextColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


