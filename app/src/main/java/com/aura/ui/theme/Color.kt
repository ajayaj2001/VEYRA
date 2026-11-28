package com.aura.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class AuraPalette(val label: String, internal val colors: Colors) {
    MINERAL("Mineral", Colors(
        black = Color(0xFF000000), canvas = Color(0xFF091412),
        surfaceRaised = Color(0xFF142623), surface = Color(0xFF10201D),
        surfaceDim = Color(0xFF0D1A18), surfaceElevated = Color(0xFF20342F),
        surfaceBright = Color(0xFF344C44), accent = Color(0xFF71D9BB), accentDim = Color(0xFF007459),
        accentSoft = Color(0x2671D9BB), white = Color(0xFFF7F3E8), gray = Color(0xFFA4B7AC),
        grayDark = Color(0xFF81988D), grayDarkest = Color(0xFF536E62), divider = Color(0xFF263B34),
    )),
    EMBER("Ember", Colors(
        black = Color(0xFF160C0B), canvas = Color(0xFF160D0D),
        surfaceRaised = Color(0xFF302022), surface = Color(0xFF261719),
        surfaceDim = Color(0xFF1D1112), surfaceElevated = Color(0xFF3B2729),
        surfaceBright = Color(0xFF5B3A39), accent = Color(0xFFE36A49), accentDim = Color(0xFFA83D2B),
        accentSoft = Color(0x26E36A49), white = Color(0xFFFBF2E9), gray = Color(0xFFD0AAA2),
        grayDark = Color(0xFFAA817B), grayDarkest = Color(0xFF785A57), divider = Color(0xFF493031),
    )),
}

internal data class Colors(
    val black: Color,
    val canvas: Color,
    val surfaceRaised: Color,
    val surface: Color,
    val surfaceDim: Color,
    val surfaceElevated: Color,
    val surfaceBright: Color,
    val accent: Color,
    val accentDim: Color,
    val accentSoft: Color,
    val white: Color,
    val gray: Color,
    val grayDark: Color,
    val grayDarkest: Color,
    val divider: Color,
)

/** App palette values are Compose state so every screen updates when Appearance changes. */
object V {
    private var activePalette by mutableStateOf(AuraPalette.MINERAL)

    val palette: AuraPalette get() = activePalette
    fun selectPalette(palette: AuraPalette) { activePalette = palette }
    fun paletteFor(value: String?): AuraPalette = AuraPalette.entries.firstOrNull { it.name == value } ?: AuraPalette.MINERAL

    val Black get() = activePalette.colors.black
    val Canvas get() = activePalette.colors.canvas
    val SurfaceRaised get() = activePalette.colors.surfaceRaised
    val Surface get() = activePalette.colors.surface
    val SurfaceDim get() = activePalette.colors.surfaceDim
    val SurfaceElevated get() = activePalette.colors.surfaceElevated
    val SurfaceBright get() = activePalette.colors.surfaceBright
    val Teal get() = activePalette.colors.accent
    val TealDim get() = activePalette.colors.accentDim
    val TealSoft get() = activePalette.colors.accentSoft
    val White get() = activePalette.colors.white
    val ButtonText = Color.White
    val Gray get() = activePalette.colors.gray
    val GrayDark get() = activePalette.colors.grayDark
    val GrayDarkest get() = activePalette.colors.grayDarkest

    // Metric and nutrition colors keep their meaning in every appearance palette.
    val Red = Color(0xFFE07070)
    val Green = Color(0xFF4ADE80)
    val Blue = Color(0xFF5E5CE6)
    val Amber = Color(0xFFCC8844)
    val SleepBlue = Color(0xFF5C8BC0)
    val SkyBlue = Color(0xFF42A5F5)
    val Purple = Color(0xFFAB47BC)
    val Coral = Color(0xFFFFB9A8)

    val Error = Color(0xFFFFB4AB)
    val Divider get() = activePalette.colors.divider
    val Scrim = Color(0xB3000000) // 70%
}
