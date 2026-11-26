package com.aura.ui.components

import android.animation.ValueAnimator
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.AppConfig
import com.aura.R
import com.aura.ui.theme.V

private val Sora = FontFamily(Font(R.font.sora_semibold, FontWeight.SemiBold))
private val HaloEasing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)

@Composable
internal fun AuraBrandLockup() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            AppConfig.APP_NAME,
            color = V.White,
            style = TextStyle(
                fontFamily = Sora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 44.sp,
                lineHeight = 56.sp,
                letterSpacing = (-1.3).sp,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            AppConfig.APP_TAGLINE,
            color = V.Gray,
            style = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 24.sp),
            textAlign = TextAlign.Center,
        )
    }
}

private data class OrbitRing(
    @DrawableRes val track: Int,
    val trackSize: Int,
    @DrawableRes val ring: Int,
    val turns: Int,
)

private data class OrbitLayers(
    val canvasSize: Int,
    val rings: List<OrbitRing>,
    @DrawableRes val halo: Int,
    val haloSize: Int,
    @DrawableRes val center: Int,
    val centerSize: Int,
    @DrawableRes val light: Int,
    val lightSize: Int,
)

private val LoadingLayers = OrbitLayers(
    canvasSize = 200,
    rings = listOf(
        OrbitRing(R.drawable.auth_loading_outer_orbit_track, 168, R.drawable.auth_loading_teal_ring, 2),
        OrbitRing(R.drawable.auth_loading_middle_orbit_track, 124, R.drawable.auth_loading_coral_ring, -1),
        OrbitRing(R.drawable.auth_loading_inner_orbit_track, 80, R.drawable.auth_loading_amber_ring, 3),
    ),
    halo = R.drawable.auth_loading_breathing_central_halo,
    haloSize = 46,
    center = R.drawable.auth_loading_aura_center,
    centerSize = 18,
    light = R.drawable.auth_loading_light_center,
    lightSize = 6,
)

private val LoginLayers = OrbitLayers(
    canvasSize = 260,
    rings = listOf(
        OrbitRing(R.drawable.auth_login_outer_orbit_track, 216, R.drawable.auth_login_teal_ring, 1),
        OrbitRing(R.drawable.auth_login_middle_orbit_track, 160, R.drawable.auth_login_coral_ring, -1),
        OrbitRing(R.drawable.auth_login_inner_orbit_track, 104, R.drawable.auth_login_amber_ring, 2),
    ),
    halo = R.drawable.auth_login_breathing_central_halo,
    haloSize = 56,
    center = R.drawable.auth_login_aura_center,
    centerSize = 22,
    light = R.drawable.auth_login_light_center,
    lightSize = 7,
)

@Composable
internal fun AuraOrbit(loading: Boolean, diameter: Dp) {
    val layers = if (loading) LoadingLayers else LoginLayers
    val phase by if (ValueAnimator.areAnimatorsEnabled()) {
        val transition = rememberInfiniteTransition(label = "Aura orbit")
        transition.animateFloat(
            0f,
            1f,
            infiniteRepeatable(tween(12_000, easing = LinearEasing)),
            label = "Orbit timeline",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    fun scaled(size: Int) = diameter * (size.toFloat() / layers.canvasSize)

    Box(Modifier.size(diameter).clipToBounds(), contentAlignment = Alignment.Center) {
        layers.rings.forEach { ring ->
            OrbitImage(ring.track, Modifier.size(scaled(ring.trackSize)))
            OrbitImage(ring.ring, Modifier.size(diameter).graphicsLayer {
                rotationZ = phase * ring.turns * 360f
            })
        }
        OrbitImage(layers.halo, Modifier.size(scaled(layers.haloSize)).graphicsLayer {
            // Four 3-second breaths, with one eased rise and fall per breath.
            val breath = (phase * 4f) % 1f
            val amount = HaloEasing.transform(1f - kotlin.math.abs(2f * breath - 1f))
            alpha = 0.1f + 0.1f * amount
            scaleX = 1f + 0.16f * amount
            scaleY = scaleX
        })
        OrbitImage(layers.center, Modifier.size(scaled(layers.centerSize)))
        OrbitImage(layers.light, Modifier.size(scaled(layers.lightSize)))
    }
}

@Composable
private fun OrbitImage(@DrawableRes image: Int, modifier: Modifier) {
    Image(painterResource(image), contentDescription = null, modifier = modifier)
}
