package com.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.ui.components.AuraBrandLockup
import com.aura.ui.components.AuraOrbit
import com.aura.ui.theme.V
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        delay(3_000)
        finish()
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(V.Canvas)
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val diameter = minOf(200.dp, maxWidth, maxHeight * 0.55f)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AuraOrbit(loading = true, diameter = diameter)
            Spacer(Modifier.height(32.dp))
            AuraBrandLockup()
        }
    }
}
