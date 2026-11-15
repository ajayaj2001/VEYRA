package com.veyra.ui.theme

import androidx.compose.ui.graphics.Color

// Core brand palette — deep, rich tones inspired by Samsung/Bevel/Vora
object VeyraColors {
    // Primary gradient endpoints
    val PrimaryStart = Color(0xFF6C63FF)   // Vibrant indigo
    val PrimaryEnd = Color(0xFF3F37C9)     // Deep indigo
    val PrimaryMid = Color(0xFF5650E8)     // Mid indigo

    // Accent — energetic pop
    val Accent = Color(0xFF00D2FF)          // Cyan electric
    val AccentSoft = Color(0xFF80E9FF)      // Soft cyan
    val AccentGlow = Color(0x4000D2FF)      // Glow cyan (25% alpha)

    // Secondary accents
    val Coral = Color(0xFFFF6B6B)
    val CoralSoft = Color(0xFFFF9B9B)
    val Amber = Color(0xFFFFBE0B)
    val AmberSoft = Color(0xFFFFD666)
    val Mint = Color(0xFF00E5A0)
    val MintSoft = Color(0xFF66F0C8)
    val Purple = Color(0xFFB06CFF)
    val PurpleSoft = Color(0xFFCDA0FF)

    // Surface system — ultra dark with blue tint
    val Background = Color(0xFF0A0B14)      // Near black with blue tint
    val Surface = Color(0xFF12131F)         // Card base
    val SurfaceElevated = Color(0xFF1A1B2E) // Elevated card
    val SurfaceGlass = Color(0x1AFFFFFF)    // Glassmorphic overlay (10% white)
    val SurfaceGlassStrong = Color(0x33FFFFFF) // Stronger glass (20% white)
    val SurfaceBorder = Color(0x1AFFFFFF)   // Subtle border

    // Text hierarchy
    val TextPrimary = Color(0xFFF0F0FF)     // Bright white with warmth
    val TextSecondary = Color(0xB3F0F0FF)   // 70% primary
    val TextTertiary = Color(0x66F0F0FF)    // 40% primary
    val TextOnPrimary = Color(0xFFFFFFFF)

    // Score colors
    val ScoreExcellent = Color(0xFF00E5A0)  // Green
    val ScoreGood = Color(0xFF00D2FF)       // Cyan
    val ScoreModerate = Color(0xFFFFBE0B)   // Amber
    val ScoreLow = Color(0xFFFF6B6B)        // Coral/Red

    // Functional
    val Divider = Color(0x1AFFFFFF)
    val Shimmer = Color(0x0DFFFFFF)
    val Overlay = Color(0xCC000000)
    val Scrim = Color(0x80000000)

    // Health metric specific
    val HeartRate = Color(0xFFFF6B6B)
    val Sleep = Color(0xFF6C63FF)
    val Steps = Color(0xFF00E5A0)
    val Calories = Color(0xFFFFBE0B)
    val HRV = Color(0xFFB06CFF)
    val Recovery = Color(0xFF00D2FF)
    val Strain = Color(0xFFFF6B6B)
}
