package com.veyra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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

/**
 * Samsung Health-style card: solid dark charcoal fill, large rounded corners,
 * NO borders, NO glass effects, NO gradients by default.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = VeyraDimens.CardRadius,
    backgroundColor: Color = VeyraColors.Surface,
    gradientColors: List<Color>? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(80),
        label = "cardPress",
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .then(
                if (gradientColors != null) {
                    Modifier.background(Brush.linearGradient(gradientColors))
                } else {
                    Modifier.background(backgroundColor)
                }
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

/**
 * Samsung Health-style gradient feature card (teal/blue gradient for highlights).
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    startColor: Color = VeyraColors.TealGradientStart,
    endColor: Color = VeyraColors.TealGradientEnd,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        gradientColors = listOf(startColor, endColor),
        content = content,
    )
}
