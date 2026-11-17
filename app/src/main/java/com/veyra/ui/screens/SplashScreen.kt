package com.veyra.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veyra.AppConfig
import com.veyra.ui.theme.V
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // Fade in animation
    val fadeIn = remember { Animatable(0f) }
    // Logo scale
    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        // Fade in
        fadeIn.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        // Hold
        delay(1800)
        // Fade out
        fadeIn.animateTo(0f, tween(400))
        onFinished()
    }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }

    // Continuous ring rotation
    val inf = rememberInfiniteTransition(label = "splash")
    val ring1Angle by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "r1",
    )
    val ring2Angle by inf.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "r2",
    )
    val ring3Angle by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "r3",
    )
    // Center pulse
    val pulse by inf.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(V.Black)
            .alpha(fadeIn.value),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Animated concentric vitals logo
            Box(
                Modifier.size((120 * scale.value).dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width / 2
                    val cy = size.height / 2

                    // Ghost tracks
                    val trackStroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    drawCircle(V.SurfaceBright.copy(alpha = 0.08f), cx * 0.95f, Offset(cx, cy), style = trackStroke)
                    drawCircle(V.SurfaceBright.copy(alpha = 0.06f), cx * 0.72f, Offset(cx, cy), style = trackStroke)
                    drawCircle(V.SurfaceBright.copy(alpha = 0.05f), cx * 0.50f, Offset(cx, cy), style = trackStroke)

                    // Outer arc — Activity (Teal)
                    val outerR = cx * 0.95f
                    val outerStroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    rotate(ring1Angle, Offset(cx, cy)) {
                        drawArc(
                            color = V.Teal,
                            startAngle = 0f, sweepAngle = 240f,
                            useCenter = false,
                            topLeft = Offset(cx - outerR, cy - outerR),
                            size = Size(outerR * 2, outerR * 2),
                            style = outerStroke,
                        )
                    }
                    // Outer dot
                    val outerDotAngle = Math.toRadians((ring1Angle).toDouble())
                    drawCircle(
                        V.Teal, 5.dp.toPx(),
                        Offset(cx + outerR * cos(outerDotAngle).toFloat(), cy + outerR * sin(outerDotAngle).toFloat()),
                    )

                    // Middle arc — Recovery (Coral)
                    val midR = cx * 0.72f
                    val midStroke = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    rotate(ring2Angle, Offset(cx, cy)) {
                        drawArc(
                            color = V.Red,
                            startAngle = 60f, sweepAngle = 220f,
                            useCenter = false,
                            topLeft = Offset(cx - midR, cy - midR),
                            size = Size(midR * 2, midR * 2),
                            style = midStroke,
                        )
                    }
                    // Middle dot
                    val midDotAngle = Math.toRadians((ring2Angle + 60).toDouble())
                    drawCircle(
                        V.Red, 4.dp.toPx(),
                        Offset(cx + midR * cos(midDotAngle).toFloat(), cy + midR * sin(midDotAngle).toFloat()),
                    )

                    // Inner arc — Nutrition (Amber)
                    val innerR = cx * 0.50f
                    val innerStroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    rotate(ring3Angle, Offset(cx, cy)) {
                        drawArc(
                            color = V.Amber,
                            startAngle = 120f, sweepAngle = 200f,
                            useCenter = false,
                            topLeft = Offset(cx - innerR, cy - innerR),
                            size = Size(innerR * 2, innerR * 2),
                            style = innerStroke,
                        )
                    }
                    // Inner dot
                    val innerDotAngle = Math.toRadians((ring3Angle + 120).toDouble())
                    drawCircle(
                        V.Amber, 3.5.dp.toPx(),
                        Offset(cx + innerR * cos(innerDotAngle).toFloat(), cy + innerR * sin(innerDotAngle).toFloat()),
                    )

                    // Center core — pulsing
                    drawCircle(V.Teal.copy(alpha = 0.08f), cx * 0.32f * pulse, Offset(cx, cy))
                    drawCircle(V.Teal, cx * 0.12f * pulse, Offset(cx, cy))
                    drawCircle(V.White, cx * 0.05f, Offset(cx, cy))
                }
            }

            Spacer(Modifier.height(32.dp))

            // App name
            Text(
                text = AppConfig.APP_NAME_UPPER,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp,
                ),
                color = V.White,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = AppConfig.APP_TAGLINE,
                style = MaterialTheme.typography.bodySmall,
                color = V.GrayDark,
            )
        }
    }
}
