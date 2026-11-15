package com.veyra.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF06110D)
val Surface = Color(0xE613241D)
val SurfaceHigh = Color(0xF21B3027)
val Hairline = Color(0xFF355044)
val Bone = Color(0xFFF7F8F3)
val Muted = Color(0xFFA6B4AC)
val Acid = Color(0xFFDFFF69)
val Recovery = Color(0xFF54E6AD)
val Sleep = Color(0xFFA294FF)
val Strain = Color(0xFF5B8CFF)
val Danger = Color(0xFFFF6262)
val AppBackgroundBrush = Brush.verticalGradient(
    0f to Color(0xFF2A250F),
    .22f to Color(0xFF132D24),
    .58f to Color(0xFF081A14),
    1f to Ink,
)

val CardBackgroundBrush = Brush.linearGradient(
    listOf(Color(0xF02A3B34), Color(0xE6172922), Color(0xF00D1C17)),
)

private val colors = darkColorScheme(
    primary = Acid,
    onPrimary = Ink,
    background = Ink,
    onBackground = Bone,
    surface = Surface,
    onSurface = Bone,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = Muted,
    outline = Hairline,
    error = Danger
)

private val typography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 76.sp,
        lineHeight = 78.sp,
        letterSpacing = (-3).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 54.sp,
        letterSpacing = (-2).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.2.sp
    )
)

@Composable
fun VeyraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
