package com.veyra.ui.screens.onboarding

import android.webkit.WebView
import android.webkit.WebViewClient
import com.veyra.AppConfig
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.veyra.ui.components.VButton
import com.veyra.ui.theme.V
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val body: String,
    val htmlAsset: String,
)

private val pages = listOf(
    OnboardingPage(
        title = "Meet ${AppConfig.APP_NAME}",
        body = "Sleep, training, nutrition, and recovery\u2014all connected in one intelligent health companion.",
        htmlAsset = "onboarding/page1.html",
    ),
    OnboardingPage(
        title = "Understand your sleep",
        body = "Track sleep stages, HRV, and wake patterns. See how your rest impacts recovery.",
        htmlAsset = "onboarding/page2.html",
    ),
    OnboardingPage(
        title = "Train smarter",
        body = "Log workouts, track progress, and watch your strength grow over time.",
        htmlAsset = "onboarding/page3.html",
    ),
    OnboardingPage(
        title = "Fuel your body",
        body = "Track calories and macros. Build meals. Hit your nutrition goals effortlessly.",
        htmlAsset = "onboarding/page4.html",
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
            PageIndicator(
                count = pages.size,
                current = pagerState.currentPage,
                modifier = Modifier.padding(bottom = 32.dp),
            )

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
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth

        // Responsive sizes based on screen height
        val illustrationSize = (screenHeight * 0.35f).coerceAtMost(320.dp)
        val titleTopPadding = screenHeight * 0.08f
        val illustrationOffset = -(screenHeight * 0.06f)
        val textOffset = illustrationSize * 0.45f

        // Title pinned to top
        Text(
            text = page.title,
            style = MaterialTheme.typography.displaySmall,
            color = V.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = titleTopPadding)
                .align(Alignment.TopCenter),
        )

        // Illustration centered — responsive offset
        Box(
            Modifier
                .size(illustrationSize)
                .align(Alignment.Center)
                .offset(y = illustrationOffset),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedSvgView(assetPath = page.htmlAsset)
        }

        // Body text below illustration
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyMedium,
            color = V.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .offset(y = textOffset)
                .padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun AnimatedSvgView(assetPath: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webViewClient = WebViewClient()
                loadUrl("file:///android_asset/$assetPath")
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
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
