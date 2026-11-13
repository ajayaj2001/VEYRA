package com.pulse.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF000000)
val Surface = Color(0xFF0D0D0F)
val SurfaceHigh = Color(0xFF17171A)
val Hairline = Color(0xFF28282D)
val Bone = Color(0xFFF5F4EF)
val Muted = Color(0xFF96969E)
val Acid = Color(0xFFE6FF5C)
val Recovery = Color(0xFF46E6A0)
val Sleep = Color(0xFF9385FF)
val Strain = Color(0xFFFF7B35)
val Danger = Color(0xFFFF6262)

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
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 84.sp,
        lineHeight = 84.sp,
        letterSpacing = (-3).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 52.sp,
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
fun PulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
