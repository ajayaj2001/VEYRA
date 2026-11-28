package com.aura.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aura.ui.components.VCard
import com.aura.AppConfig
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.AuraWordmark
import com.aura.ui.theme.V
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(nav: NavController) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    Column(
        Modifier
            .fillMaxSize()
            .background(V.Canvas)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        // Header
        Appear(visible, 0) {
            Column(Modifier.padding(horizontal = AuraScreenInset, vertical = 16.dp)) {
                Text(AppConfig.APP_NAME, style = AuraWordmark, color = V.White)
                Text(greeting(), style = MaterialTheme.typography.bodyMedium, color = V.Gray)
            }
        }

        // Daily Activity card
        Appear(visible, 100) {
            VCard(Modifier.padding(horizontal = AuraScreenInset), featured = true) {
                Column {
                    Text("Daily activity", style = MaterialTheme.typography.titleSmall, color = V.Gray)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricBullet(V.Green, "8,452", "steps")
                            MetricBullet(V.Red, "72", "bpm")
                            MetricBullet(V.Amber, "1,847", "kcal")
                        }
                        ScoreRing(78, V.Teal, "Activity", 80.dp)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Steps card
        Appear(visible, 200) {
            VCard(Modifier.padding(horizontal = AuraScreenInset)) {
                Column {
                    Text("Steps", style = MaterialTheme.typography.titleSmall, color = V.Gray)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text("8,452", style = MaterialTheme.typography.displayLarge, color = V.White)
                            Text("steps", style = MaterialTheme.typography.bodyMedium, color = V.Gray)
                        }
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("1.52 km", style = MaterialTheme.typography.bodyMedium, color = V.Gray)
                            Text("61 kcal", style = MaterialTheme.typography.bodyMedium, color = V.Gray)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    ProgressBar(0.845f, V.Green)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0", style = MaterialTheme.typography.labelSmall, color = V.Gray)
                        Text("10,000", style = MaterialTheme.typography.labelSmall, color = V.Gray)
                    }
                    Spacer(Modifier.height(16.dp))
                    WeekBars(listOf(3200f, 5100f, 7800f, 4200f, 8500f, 6300f, 8452f), V.Green)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Score rings row
        Appear(visible, 300) {
            Row(Modifier.fillMaxWidth().padding(horizontal = AuraScreenInset), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VCard(Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        ScoreRing(78, V.Red, "Recovery", 72.dp)
                        Spacer(Modifier.height(4.dp))
                        Text("Good", style = MaterialTheme.typography.labelSmall, color = V.Red)
                    }
                }
                VCard(Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        ScoreRing(85, V.Blue, "Sleep", 72.dp)
                        Spacer(Modifier.height(4.dp))
                        Text("7h 12m", style = MaterialTheme.typography.labelSmall, color = V.Blue)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Recovery insight
        Appear(visible, 400) {
            VCard(Modifier.padding(horizontal = AuraScreenInset)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(V.Red))
                        Spacer(Modifier.width(8.dp))
                        Text("Recovery", style = MaterialTheme.typography.titleLarge, color = V.White)
                        Spacer(Modifier.weight(1f))
                        Text("78/100", style = MaterialTheme.typography.headlineMedium, color = V.Red)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Your HRV is above average and resting HR is stable. You're well-recovered for a moderate to hard workout today.",
                        style = MaterialTheme.typography.bodyMedium, color = V.Gray,
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(V.Teal.copy(alpha = 0.1f)).padding(12.dp)
                    ) {
                        Text(
                            "Go all out today \u2014 your body is primed for peak performance.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = V.Teal,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Metric cards
        Appear(visible, 500) {
            Column(Modifier.padding(horizontal = AuraScreenInset), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeartRateCard(Modifier.weight(1f))
                    SmallMetric("Resting HR", "58", "bpm", V.Red, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallMetric("HRV", "48", "ms", V.Teal, Modifier.weight(1f))
                    SmallMetric("Sleep", "7h 12m", "", V.SleepBlue, Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─── Sub-components ───

@Composable
private fun Appear(visible: Boolean, delay: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600, delay)) + slideInVertically(tween(600, delay)) { 50 },
    ) { content() }
}

@Composable
private fun MetricBullet(color: Color, value: String, unit: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.3f)))
        }
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = V.White)
        Spacer(Modifier.width(6.dp))
        Text(unit, style = MaterialTheme.typography.bodyMedium, color = V.Gray)
    }
}

@Composable
private fun SmallMetric(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    VCard(modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                }
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.titleSmall, color = V.Gray)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium, color = V.White)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(unit, style = MaterialTheme.typography.bodySmall, color = V.Gray)
                }
            }
        }
    }
}

