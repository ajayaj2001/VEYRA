package com.veyra.ui.screens.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
    Column(
        Modifier
            .fillMaxSize()
            .background(V.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(80.dp))

        // Wordmark
        Text(
            text = "AURA",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
            ),
            color = V.White,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Your health. Connected.",
            style = MaterialTheme.typography.bodyMedium,
            color = V.Gray,
        )

        Spacer(Modifier.height(48.dp))

        // Abstract illustration
        Box(
            Modifier.size(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2
                val r = size.minDimension * 0.38f

                // Pulse circle
                drawCircle(V.Teal.copy(alpha = 0.06f), r * 1.2f, Offset(cx, cy))
                drawCircle(V.SurfaceBright.copy(alpha = 0.3f), r, Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1f))

                // Health dots
                val colors = listOf(V.Teal, V.Blue, V.Red, V.Amber)
                colors.forEachIndexed { i, c ->
                    val a = Math.toRadians((i * 90.0 - 45))
                    val dx = cx + r * cos(a).toFloat()
                    val dy = cy + r * sin(a).toFloat()
                    drawCircle(c.copy(alpha = 0.12f), 20f, Offset(dx, dy))
                    drawCircle(c, 8f, Offset(dx, dy))
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Google SSO button
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(V.Surface)
                .border(1.dp, V.SurfaceElevated, RoundedCornerShape(9999.dp))
                .clickable(onClick = onGoogleSignIn),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Google "G" placeholder
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(V.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("G", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = V.Black)
                }
                Text(
                    "Continue with Google",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = V.White,
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Terms
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = V.GrayDark)) {
                    append("By continuing, you agree to our ")
                }
                withStyle(SpanStyle(color = V.Teal)) {
                    append("Terms of Service")
                }
                withStyle(SpanStyle(color = V.GrayDark)) {
                    append(" and ")
                }
                withStyle(SpanStyle(color = V.Teal)) {
                    append("Privacy Policy")
                }
            },
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(24.dp))
    }
}
