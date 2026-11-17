package com.veyra.ui.screens.auth

import com.veyra.AppConfig
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veyra.ui.theme.V
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LoginScreen(
    onGoogleSignIn: () -> Unit,
    onExistingUser: () -> Unit,
) {
    val inf = rememberInfiniteTransition(label = "login")
    val r1 by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart), label = "r1")
    val r2 by inf.animateFloat(360f, 0f, infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart), label = "r2")
    val r3 by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart), label = "r3")
    val pulse by inf.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(V.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val h = maxHeight

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(h * 0.10f))

            // Wordmark
            Text(
                text = AppConfig.APP_NAME_UPPER,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp,
                ),
                color = V.White,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = AppConfig.APP_TAGLINE,
                style = MaterialTheme.typography.bodyMedium,
                color = V.GrayDark,
            )

            Spacer(Modifier.weight(1f))

            // Animated concentric rings — same as splash but bigger
            Box(
                Modifier.size((h * 0.28f).coerceAtMost(220.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width / 2
                    val cy = size.height / 2

                    // Ghost tracks
                    val track = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
                    drawCircle(V.SurfaceBright.copy(alpha = 0.08f), cx * 0.92f, Offset(cx, cy), style = track)
                    drawCircle(V.SurfaceBright.copy(alpha = 0.06f), cx * 0.68f, Offset(cx, cy), style = track)
                    drawCircle(V.SurfaceBright.copy(alpha = 0.05f), cx * 0.45f, Offset(cx, cy), style = track)

                    // Outer — Teal
                    val outerR = cx * 0.92f
                    rotate(r1, Offset(cx, cy)) {
                        drawArc(V.Teal, 0f, 240f, false, Offset(cx - outerR, cy - outerR), Size(outerR * 2, outerR * 2), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                    }
                    val a1 = Math.toRadians(r1.toDouble())
                    drawCircle(V.Teal, 5.dp.toPx(), Offset(cx + outerR * cos(a1).toFloat(), cy + outerR * sin(a1).toFloat()))

                    // Middle — Coral
                    val midR = cx * 0.68f
                    rotate(r2, Offset(cx, cy)) {
                        drawArc(V.Red, 60f, 220f, false, Offset(cx - midR, cy - midR), Size(midR * 2, midR * 2), style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round))
                    }
                    val a2 = Math.toRadians((r2 + 60).toDouble())
                    drawCircle(V.Red, 4.dp.toPx(), Offset(cx + midR * cos(a2).toFloat(), cy + midR * sin(a2).toFloat()))

                    // Inner — Amber
                    val innerR = cx * 0.45f
                    rotate(r3, Offset(cx, cy)) {
                        drawArc(V.Amber, 120f, 200f, false, Offset(cx - innerR, cy - innerR), Size(innerR * 2, innerR * 2), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                    }
                    val a3 = Math.toRadians((r3 + 120).toDouble())
                    drawCircle(V.Amber, 3.5.dp.toPx(), Offset(cx + innerR * cos(a3).toFloat(), cy + innerR * sin(a3).toFloat()))

                    // Core
                    drawCircle(V.Teal.copy(alpha = 0.08f), cx * 0.28f * pulse, Offset(cx, cy))
                    drawCircle(V.Teal, cx * 0.10f * pulse, Offset(cx, cy))
                    drawCircle(V.White, cx * 0.04f, Offset(cx, cy))
                }
            }

            Spacer(Modifier.weight(1f))

            // Google SSO button — premium dark style
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(V.Surface)
                    .border(1.dp, V.SurfaceBright.copy(alpha = 0.5f), RoundedCornerShape(9999.dp))
                    .clickable(onClick = onGoogleSignIn),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    // Google G colored
                    Canvas(Modifier.size(20.dp)) {
                        val s = size.width
                        // Blue
                        drawArc(Color(0xFF4285F4), -10f, 120f, true, Offset.Zero, Size(s, s))
                        // Green
                        drawArc(Color(0xFF34A853), 110f, 120f, true, Offset.Zero, Size(s, s))
                        // Yellow
                        drawArc(Color(0xFFFBBC05), 230f, 60f, true, Offset.Zero, Size(s, s))
                        // Red
                        drawArc(Color(0xFFEA4335), 290f, 60f, true, Offset.Zero, Size(s, s))
                        // White center
                        drawCircle(V.Surface, s * 0.32f, Offset(s / 2, s / 2))
                        // Blue right bar
                        drawRect(Color(0xFF4285F4), Offset(s * 0.5f, s * 0.35f), Size(s * 0.45f, s * 0.3f))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Continue with Google",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = V.White,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Terms
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = V.GrayDark)) { append("By continuing, you agree to our ") }
                    withStyle(SpanStyle(color = V.Teal)) { append("Terms of Service") }
                    withStyle(SpanStyle(color = V.GrayDark)) { append(" and ") }
                    withStyle(SpanStyle(color = V.Teal)) { append("Privacy Policy") }
                },
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
