package com.veyra.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.veyra.navigation.Screen
import com.veyra.ui.components.GlassCard
import com.veyra.ui.components.GradientCard
import com.veyra.ui.theme.VeyraColors
import com.veyra.ui.theme.VeyraDimens
import kotlinx.coroutines.delay

@Composable
fun ProfileScreen(navController: NavController) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VeyraColors.Background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = VeyraDimens.SpaceXxl),
    ) {
        // Profile header
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VeyraDimens.SpaceLg)
                    .padding(top = VeyraDimens.SpaceXl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(VeyraColors.Teal, VeyraColors.Green),
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(Modifier.height(VeyraDimens.SpaceMd))
                Text(
                    text = "Veyra User",
                    style = MaterialTheme.typography.headlineMedium,
                    color = VeyraColors.TextPrimary,
                )
                Text(
                    text = "Premium Member",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeyraColors.Green,
                )
            }
        }

        Spacer(Modifier.height(VeyraDimens.SpaceXxl))

        // AI Coach card
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800, 200)) + slideInVertically(tween(800, 200)) { 60 },
        ) {
            GradientCard(
                modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg),
                startColor = VeyraColors.Teal,
                endColor = VeyraColors.Purple,
                onClick = { navController.navigate(Screen.AiCoach.route) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(VeyraDimens.IconXl),
                    )
                    Spacer(Modifier.width(VeyraDimens.SpaceMd))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "AI Health Coach",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Get personalized insights and recommendations",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }

        Spacer(Modifier.height(VeyraDimens.SpaceLg))

        // Menu items
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800, 400)) + slideInVertically(tween(800, 400)) { 60 },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg),
                verticalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceSm),
            ) {
                ProfileMenuItem(
                    icon = Icons.Rounded.Description,
                    title = "Medical Records",
                    subtitle = "Encrypted health documents",
                    onClick = { navController.navigate(Screen.MedicalRecords.route) },
                )
                ProfileMenuItem(
                    icon = Icons.Rounded.FitnessCenter,
                    title = "Workout Preferences",
                    subtitle = "Default sets, rest timer, units",
                    onClick = { /* Settings placeholder */ },
                )
                ProfileMenuItem(
                    icon = Icons.Rounded.LocalFireDepartment,
                    title = "Nutrition Goals",
                    subtitle = "Calorie and macro targets",
                    onClick = { /* Goals placeholder */ },
                )
                ProfileMenuItem(
                    icon = Icons.Rounded.Lock,
                    title = "Health Connect",
                    subtitle = "Manage data permissions",
                    onClick = { /* Health connect settings */ },
                )
                ProfileMenuItem(
                    icon = Icons.Rounded.Settings,
                    title = "App Settings",
                    subtitle = "Theme, notifications, data",
                    onClick = { /* Settings placeholder */ },
                )
                ProfileMenuItem(
                    icon = Icons.Rounded.Info,
                    title = "About Veyra",
                    subtitle = "Version 1.0.0",
                    onClick = { },
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    GlassCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = VeyraColors.Green,
                modifier = Modifier.size(VeyraDimens.IconLg),
            )
            Spacer(Modifier.width(VeyraDimens.SpaceMd))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = VeyraColors.TextPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = VeyraColors.TextTertiary,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = VeyraColors.TextTertiary,
            )
        }
    }
}
