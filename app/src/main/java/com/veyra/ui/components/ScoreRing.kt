package com.veyra.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.veyra.ui.theme.VeyraColors
import com.veyra.ui.theme.VeyraDimens

@Composable
fun ScoreRing(
    score: Int,
    maxScore: Int = 100,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = VeyraDimens.ScoreRingSize,
    strokeWidth: Dp = VeyraDimens.ScoreRingStroke,
    secondaryColor: Color = color.copy(alpha = 0.3f),
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(score) {
        progress.animateTo(
            targetValue = score.toFloat() / maxScore.toFloat(),
            animationSpec = tween(1200, easing = FastOutSlowInEasing),
        )
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(
                this.size.width - strokePx,
                this.size.height - strokePx,
            )
            val topLeft = Offset(strokePx / 2, strokePx / 2)

            // Background track
            drawArc(
                color = secondaryColor.copy(alpha = 0.15f),
                startAngle = -225f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // Progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(color.copy(alpha = 0.6f), color),
                ),
                startAngle = -225f,
                sweepAngle = 270f * progress.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.28f).sp,
                ),
                color = VeyraColors.TextPrimary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = VeyraColors.TextTertiary,
            )
        }
    }
}

fun scoreColor(score: Int): Color = when {
    score >= 80 -> VeyraColors.ScoreExcellent
    score >= 60 -> VeyraColors.ScoreGood
    score >= 40 -> VeyraColors.ScoreModerate
    else -> VeyraColors.ScoreLow
}
