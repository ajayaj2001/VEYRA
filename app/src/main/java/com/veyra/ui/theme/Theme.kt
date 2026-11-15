package com.veyra.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VeyraColorScheme = darkColorScheme(
    primary = VeyraColors.PrimaryStart,
    onPrimary = VeyraColors.TextOnPrimary,
    primaryContainer = VeyraColors.PrimaryEnd,
    onPrimaryContainer = VeyraColors.TextOnPrimary,
    secondary = VeyraColors.Accent,
    onSecondary = VeyraColors.Background,
    secondaryContainer = VeyraColors.SurfaceElevated,
    onSecondaryContainer = VeyraColors.Accent,
    tertiary = VeyraColors.Mint,
    onTertiary = VeyraColors.Background,
    background = VeyraColors.Background,
    onBackground = VeyraColors.TextPrimary,
    surface = VeyraColors.Surface,
    onSurface = VeyraColors.TextPrimary,
    surfaceVariant = VeyraColors.SurfaceElevated,
    onSurfaceVariant = VeyraColors.TextSecondary,
    outline = VeyraColors.SurfaceBorder,
    outlineVariant = VeyraColors.Divider,
    error = VeyraColors.Coral,
    onError = VeyraColors.TextOnPrimary,
    scrim = VeyraColors.Scrim,
)

@Composable
fun VeyraTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = VeyraColorScheme,
        typography = VeyraTypography,
        content = content,
    )
}
