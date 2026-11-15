package com.veyra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.veyra.ui.theme.VeyraColors
import com.veyra.ui.theme.VeyraDimens

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = VeyraDimens.RadiusXl,
    glassAlpha: Float = 0.08f,
    borderAlpha: Float = 0.12f,
    gradientColors: List<Color>? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "cardPress",
    )

    val shape = RoundedCornerShape(cornerRadius)
    val bgBrush = if (gradientColors != null) {
        Brush.linearGradient(gradientColors)
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = glassAlpha),
                Color.White.copy(alpha = glassAlpha * 0.4f),
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(VeyraColors.Surface)
            .background(bgBrush)
            .border(
                width = VeyraDimens.SpaceXxs,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha),
                        Color.White.copy(alpha = borderAlpha * 0.3f),
                    )
                ),
                shape = shape,
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier
            )
            .padding(VeyraDimens.CardPadding),
        content = content,
    )
}

@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    startColor: Color = VeyraColors.PrimaryStart,
    endColor: Color = VeyraColors.PrimaryEnd,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        gradientColors = listOf(startColor, endColor),
        borderAlpha = 0.2f,
        content = content,
    )
}
