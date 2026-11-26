package com.veyra.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Kinetic Obsidian — VEYRA Design System
 * Extracted from Stitch project. Pure OLED black + teal accent.
 */
object V {
    // Canvas
    val Black = Color(0xFF000000)

    // Surfaces — neutral grays, no color tint
    val SurfaceRaised = Color(0xFF1E2020)
    val Surface = Color(0xFF1C1C1E)
    val SurfaceDim = Color(0xFF131313)
    val SurfaceElevated = Color(0xFF2C2C2E)
    val SurfaceBright = Color(0xFF3A3A3C)

    // Brand accent
    val Teal = Color(0xFF2EC4A0)
    val TealDim = Color(0xFF006B55)
    val TealSoft = Color(0x262EC4A0)  // 15% alpha

    // Text
    val White = Color(0xFFFFFFFF)
    val Gray = Color(0xFF8E8E93)
    val GrayDark = Color(0xFF636366)
    val GrayDarkest = Color(0xFF48484A)

    // Metric palette — color = data only
    val Red = Color(0xFFE07070)
    val Green = Color(0xFF4ADE80)
    val Blue = Color(0xFF5E5CE6)
    val Amber = Color(0xFFCC8844)
    val SleepBlue = Color(0xFF5C8BC0)
    val SkyBlue = Color(0xFF42A5F5)
    val Purple = Color(0xFFAB47BC)
    val Coral = Color(0xFFFFB9A8)

    // Functional
    val Error = Color(0xFFFFB4AB)
    val Divider = Color(0xFF1C1C1E)
    val Scrim = Color(0xB3000000) // 70%
}
