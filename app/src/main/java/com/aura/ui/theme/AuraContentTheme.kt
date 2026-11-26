package com.aura.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.R

internal val AuraScreenInset = 24.dp
internal val AuraFieldRadius = 16.dp

internal val AuraWordmark = TextStyle(
    fontFamily = FontFamily(Font(R.font.sora_semibold, FontWeight.SemiBold)),
    fontSize = 44.sp,
    lineHeight = 56.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = (-1.3).sp,
)

internal val AuraBody = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 24.sp)
internal val AuraCaption = AuraBody.copy(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
internal val AuraButtonLabel = AuraBody.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium)
private val AuraHeading = AuraBody.copy(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold)
private val AuraSectionTitle = AuraHeading.copy(fontSize = 24.sp, lineHeight = 32.sp)
private val AuraContentTypography = AuraTypography.copy(
    displayLarge = AuraHeading,
    displayMedium = AuraHeading,
    displaySmall = AuraSectionTitle,
    headlineLarge = AuraHeading,
    headlineMedium = AuraSectionTitle,
    headlineSmall = AuraButtonLabel,
    titleLarge = AuraButtonLabel,
    titleMedium = AuraBody.copy(fontWeight = FontWeight.Medium),
    titleSmall = AuraCaption,
    bodyLarge = AuraBody,
    bodyMedium = AuraBody,
    bodySmall = AuraCaption,
    labelLarge = AuraButtonLabel,
    labelMedium = AuraCaption,
    labelSmall = AuraCaption,
)

@Composable
internal fun AuraContentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            surface = V.SurfaceRaised,
            surfaceVariant = V.SurfaceRaised,
            surfaceContainer = V.SurfaceRaised,
            surfaceContainerHigh = V.SurfaceRaised,
        ),
        typography = AuraContentTypography,
        content = content,
    )
}
