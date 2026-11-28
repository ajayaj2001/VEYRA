package com.aura.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.aura.R
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.ui.theme.V

/** Mineral card with an optional slate accent for featured content. */
@Composable
fun VCard(
    modifier: Modifier = Modifier,
    color: Color = V.SurfaceRaised,
    radius: Dp = 20.dp,
    featured: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.98f else 1f, tween(80), label = "card")

    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .fillMaxWidth()
            .scale(s)
            .clip(shape)
            .background(color)
            .then(
                if (featured) Modifier.paint(
                    painterResource(R.drawable.mineral_slate),
                    sizeToIntrinsics = false,
                    alignment = Alignment.TopEnd,
                    contentScale = ContentScale.Crop,
                    alpha = 0.6f,
                ) else Modifier
            )
            .border(1.dp, V.Divider.copy(alpha = 0.75f), shape)
            .then(
                if (onClick != null) Modifier.clickable(interaction, null, onClick = onClick)
                else Modifier
            )
            .padding(16.dp),
        content = content,
    )
}

/** Primary button with a jade fill and white text. */
@Composable
fun VButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = V.TealDim,
    contentColor: Color = V.ButtonText,
    labelStyle: TextStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.97f else 1f, tween(80), label = "btn")
    val alpha = if (enabled) 1f else 0.35f

    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .scale(s)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = alpha))
            .clickable(interaction, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = labelStyle,
            textAlign = TextAlign.Center,
            color = contentColor.copy(alpha = alpha),
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
    spacing: Dp = 8.dp,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
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
