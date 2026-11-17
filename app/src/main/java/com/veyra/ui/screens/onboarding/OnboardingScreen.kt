package com.veyra.ui.screens.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veyra.ui.components.VButton
import com.veyra.ui.theme.V
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private data class OnboardingPage(
    val title: String,
    val body: String,
    val colors: List<Color>,
)

private val pages = listOf(
    OnboardingPage(
        title = "Meet Veyra",
        body = "Sleep, training, nutrition, and recovery\u2014all connected in one intelligent health companion.",
        colors = listOf(V.Teal, V.Blue, V.Amber, V.Red),
    ),
    OnboardingPage(
        title = "Understand your sleep",
        body = "Track sleep stages, HRV, and wake patterns. See how your rest impacts recovery.",
        colors = listOf(V.Blue, V.SkyBlue, Color(0xFF9B8EC4)),
    ),
    OnboardingPage(
        title = "Train smarter",
        body = "Log workouts, track progress, and watch your strength grow over time.",
        colors = listOf(V.Green, V.Teal),
    ),
    OnboardingPage(
        title = "Fuel your body",
        body = "Track calories and macros. Build meals. Hit your nutrition goals effortlessly.",
        colors = listOf(V.Amber, V.Green, V.Teal),
    ),
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(V.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            OnboardingPageContent(pages[page])
        }

        // Bottom controls
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Page dots
            PageIndicator(
                count = pages.size,
                current = pagerState.currentPage,
                modifier = Modifier.padding(bottom = 32.dp),
            )

            // Button
            VButton(
                text = if (pagerState.currentPage == pages.lastIndex) "Get Started" else "Continue",
                onClick = {
                    if (pagerState.currentPage == pages.lastIndex) {
                        onComplete()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(80.dp))

        // Abstract illustration
        Box(
            Modifier.size(260.dp),
            contentAlignment = Alignment.Center,
        ) {
            OnboardingIllustration(page.colors)
        }

        Spacer(Modifier.height(48.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.displaySmall,
            color = V.White,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyMedium,
            color = V.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(140.dp)) // Space for bottom controls
    }
}

@Composable
private fun OnboardingIllustration(colors: List<Color>) {
    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = size.minDimension * 0.35f

        // Outer ring
        drawCircle(
            color = V.SurfaceBright.copy(alpha = 0.3f),
            radius = radius + 20f,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f),
        )

        // Inner ring
        drawCircle(
            color = V.SurfaceBright.copy(alpha = 0.15f),
            radius = radius * 0.5f,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1f),
        )

        // Center pulse
        drawCircle(
            color = colors.firstOrNull()?.copy(alpha = 0.08f) ?: V.Teal.copy(alpha = 0.08f),
            radius = radius * 0.4f,
            center = Offset(cx, cy),
        )

        // Orbiting dots
        colors.forEachIndexed { i, color ->
            val angle = (i * 360f / colors.size) - 90f
            val rad = Math.toRadians(angle.toDouble())
            val dotX = cx + radius * cos(rad).toFloat()
            val dotY = cy + radius * sin(rad).toFloat()

            // Glow
            drawCircle(
                color = color.copy(alpha = 0.15f),
                radius = 24f,
                center = Offset(dotX, dotY),
            )
            // Dot
            drawCircle(
                color = color,
                radius = 10f,
                center = Offset(dotX, dotY),
            )
        }

        // Connecting dotted arcs (subtle)
        val dotCount = 40
        for (j in 0 until dotCount) {
            val angle = (j * 360f / dotCount)
            val rad = Math.toRadians(angle.toDouble())
            val dx = cx + (radius + 20f) * cos(rad).toFloat()
            val dy = cy + (radius + 20f) * sin(rad).toFloat()
            if (j % 3 == 0) {
                drawCircle(
                    color = V.SurfaceBright.copy(alpha = 0.2f),
                    radius = 1.5f,
                    center = Offset(dx, dy),
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val width by animateFloatAsState(
                targetValue = if (i == current) 24f else 6f,
                animationSpec = tween(300),
                label = "dot",
            )
            Box(
                Modifier
                    .height(6.dp)
                    .size(width.dp, 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i == current) V.Teal else V.SurfaceBright)
            )
        }
    }
}
