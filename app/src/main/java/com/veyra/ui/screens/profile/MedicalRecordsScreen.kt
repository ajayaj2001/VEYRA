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
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.veyra.ui.components.GlassCard
import com.veyra.ui.components.VeyraButton
import com.veyra.ui.theme.VeyraColors
import com.veyra.ui.theme.VeyraDimens

@Composable
fun MedicalRecordsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VeyraColors.Background)
            .verticalScroll(rememberScrollState())
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
                text = "Medical Records",
                style = MaterialTheme.typography.headlineSmall,
                color = VeyraColors.TextPrimary,
            )
        }

        Column(
            modifier = Modifier.padding(VeyraDimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceMd),
        ) {
            // Security notice
            GlassCard(
                gradientColors = listOf(
                    VeyraColors.Green.copy(alpha = 0.1f),
                    VeyraColors.Green.copy(alpha = 0.05f),
                ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = VeyraColors.Green,
                        modifier = Modifier.size(VeyraDimens.IconXl),
                    )
                    Spacer(Modifier.width(VeyraDimens.SpaceMd))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "End-to-End Encrypted",
                            style = MaterialTheme.typography.titleMedium,
                            color = VeyraColors.Green,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "All medical records are encrypted on your device using AES-256. Only you can access them.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeyraColors.TextTertiary,
                        )
                    }
                }
            }

            // Upload area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(VeyraDimens.CardRadius))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                VeyraColors.SurfaceElevated,
                                VeyraColors.Surface,
                            ),
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.CloudUpload,
                        contentDescription = null,
                        tint = VeyraColors.TextTertiary,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(VeyraDimens.SpaceMd))
                    Text(
                        text = "Upload Medical Records",
                        style = MaterialTheme.typography.titleMedium,
                        color = VeyraColors.TextSecondary,
                    )
                    Text(
                        text = "PDF, images, or documents",
                        style = MaterialTheme.typography.bodySmall,
                        color = VeyraColors.TextTertiary,
                    )
                }
            }

            VeyraButton(
                text = "Choose Files",
                onClick = { /* File picker placeholder */ },
            )

            Spacer(Modifier.height(VeyraDimens.SpaceMd))

            // Empty state
            GlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Rounded.Description,
                        contentDescription = null,
                        tint = VeyraColors.TextTertiary,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(VeyraDimens.SpaceMd))
                    Text(
                        text = "No Records Yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = VeyraColors.TextSecondary,
                    )
                    Text(
                        text = "Upload lab results, prescriptions, or other health documents to keep them secure and accessible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VeyraColors.TextTertiary,
                    )
                }
            }

            // Supported formats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceSm),
            ) {
                listOf("PDF", "JPG", "PNG", "HEIC").forEach { format ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(VeyraColors.SurfaceElevated)
                            .padding(horizontal = VeyraDimens.SpaceMd, vertical = VeyraDimens.SpaceXs),
                    ) {
                        Text(
                            text = format,
                            style = MaterialTheme.typography.labelSmall,
                            color = VeyraColors.TextTertiary,
                        )
                    }
                }
            }
        }
    }
}
