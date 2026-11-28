package com.aura.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun AuraTheme(content: @Composable () -> Unit) {
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
        colorScheme = darkColorScheme(
            primary = V.Teal,
            onPrimary = V.Black,
            primaryContainer = V.Teal,
            onPrimaryContainer = V.Black,
            secondary = V.Gray,
            onSecondary = V.Black,
            background = V.Canvas,
            onBackground = V.White,
            surface = V.Surface,
            onSurface = V.White,
            surfaceVariant = V.SurfaceElevated,
            onSurfaceVariant = V.Gray,
            outline = V.SurfaceBright,
            outlineVariant = V.Divider,
            error = V.Error,
            onError = V.Black,
            scrim = V.Scrim,
        ),
        typography = AuraTypography,
        content = content,
    )
}
