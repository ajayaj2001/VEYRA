package com.veyra.ui.theme

import androidx.compose.ui.graphics.Color

object VeyraColors {
    // Background — pure black like Samsung Health
    val Background = Color(0xFF000000)

    // Card surfaces — solid dark charcoal, NO borders, NO glass
    val Surface = Color(0xFF1C1C1E)
    val SurfaceElevated = Color(0xFF2C2C2E)
    val SurfaceBright = Color(0xFF3A3A3C)

    // Primary green accent — Samsung Health's signature green
    val Green = Color(0xFF6DD400)
    val GreenDark = Color(0xFF5AB800)
    val GreenSoft = Color(0x336DD400)

    // Teal — for feature banners and links
    val Teal = Color(0xFF00BFA5)
    val TealDark = Color(0xFF009688)
    val TealGradientStart = Color(0xFF00897B)
    val TealGradientEnd = Color(0xFF004D40)

    // Blue — for sleep, cardio features
    val Blue = Color(0xFF448AFF)
    val BlueDark = Color(0xFF1565C0)
    val BlueGradientStart = Color(0xFF1E88E5)
    val BlueGradientEnd = Color(0xFF0D47A1)

    // Secondary accents
    val Orange = Color(0xFFFF6D00)
    val OrangeSoft = Color(0xFFFF9100)
    val Coral = Color(0xFFE57373)
    val Purple = Color(0xFFAB47BC)
    val PurpleSoft = Color(0xFFCE93D8)
    val Amber = Color(0xFFFFCA28)
    val Olive = Color(0xFF9E9D24)

    // Text hierarchy — Samsung uses pure white + grays
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8E8E93)
    val TextTertiary = Color(0xFF636366)
    val TextDisabled = Color(0xFF48484A)

    // Functional
    val Divider = Color(0xFF2C2C2E)
    val Overlay = Color(0xCC000000)
    val Scrim = Color(0x80000000)

    // Tab & chip backgrounds
    val TabSelected = Color(0xFF3A3A3C)
    val TabUnselected = Color.Transparent
    val NavPill = Color(0xFF3A3A3C)

    // Icon circle backgrounds (Samsung exercise style)
    val IconCircleGreen = Color(0xFF6DD400)
    val IconCircleOlive = Color(0xFF9E9D24)
    val IconCircleCoral = Color(0xFFE57373)
    val IconCircleGray = Color(0xFF636366)
    val IconCircleTeal = Color(0xFF26A69A)
    val IconCircleBlue = Color(0xFF42A5F5)
    val IconCirclePurple = Color(0xFFAB47BC)
    val IconCircleAmber = Color(0xFFFFCA28)

    // Health metric specific
    val HeartRate = Color(0xFFE57373)
    val Sleep = Color(0xFF448AFF)
    val Steps = Color(0xFF6DD400)
    val Calories = Color(0xFFAB47BC)
    val ActiveMinutes = Color(0xFF42A5F5)
    val HRV = Color(0xFF26A69A)
    val Recovery = Color(0xFF00BFA5)
    val Strain = Color(0xFFFF6D00)

    // Score colors
    val ScoreExcellent = Color(0xFF6DD400)
    val ScoreGood = Color(0xFF00BFA5)
    val ScoreModerate = Color(0xFFFFCA28)
    val ScoreLow = Color(0xFFE57373)

    // Progress bar
    val ProgressTrack = Color(0xFF3A3A3C)
    val ProgressFill = Color(0xFF6DD400)
}
