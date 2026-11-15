package com.veyra.ui.screens.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.veyra.ui.components.GlassCard
import com.veyra.ui.components.VeyraButton
import com.veyra.ui.components.VeyraOutlineButton
import com.veyra.ui.theme.VeyraColors
import com.veyra.ui.theme.VeyraDimens

@Composable
fun AiCoachScreen(navController: NavController) {
    var apiKey by remember { mutableStateOf("") }
    var isConfigured by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VeyraColors.Background)
            .statusBarsPadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VeyraDimens.SpaceSm, vertical = VeyraDimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = VeyraColors.TextPrimary)
            }
            Text(
                text = "AI Health Coach",
                style = MaterialTheme.typography.headlineSmall,
                color = VeyraColors.TextPrimary,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(VeyraDimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceMd),
        ) {
            if (!isConfigured) {
                // API key setup
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VeyraDimens.CardRadius))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    VeyraColors.Teal.copy(alpha = 0.15f),
                                    VeyraColors.Purple.copy(alpha = 0.1f),
                                ),
                            )
                        )
                        .padding(VeyraDimens.SpaceXxl),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Psychology,
                            contentDescription = null,
                            tint = VeyraColors.Teal,
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(Modifier.height(VeyraDimens.SpaceLg))
                        Text(
                            text = "AI Health Coach",
                            style = MaterialTheme.typography.headlineMedium,
                            color = VeyraColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(VeyraDimens.SpaceSm))
                        Text(
                            text = "Get personalized health insights, workout recommendations, and nutrition advice powered by AI.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VeyraColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg),
                        )
                    }
                }

                Spacer(Modifier.height(VeyraDimens.SpaceMd))

                GlassCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Key, null, tint = VeyraColors.Amber, modifier = Modifier.size(VeyraDimens.IconLg))
                            Spacer(Modifier.width(VeyraDimens.SpaceSm))
                            Text(
                                text = "Bring Your Own API Key",
                                style = MaterialTheme.typography.titleLarge,
                                color = VeyraColors.TextPrimary,
                            )
                        }
                        Spacer(Modifier.height(VeyraDimens.SpaceSm))
                        Text(
                            text = "Your API key is stored locally on your device and never sent to our servers. We support OpenAI, Anthropic, and other providers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeyraColors.TextTertiary,
                        )
                        Spacer(Modifier.height(VeyraDimens.SpaceMd))
                        TextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            placeholder = { Text("Enter your API key") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(VeyraDimens.CardRadiusSm),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = VeyraColors.Surface,
                                unfocusedContainerColor = VeyraColors.Surface,
                                focusedTextColor = VeyraColors.TextPrimary,
                                unfocusedTextColor = VeyraColors.TextPrimary,
                                cursorColor = VeyraColors.Green,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        )
                        Spacer(Modifier.height(VeyraDimens.SpaceMd))
                        VeyraButton(
                            text = "Connect",
                            enabled = apiKey.isNotBlank(),
                            onClick = { isConfigured = true },
                        )
                    }
                }
            } else {
                // Chat placeholder
                GlassCard {
                    Column {
                        Text(
                            text = "AI Coach Connected",
                            style = MaterialTheme.typography.titleLarge,
                            color = VeyraColors.Green,
                        )
                        Spacer(Modifier.height(VeyraDimens.SpaceSm))
                        Text(
                            text = "Your AI health coach is ready. This feature is coming in a future update with personalized insights based on your health data, workout history, and nutrition logs.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VeyraColors.TextSecondary,
                        )
                    }
                }

                // Quick suggestion chips
                listOf(
                    "How's my recovery today?",
                    "Suggest a workout for today",
                    "Am I eating enough protein?",
                    "Tips to improve my sleep",
                ).forEach { suggestion ->
                    VeyraOutlineButton(
                        text = suggestion,
                        onClick = { message = suggestion },
                        color = VeyraColors.Teal,
                    )
                }
            }
        }

        // Message input (visible when configured)
        if (isConfigured) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VeyraColors.Surface)
                    .padding(VeyraDimens.SpaceMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask your AI coach...") },
                    singleLine = true,
                    shape = RoundedCornerShape(VeyraDimens.PillRadius),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = VeyraColors.SurfaceElevated,
                        unfocusedContainerColor = VeyraColors.SurfaceElevated,
                        focusedTextColor = VeyraColors.TextPrimary,
                        unfocusedTextColor = VeyraColors.TextPrimary,
                        cursorColor = VeyraColors.Green,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                Spacer(Modifier.width(VeyraDimens.SpaceSm))
                IconButton(onClick = { /* Send message placeholder */ }) {
                    Icon(Icons.Rounded.Send, "Send", tint = VeyraColors.Green)
                }
            }
        }
    }
}
