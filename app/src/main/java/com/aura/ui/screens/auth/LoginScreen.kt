package com.aura.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.R
import com.aura.ui.components.AuraBrandLockup
import com.aura.ui.components.AuraOrbit
import com.aura.ui.theme.V
import com.aura.ui.theme.AuraScreenInset

private val SignInCaption = TextStyle(
    fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
    fontSize = 12.sp, lineHeight = 18.sp,
)

@Composable
fun LoginScreen(
    onGoogleSignIn: () -> Unit,
    signingIn: Boolean,
    error: String?,
    onDebugPreview: (() -> Unit)? = null,
) {
    var wordmarkTaps by remember { mutableIntStateOf(0) }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(V.Canvas)
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = AuraScreenInset),
        contentAlignment = Alignment.TopCenter,
    ) {
        val diameter = minOf(260.dp, maxWidth, maxHeight * 0.36f)
        val spacing = if (maxHeight < 560.dp) 16.dp else 26.dp
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).heightIn(min = maxHeight)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val wordmarkModifier = if (onDebugPreview != null && !signingIn) {
                Modifier.clickable(role = Role.Button) {
                    wordmarkTaps++
                    if (wordmarkTaps == 10) {
                        wordmarkTaps = 0
                        onDebugPreview()
                    }
                }
            } else {
                Modifier
            }
            AuraBrandLockup(wordmarkModifier)
            AuraOrbit(loading = false, diameter = diameter)
            if (error != null) Text(error, color = V.Error, style = SignInCaption, textAlign = TextAlign.Center)
            GoogleButton({
                wordmarkTaps = 0
                onGoogleSignIn()
            }, signingIn)
            Text(
                buildAnnotatedString {
                    append("By continuing, you agree to our\n")
                    withStyle(SpanStyle(color = V.Teal)) { append("Terms of Service") }
                    append(" and ")
                    withStyle(SpanStyle(color = V.Teal)) { append("Privacy Policy") }
                },
                style = SignInCaption,
                color = V.Gray,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GoogleButton(onClick: () -> Unit, signingIn: Boolean) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 58.dp).clip(shape)
            .background(V.SurfaceRaised).border(1.dp, V.SurfaceBright, shape)
            .clickable(enabled = !signingIn, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        Image(painterResource(R.drawable.auth_login_google_g_brand_colors), null, Modifier.size(24.dp))
        Text(
            if (signingIn) "Signing in…" else "Continue with Google",
            color = V.White,
            style = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
                fontSize = 18.sp, lineHeight = 24.sp),
            textAlign = TextAlign.Center,
        )
    }
}
