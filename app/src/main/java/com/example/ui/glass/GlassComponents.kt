package com.example.ui.glass

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.model.GlassTheme

@Composable
fun GlassBackgroundContainer(
    theme: GlassTheme,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = theme.bgGradient
                )
            )
    ) {
        // Fast, GPU-accelerated ambient background glow orbs
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(theme.glowColor.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(width * 0.25f, height * 0.25f),
                    radius = width * 0.6f
                ),
                radius = width * 0.6f,
                center = Offset(width * 0.25f, height * 0.25f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(theme.accentColor.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(width * 0.75f, height * 0.70f),
                    radius = width * 0.7f
                ),
                radius = width * 0.7f,
                center = Offset(width * 0.75f, height * 0.70f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(theme.glowColor.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(width * 0.5f, height * 0.90f),
                    radius = width * 0.5f
                ),
                radius = width * 0.5f,
                center = Offset(width * 0.5f, height * 0.90f)
            )
        }

        // Content on top of ambient glass canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            content()
        }
    }
}

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    theme: GlassTheme = GlassTheme.DarkGreen,
    shape: Shape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(theme.glassFill)
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        theme.glassBorder,
                        Color.White.copy(alpha = 0.05f),
                        theme.glassBorder.copy(alpha = 0.3f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(400f, 400f)
                ),
                shape = shape
            )
            .padding(contentPadding)
    ) {
        content()
    }
}

private val DEFAULT_ARTWORK_GRADIENTS = listOf(
    listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF06B6D4)),
    listOf(Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF6366F1)),
    listOf(Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF059669)),
    listOf(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF7C3AED)),
    listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF1D4ED8))
)

private val GLASS_SHEEN_BRUSH = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.25f),
        Color.Transparent,
        Color.Black.copy(alpha = 0.3f)
    ),
    start = Offset(0f, 0f),
    end = Offset(500f, 500f)
)

@Composable
fun GlassCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    theme: GlassTheme = GlassTheme.DarkGreen,
    shape: Shape = RoundedCornerShape(20.dp),
    testTag: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardScale"
    )

    val borderBrush = remember(theme) {
        Brush.linearGradient(
            colors = listOf(
                theme.glassBorder,
                Color.White.copy(alpha = 0.08f),
                theme.glassBorder.copy(alpha = 0.2f)
            )
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(theme.glassFill)
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = theme.accentColor),
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        content()
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    theme: GlassTheme = GlassTheme.DarkGreen,
    isHighlighted: Boolean = false,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "buttonScale"
    )

    val shape = RoundedCornerShape(16.dp)
    val backgroundBrush = if (isHighlighted) {
        Brush.horizontalGradient(
            colors = listOf(
                theme.accentColor,
                theme.accentColor.copy(alpha = 0.8f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                theme.glassFill,
                theme.glassFill.copy(alpha = 0.2f)
            )
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = if (isHighlighted) listOf(Color.White.copy(alpha = 0.6f), theme.accentColor)
                    else listOf(theme.glassBorder, Color.White.copy(alpha = 0.1f))
                ),
                shape = shape
            )
            .clickable(
                indication = ripple(color = theme.accentColor),
                interactionSource = interactionSource,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHighlighted) Color.White else theme.textColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (isHighlighted) Color.White else theme.textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    theme: GlassTheme = GlassTheme.DarkGreen,
    tint: Color = theme.textColor,
    isActive: Boolean = false,
    size: Dp = 48.dp,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "iconButtonScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(size)
            .clip(CircleShape)
            .background(
                if (isActive) theme.accentColor.copy(alpha = 0.35f)
                else theme.glassFill
            )
            .border(
                width = 1.dp,
                color = if (isActive) theme.accentColor else theme.glassBorder,
                shape = CircleShape
            )
            .clickable(
                indication = ripple(color = theme.accentColor),
                interactionSource = interactionSource,
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) theme.accentColor else tint,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
fun GlassChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    theme: GlassTheme = GlassTheme.DarkGreen,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "chipScale"
    )

    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(
                if (isSelected) theme.accentColor else theme.glassFill
            )
            .border(
                width = 1.dp,
                color = if (isSelected) theme.accentColor else theme.glassBorder,
                shape = shape
            )
            .clickable(
                indication = ripple(color = theme.accentColor),
                interactionSource = interactionSource,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else theme.textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun GlassArtworkCard(
    gradientIndex: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    theme: GlassTheme = GlassTheme.DarkGreen,
    shape: Shape = RoundedCornerShape(14.dp),
    titleText: String = "",
    subtitleText: String = ""
) {
    val selectedGradient = remember(gradientIndex) {
        DEFAULT_ARTWORK_GRADIENTS[gradientIndex % DEFAULT_ARTWORK_GRADIENTS.size]
    }
    var isImageError by remember(imageUrl) { mutableStateOf(false) }
    val showRecordCanvas = imageUrl.isNullOrEmpty() || isImageError

    val rotationAngle = if (isPlaying && showRecordCanvas) {
        val infiniteTransition = rememberInfiniteTransition(label = "disc_rotation")
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "disc_angle"
        )
        angle
    } else 0f

    val bgBrush = remember(selectedGradient) {
        Brush.linearGradient(colors = selectedGradient)
    }

    val borderBrush = remember(theme) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.6f),
                theme.glassBorder,
                Color.White.copy(alpha = 0.2f)
            )
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(brush = bgBrush)
            .border(
                width = 1.5.dp,
                brush = borderBrush,
                shape = shape
            )
    ) {
        if (!showRecordCanvas) {
            val context = LocalContext.current
            val imageRequest = remember(imageUrl) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .memoryCacheKey(imageUrl)
                    .diskCacheKey(imageUrl)
                    .size(256, 256)
                    .allowHardware(true)
                    .crossfade(150)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = titleText.ifEmpty { "Album Artwork" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    isImageError = true
                }
            )
        } else {
            // Vinyl Record Graphic Overlay
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .graphicsLayer {
                        rotationZ = rotationAngle
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.width / 2f

                // Outer record grooves
                drawCircle(
                    color = Color.Black.copy(alpha = 0.25f),
                    radius = outerRadius,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = outerRadius * 0.85f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = outerRadius * 0.7f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = outerRadius * 0.55f,
                    center = center
                )

                // Inner label
                drawCircle(
                    color = theme.accentColor,
                    radius = outerRadius * 0.35f,
                    center = center
                )
                drawCircle(
                    color = Color.Black,
                    radius = outerRadius * 0.1f,
                    center = center
                )
            }
        }

        // Glass reflection sheen overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = GLASS_SHEEN_BRUSH)
        )

        if (titleText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = titleText,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (subtitleText.isNotEmpty()) {
                        Text(
                            text = subtitleText,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    theme: GlassTheme = GlassTheme.DarkGreen,
    testTag: String? = null
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = theme.accentColor,
            activeTrackColor = theme.accentColor,
            inactiveTrackColor = theme.glassBorder
        ),
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    )
}
