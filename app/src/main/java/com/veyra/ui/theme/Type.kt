package com.veyra.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Uses system default sans-serif (Roboto on most Android devices).
// To use Inter: add font files to res/font/ and define a custom FontFamily here.
val VeyraFontFamily = FontFamily.SansSerif

val VeyraTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = VeyraFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.5).sp,
        color = VeyraColors.TextPrimary,
    ),
    displayMedium = TextStyle(
        fontFamily = VeyraFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        color = VeyraColors.TextPrimary,
    ),
    displaySmall = TextStyle(
        fontFamily = VeyraFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.25).sp,
        color = VeyraColors.TextPrimary,
    ),
    headlineLarge = TextStyle(
        fontFamily = VeyraFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp,
        color = VeyraColors.TextPrimary,
    ),
    headlineMedium = TextStyle(
        fontFamily = VeyraFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = VeyraColors.TextPrimary,
    ),
    headlineSmall = TextStyle(
        fontFamily = VeyraFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = VeyraColors.TextPrimary,
    ),
    titleLarge = TextStyle(
        fontFamily = VeyraFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = VeyraColors.TextPrimary,
    ),
    titleMedium = TextStyle(
        fontFamily = VeyraFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = VeyraColors.TextPrimary,
    ),
    titleSmall = TextStyle(
        fontFamily = VeyraFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
        color = VeyraColors.TextSecondary,
    ),
    bodyLarge = TextStyle(
        fontFamily = VeyraFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = VeyraColors.TextPrimary,
    ),
    bodyMedium = TextStyle(
        fontFamily = VeyraFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = VeyraColors.TextSecondary,
    ),
    bodySmall = TextStyle(
        fontFamily = VeyraFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = VeyraColors.TextTertiary,
    ),
    labelLarge = TextStyle(
        fontFamily = VeyraFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
        color = VeyraColors.TextPrimary,
    ),
    labelMedium = TextStyle(
        fontFamily = VeyraFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = VeyraColors.TextSecondary,
    ),
    labelSmall = TextStyle(
        fontFamily = VeyraFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        color = VeyraColors.TextTertiary,
    ),
)
