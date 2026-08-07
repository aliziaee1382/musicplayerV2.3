package com.example.model

import androidx.compose.ui.graphics.Color

data class GlassTheme(
    val id: String,
    val name: String,
    val colorKey: String, // "blue", "red", "purple", "yellow", "green", "orange"
    val colorNameEn: String,
    val colorNameFa: String,
    val isLight: Boolean,
    val bgGradient: List<Color>,
    val accentColor: Color,
    val glassFill: Color,
    val glassBorder: Color,
    val glowColor: Color,
    val textColor: Color = if (isLight) Color(0xFF0F172A) else Color.White,
    val subtextColor: Color = if (isLight) Color(0xFF475569) else Color.White.copy(alpha = 0.65f)
) {
    companion object {
        val DarkBlue = GlassTheme(
            id = "dark_blue",
            name = "Dark Blue",
            colorKey = "blue",
            colorNameEn = "Blue",
            colorNameFa = "Blue",
            isLight = false,
            bgGradient = listOf(Color(0xFF0A192F), Color(0xFF0F2B4A), Color(0xFF06101E), Color(0xFF020914)),
            accentColor = Color(0xFF38BDF8),
            glassFill = Color(0xE00F172A),
            glassBorder = Color(0x5038BDF8),
            glowColor = Color(0x660284C7)
        )

        val LightBlue = GlassTheme(
            id = "light_blue",
            name = "Light Blue",
            colorKey = "blue",
            colorNameEn = "Blue",
            colorNameFa = "Blue",
            isLight = true,
            bgGradient = listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD), Color(0xFFE0E7FF), Color(0xFFF1F5F9)),
            accentColor = Color(0xFF0284C7),
            glassFill = Color(0xF2FFFFFF),
            glassBorder = Color(0x500284C7),
            glowColor = Color(0x3038BDF8)
        )

        val DarkRed = GlassTheme(
            id = "dark_red",
            name = "Dark Red",
            colorKey = "red",
            colorNameEn = "Red",
            colorNameFa = "Red",
            isLight = false,
            bgGradient = listOf(Color(0xFF2E081A), Color(0xFF450A23), Color(0xFF1F0310), Color(0xFF0F0108)),
            accentColor = Color(0xFFFB7185),
            glassFill = Color(0xE01F0814),
            glassBorder = Color(0x50FB7185),
            glowColor = Color(0x66E11D48)
        )

        val LightRed = GlassTheme(
            id = "light_red",
            name = "Light Red",
            colorKey = "red",
            colorNameEn = "Red",
            colorNameFa = "Red",
            isLight = true,
            bgGradient = listOf(Color(0xFFFFE4E6), Color(0xFFFECDD3), Color(0xFFFCE7F3), Color(0xFFFFF1F2)),
            accentColor = Color(0xFFE11D48),
            glassFill = Color(0xF2FFFFFF),
            glassBorder = Color(0x50E11D48),
            glowColor = Color(0x30FB7185)
        )

        val DarkPurple = GlassTheme(
            id = "dark_purple",
            name = "Dark Purple",
            colorKey = "purple",
            colorNameEn = "Purple",
            colorNameFa = "Purple",
            isLight = false,
            bgGradient = listOf(Color(0xFF130A2A), Color(0xFF261247), Color(0xFF1D1B4B), Color(0xFF0F172A)),
            accentColor = Color(0xFFA78BFA),
            glassFill = Color(0xE0130D2A),
            glassBorder = Color(0x50A78BFA),
            glowColor = Color(0x668B5CF6)
        )

        val LightPurple = GlassTheme(
            id = "light_purple",
            name = "Light Purple",
            colorKey = "purple",
            colorNameEn = "Purple",
            colorNameFa = "Purple",
            isLight = true,
            bgGradient = listOf(Color(0xFFF3E8FF), Color(0xFFEDE9FE), Color(0xFFE0E7FF), Color(0xFFF8FAFC)),
            accentColor = Color(0xFF7C3AED),
            glassFill = Color(0xF2FFFFFF),
            glassBorder = Color(0x507C3AED),
            glowColor = Color(0x30A78BFA)
        )

        val DarkYellow = GlassTheme(
            id = "dark_yellow",
            name = "Dark Yellow",
            colorKey = "yellow",
            colorNameEn = "Yellow",
            colorNameFa = "Yellow",
            isLight = false,
            bgGradient = listOf(Color(0xFF261300), Color(0xFF3B1D00), Color(0xFF1F1000), Color(0xFF120800)),
            accentColor = Color(0xFFFBBF24),
            glassFill = Color(0xE01C1105),
            glassBorder = Color(0x50FBBF24),
            glowColor = Color(0x66F59E0B)
        )

        val LightYellow = GlassTheme(
            id = "light_yellow",
            name = "Light Yellow",
            colorKey = "yellow",
            colorNameEn = "Yellow",
            colorNameFa = "Yellow",
            isLight = true,
            bgGradient = listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A), Color(0xFFFEF9C3), Color(0xFFFFFBEB)),
            accentColor = Color(0xFFD97706),
            glassFill = Color(0xF2FFFFFF),
            glassBorder = Color(0x50D97706),
            glowColor = Color(0x30FBBF24)
        )

        val DarkGreen = GlassTheme(
            id = "dark_green",
            name = "Dark Green",
            colorKey = "green",
            colorNameEn = "Green",
            colorNameFa = "Green",
            isLight = false,
            bgGradient = listOf(Color(0xFF02201D), Color(0xFF042F2C), Color(0xFF093824), Color(0xFF031412)),
            accentColor = Color(0xFF34D399),
            glassFill = Color(0xE0061A18),
            glassBorder = Color(0x5034D399),
            glowColor = Color(0x6610B981)
        )

        val LightGreen = GlassTheme(
            id = "light_green",
            name = "Light Green",
            colorKey = "green",
            colorNameEn = "Green",
            colorNameFa = "Green",
            isLight = true,
            bgGradient = listOf(Color(0xFFD1FAE5), Color(0xFFECFDF5), Color(0xFFDCFCE7), Color(0xFFF0FDF4)),
            accentColor = Color(0xFF059669),
            glassFill = Color(0xF2FFFFFF),
            glassBorder = Color(0x50059669),
            glowColor = Color(0x3034D399)
        )

        val DarkOrange = GlassTheme(
            id = "dark_orange",
            name = "Dark Orange",
            colorKey = "orange",
            colorNameEn = "Orange",
            colorNameFa = "Orange",
            isLight = false,
            bgGradient = listOf(Color(0xFF2A0F03), Color(0xFF3E1700), Color(0xFF1D0A00), Color(0xFF120500)),
            accentColor = Color(0xFFFB923C),
            glassFill = Color(0xE01E0C04),
            glassBorder = Color(0x50FB923C),
            glowColor = Color(0x66EA580C)
        )

        val LightOrange = GlassTheme(
            id = "light_orange",
            name = "Light Orange",
            colorKey = "orange",
            colorNameEn = "Orange",
            colorNameFa = "Orange",
            isLight = true,
            bgGradient = listOf(Color(0xFFFFEDD5), Color(0xFFFFD8A8), Color(0xFFFFF7ED), Color(0xFFFAFAF9)),
            accentColor = Color(0xFFEA580C),
            glassFill = Color(0xF2FFFFFF),
            glassBorder = Color(0x50EA580C),
            glowColor = Color(0x30FB923C)
        )

        val PurpleBlue = DarkPurple
        val NeonMidnight = DarkBlue
        val EmeraldAurora = DarkGreen
        val SunsetCrimson = DarkRed
        val CyberpunkAmber = DarkYellow

        val ALL_THEMES = listOf(
            DarkBlue, LightBlue,
            DarkRed, LightRed,
            DarkPurple, LightPurple,
            DarkYellow, LightYellow,
            DarkGreen, LightGreen,
            DarkOrange, LightOrange
        )

        data class ColorOption(
            val key: String,
            val nameEn: String,
            val nameFa: String,
            val color: Color
        )

        val COLOR_OPTIONS = listOf(
            ColorOption("blue", "Blue", "Blue", Color(0xFF38BDF8)),
            ColorOption("red", "Red", "Red", Color(0xFFFB7185)),
            ColorOption("purple", "Purple", "Purple", Color(0xFFA78BFA)),
            ColorOption("yellow", "Yellow", "Yellow", Color(0xFFFBBF24)),
            ColorOption("green", "Green", "Green", Color(0xFF34D399)),
            ColorOption("orange", "Orange", "Orange", Color(0xFFFB923C))
        )

        fun getThemeForModeAndColor(isLight: Boolean, colorKey: String): GlassTheme {
            return ALL_THEMES.find { it.isLight == isLight && it.colorKey == colorKey }
                ?: if (isLight) LightPurple else DarkPurple
        }
    }
}

