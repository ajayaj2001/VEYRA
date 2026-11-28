package com.aura.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V

@Composable
fun MedicalScreen(nav: NavController) {
    Column(
        Modifier.fillMaxSize().background(V.Canvas).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, "Back", tint = V.White) }
            Text("Medical Records", style = MaterialTheme.typography.titleLarge, color = V.White)
        }

        Column(Modifier.padding(horizontal = AuraScreenInset, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Security banner
            VCard(color = V.Teal.copy(alpha = 0.06f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shield, null, tint = V.Teal, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("End-to-End Encrypted", style = MaterialTheme.typography.titleMedium, color = V.Teal, fontWeight = FontWeight.Bold)
                        Text("AES-256 on device. Only you can access.", style = MaterialTheme.typography.bodySmall, color = V.Gray)
                    }
                }
            }

            // Upload area
            Box(
                Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(20.dp))
                    .background(V.SurfaceRaised),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CloudUpload, null, tint = V.Gray, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Upload Medical Records", style = MaterialTheme.typography.titleMedium, color = V.Gray)
                    Text("PDF, images, or documents", style = MaterialTheme.typography.bodySmall, color = V.Gray)
                }
            }

            AuraPrimaryButton("Choose Files", onClick = { /* File picker */ })

            // Format badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("PDF", "JPG", "PNG", "HEIC").forEach { f ->
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(V.SurfaceRaised).padding(horizontal = 10.dp, vertical = 4.dp)
                    ) { Text(f, style = MaterialTheme.typography.labelSmall, color = V.Gray) }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Empty state
            VCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Description, null, tint = V.Gray, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("No Records Yet", style = MaterialTheme.typography.titleMedium, color = V.Gray)
                    Text("Upload lab results, prescriptions, or health documents.", style = MaterialTheme.typography.bodySmall, color = V.Gray)
                }
            }
        }
    }
}
