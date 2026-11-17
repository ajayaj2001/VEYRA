package com.veyra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.veyra.ui.theme.V

/** Kinetic Obsidian card — solid fill, no border, no glass. */
@Composable
fun VCard(
    modifier: Modifier = Modifier,
    color: Color = V.Surface,
    radius: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.98f else 1f, tween(80), label = "card")

    Box(
        modifier
            .fillMaxWidth()
            .scale(s)
            .clip(RoundedCornerShape(radius))
            .background(color)
            .then(
                if (onClick != null) Modifier.clickable(interaction, null, onClick = onClick)
                else Modifier
            )
            .padding(20.dp),
        content = content,
    )
}

/** Primary pill button — teal fill, black text. */
@Composable
fun VButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = V.Teal,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.97f else 1f, tween(80), label = "btn")
    val alpha = if (enabled) 1f else 0.35f

    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(s)
            .clip(RoundedCornerShape(9999.dp))
            .background(color.copy(alpha = alpha))
            .clickable(interaction, null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = V.White.copy(alpha = alpha),
        )
    }
}

/** Ghost/secondary button — dark fill, white text. */
@Composable
fun VButtonGhost(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = V.White,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.97f else 1f, tween(80), label = "ghost")

    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(s)
            .clip(RoundedCornerShape(9999.dp))
            .background(V.Surface)
            .clickable(interaction, null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}

/** Progress bar: 3 segments for onboarding steps. */
@Composable
fun StepProgress(
    current: Int,
    total: Int = 3,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < current) V.Teal else V.SurfaceBright)
            )
        }
    }
}

/** Pagination dots for onboarding swipes. */
@Composable
fun PageDots(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .height(6.dp)
                    .then(
                        if (i == current) Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            // Active dot is wider capsule
                            .clip(RoundedCornerShape(3.dp))
                            .background(V.Teal)
                        else Modifier
                            .weight(0.3f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(V.SurfaceBright)
                    )
            )
        }
    }
}