@Composable
private fun HeartRateCard(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "hr")
    val phase by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "hrPhase",
    )

    VCard(modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(V.Red.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Box(Modifier.size(12.dp).clip(CircleShape).background(V.Red)) }
                Spacer(Modifier.width(8.dp))
                Text("Heart Rate", style = MaterialTheme.typography.titleSmall, color = V.Gray)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("72", style = MaterialTheme.typography.headlineMedium, color = V.White)
                Spacer(Modifier.width(4.dp))
                Text("bpm", style = MaterialTheme.typography.bodySmall, color = V.Gray)
            }
            Spacer(Modifier.height(8.dp))
            // ECG wave
            Canvas(Modifier.fillMaxWidth().height(32.dp)) {
                val w = size.width
                val h = size.height
                val mid = h / 2
                val offset = phase * w * 0.5f // Scrolling offset

                val path = Path()
                val segW = w / 5f // One heartbeat segment width

                path.moveTo(-offset, mid)

                // Draw repeating ECG pattern across width + extra for scrolling
                var x = -offset
                while (x < w + segW) {
                    // Flat baseline
                    path.lineTo(x + segW * 0.3f, mid)
                    // Small P wave
                    path.lineTo(x + segW * 0.35f, mid - h * 0.08f)
                    path.lineTo(x + segW * 0.4f, mid)
                    // Sharp QRS complex
                    path.lineTo(x + segW * 0.45f, mid + h * 0.1f) // Q dip
                    path.lineTo(x + segW * 0.5f, mid - h * 0.4f)  // R peak
                    path.lineTo(x + segW * 0.55f, mid + h * 0.15f) // S dip
                    // Return to baseline
                    path.lineTo(x + segW * 0.6f, mid)
                    // T wave
                    path.lineTo(x + segW * 0.7f, mid - h * 0.12f)
                    path.lineTo(x + segW * 0.8f, mid)
                    // Flat
                    path.lineTo(x + segW, mid)
                    x += segW
                }

                drawPath(
                    path = path,
                    color = V.Red.copy(alpha = 0.7f),
                    style = DrawStroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, color: Color) {
    val anim by animateFloatAsState(progress, tween(1000), label = "prog")
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(V.SurfaceBright)) {
        Box(Modifier.fillMaxWidth(anim).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}

@Composable
private fun ScoreRing(score: Int, color: Color, label: String, size: androidx.compose.ui.unit.Dp) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(score) { anim.animateTo(score / 100f, tween(1200, easing = FastOutSlowInEasing)) }

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 7.dp.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val tl = Offset(stroke / 2, stroke / 2)
            drawArc(V.SurfaceBright, -225f, 270f, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color, -225f, 270f * anim.value, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = V.White)
            Text(label, style = MaterialTheme.typography.labelSmall, color = V.Gray)
        }
    }
}

@Composable
private fun WeekBars(values: List<Float>, color: Color) {
    val max = values.max()
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Column {
        Canvas(Modifier.fillMaxWidth().height(60.dp)) {
            val barW = size.width / values.size
            val w = barW * 0.35f
            val halfW = w / 2f // For full capsule: radius = half width
            values.forEachIndexed { i, v ->
                val h = (v / max * size.height * 0.9f).coerceAtLeast(w) // Min height = width for pill shape
                val x = i * barW + (barW - w) / 2
                drawRoundRect(
                    color = if (i == values.lastIndex) color else color.copy(alpha = 0.4f),
                    topLeft = Offset(x, size.height - h),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(halfW, halfW), // Full capsule ends
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            days.forEachIndexed { i, d ->
                Text(d, style = MaterialTheme.typography.labelSmall,
                    color = if (i == days.lastIndex) color else V.Gray)
            }
        }
    }
}

private fun greeting(): String {
    val h = java.time.LocalTime.now().hour
    return when {
        h < 12 -> "Good morning"
        h < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}
