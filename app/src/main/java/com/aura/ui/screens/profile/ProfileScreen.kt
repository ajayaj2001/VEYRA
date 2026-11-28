package com.aura.ui.screens.profile

import com.aura.AppConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aura.navigation.Routes
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V

private data class MenuItem(
    val title: String,
    val subtitle: String,
    val color: Color,
    val route: String,
)

private val menuItems = listOf(
    MenuItem("Settings", "Nutrition goals, notifications, and health permissions", V.Teal, Routes.SETTINGS),
    MenuItem("Medical Records", "Encrypted health documents", V.Blue, Routes.MEDICAL),
    MenuItem("AI food estimation", "Gemini API key for photo and text meal logging", V.Amber, Routes.AI_SETTINGS),
)

@Composable
fun ProfileScreen(nav: NavController, profileName: String, onSignOut: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(V.Canvas)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = 100.dp),
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Profile",
                style = MaterialTheme.typography.headlineLarge,
                color = V.White,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                Icon(Icons.Rounded.Settings, "Settings", tint = V.Gray)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Avatar
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(V.Teal, V.Blue))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Person, null, tint = V.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(profileName.ifBlank { "${AppConfig.APP_NAME} User" }, style = MaterialTheme.typography.headlineMedium, color = V.White)
            Text("Premium Member", style = MaterialTheme.typography.labelMedium, color = V.Teal)
            Text("Member since Nov 2026", style = MaterialTheme.typography.labelSmall, color = V.Gray)
        }

        Spacer(Modifier.height(24.dp))

        // Stats row
        VCard(Modifier.padding(horizontal = AuraScreenInset)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatItem("Workouts", "47")
                Box(Modifier.size(width = 1.dp, height = 36.dp).background(V.SurfaceBright))
                StatItem("Streak", "5 days")
                Box(Modifier.size(width = 1.dp, height = 36.dp).background(V.SurfaceBright))
                StatItem("Avg Recovery", "78", V.Green)
            }
        }

        Spacer(Modifier.height(16.dp))

        // AI Coach card
        VCard(
            Modifier.padding(horizontal = AuraScreenInset),
            color = V.SurfaceRaised,
            onClick = { nav.navigate(Routes.COACH) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(V.Teal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✦", color = V.Teal)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("AI Health Coach", style = MaterialTheme.typography.titleLarge, color = V.White)
                    Text("Get personalized insights", style = MaterialTheme.typography.bodySmall, color = V.Gray)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = V.Gray)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Menu items
        Column(Modifier.padding(horizontal = AuraScreenInset)) {
            menuItems.forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate(item.route) }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(V.SurfaceRaised),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(item.color)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, color = V.White)
                        Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = V.Gray)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = V.Gray)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(V.SurfaceRaised))
            }
        }

        Spacer(Modifier.height(24.dp))

        // Sign out
        Text(
            "Sign Out",
            style = MaterialTheme.typography.labelLarge,
            color = V.Red,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSignOut)
                .padding(16.dp),
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, dotColor: Color? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = V.Gray)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = V.White)
            if (dotColor != null) {
                Spacer(Modifier.width(4.dp))
                Box(Modifier.size(6.dp).clip(CircleShape).background(dotColor))
            }
        }
    }
}
