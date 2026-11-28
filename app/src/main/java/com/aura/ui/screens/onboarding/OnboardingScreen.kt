package com.aura.ui.screens.onboarding

import android.content.Context
import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import com.aura.AppConfig
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.aura.ui.components.VButton
import com.aura.ui.theme.V
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

    Column(
        Modifier.fillMaxSize().background(V.Canvas)
            .statusBarsPadding().navigationBarsPadding(),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { page ->
            OnboardingPageContent(pages[page])
        }

        // Bottom controls
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PageIndicator(
                count = pages.size,
                current = pagerState.currentPage,
                modifier = Modifier.padding(bottom = 24.dp),
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
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        val screenHeight = maxHeight
        val illustrationSize = minOf(maxWidth, screenHeight * 0.44f, 320.dp)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(top = (screenHeight * 0.09f).coerceAtMost(64.dp), bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.displaySmall,
                color = V.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height((screenHeight * 0.12f).coerceAtMost(80.dp)))
            Box(Modifier.size(illustrationSize)) {
                AnimatedSvgView(assetPath = page.htmlAsset)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = page.body,
                style = MaterialTheme.typography.bodyMedium,
                color = V.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
private fun AnimatedSvgView(assetPath: String) {
    AndroidView(
        factory = { context -> IllustrationWebView(context, assetPath) },
        modifier = Modifier.fillMaxSize(),
        onRelease = { it.destroy() },
    )
}

private class IllustrationWebView(context: Context, assetPath: String) : WebView(context) {
    init {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        settings.javaScriptEnabled = true
        settings.blockNetworkLoads = true
        settings.useWideViewPort = true
        settings.setSupportZoom(false)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                updateIllustrationViewport()
            }
        }
        loadUrl("file:///android_asset/$assetPath")
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        updateIllustrationViewport()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = false

    private fun updateIllustrationViewport() {
        if (width > 0 && height > 0) {
            // Some WebViews report a layout viewport larger than the visible Android view.
            evaluateJavascript("if (window.setIllustrationViewport) window.setIllustrationViewport($width, $height)", null)
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
