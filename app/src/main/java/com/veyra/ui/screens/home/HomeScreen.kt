package com.veyra.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veyra.ui.components.GlassCard
import com.veyra.ui.components.GradientCard
import com.veyra.ui.components.MetricCard
import com.veyra.ui.components.ScoreRing
import com.veyra.ui.components.SectionHeader
import com.veyra.ui.components.scoreColor
import com.veyra.ui.theme.VeyraColors
import com.veyra.ui.theme.VeyraDimens
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VeyraColors.Accent)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VeyraColors.Background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = VeyraDimens.SpaceLg),
    ) {
        // Header
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VeyraDimens.SpaceLg)
                    .padding(top = VeyraDimens.SpaceXl),
            ) {
                Text(
                    text = state.greeting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeyraColors.TextTertiary,
                )
                Text(
                    text = "Your Health Today",
                    style = MaterialTheme.typography.displaySmall,
                    color = VeyraColors.TextPrimary,
                )
            }
        }

        Spacer(Modifier.height(VeyraDimens.SpaceXxl))

        // Score Rings
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800, 200)) + slideInVertically(tween(800, 200)) { 60 },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VeyraDimens.SpaceLg),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ScoreRing(
                    score = state.recoveryScore,
                    label = "Recovery",
                    color = scoreColor(state.recoveryScore),
                )
                ScoreRing(
                    score = state.sleepScore,
                    label = "Sleep",
                    color = scoreColor(state.sleepScore),
                )
                ScoreRing(
                    score = state.strainScore,
                    label = "Strain",
                    color = scoreColor(state.strainScore),
                )
            }
        }

        Spacer(Modifier.height(VeyraDimens.SpaceXxl))

        // Recovery insight card
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800, 400)) + slideInVertically(tween(800, 400)) { 60 },
        ) {
            Column(Modifier.padding(horizontal = VeyraDimens.SpaceLg)) {
                ScoreInsightCard(
                    title = "Recovery",
                    score = state.recoveryScore,
                    explanation = state.recoveryExplanation,
                    tip = state.recoveryTip,
                    color = VeyraColors.Recovery,
                )
            }
        }

        Spacer(Modifier.height(VeyraDimens.SpaceMd))

        // Sleep insight card
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800, 500)) + slideInVertically(tween(800, 500)) { 60 },
        ) {
            Column(Modifier.padding(horizontal = VeyraDimens.SpaceLg)) {
                ScoreInsightCard(
                    title = "Sleep",
                    score = state.sleepScore,
                    explanation = state.sleepExplanation,
                    tip = state.sleepTip,
                    color = VeyraColors.Sleep,
                )
            }
        }

        Spacer(Modifier.height(VeyraDimens.SpaceMd))

        // Strain insight card
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800, 600)) + slideInVertically(tween(800, 600)) { 60 },
        ) {
            Column(Modifier.padding(horizontal = VeyraDimens.SpaceLg)) {
                ScoreInsightCard(
                    title = "Strain",
                    score = state.strainScore,
                    explanation = state.strainExplanation,
                    tip = state.strainTip,
                    color = VeyraColors.Strain,
                )
            }
        }

        Spacer(Modifier.height(VeyraDimens.SpaceXxl))

        // Health Metrics
        SectionHeader(title = "Health Metrics")
        Spacer(Modifier.height(VeyraDimens.SpaceSm))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800, 700)) + slideInVertically(tween(800, 700)) { 60 },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg),
                verticalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceMd),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceMd),
                ) {
                    MetricCard(
                        icon = Icons.Rounded.DirectionsWalk,
                        label = "Steps",
                        value = "%,d".format(state.healthData.steps),
                        color = VeyraColors.Steps,
                        modifier = Modifier.weight(1f),
                        subtitle = "Goal: 10,000",
                    )
                    MetricCard(
                        icon = Icons.Rounded.LocalFireDepartment,
                        label = "Calories",
                        value = state.healthData.calories.toInt().toString(),
                        unit = "kcal",
                        color = VeyraColors.Calories,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceMd),
                ) {
                    MetricCard(
                        icon = Icons.Rounded.Favorite,
                        label = "Heart Rate",
                        value = state.healthData.heartRateAvg.toString(),
                        unit = "bpm",
                        color = VeyraColors.HeartRate,
                        modifier = Modifier.weight(1f),
                        subtitle = "${state.healthData.heartRateMin}-${state.healthData.heartRateMax} range",
                    )
                    MetricCard(
                        icon = Icons.Rounded.MonitorHeart,
                        label = "Resting HR",
                        value = state.healthData.restingHeartRate.toString(),
                        unit = "bpm",
                        color = VeyraColors.HeartRate,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceMd),
                ) {
                    MetricCard(
                        icon = Icons.Rounded.Psychology,
                        label = "HRV",
                        value = state.healthData.hrvRmssd.toInt().toString(),
                        unit = "ms",
                        color = VeyraColors.HRV,
                        modifier = Modifier.weight(1f),
                        subtitle = "RMSSD",
                    )
                    MetricCard(
                        icon = Icons.Rounded.Bedtime,
                        label = "Sleep",
                        value = "${state.healthData.sleepDurationMinutes / 60}h ${state.healthData.sleepDurationMinutes % 60}m",
                        color = VeyraColors.Sleep,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(VeyraDimens.SpaceXxl))
    }
}

@Composable
private fun ScoreInsightCard(
    title: String,
    score: Int,
    explanation: String,
    tip: String,
    color: androidx.compose.ui.graphics.Color,
) {
    GlassCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Spacer(Modifier.width(VeyraDimens.SpaceSm))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = color,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "$score/100",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                    ),
                    color = scoreColor(score),
                )
            }
            Spacer(Modifier.height(VeyraDimens.SpaceMd))
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = VeyraColors.TextSecondary,
            )
            Spacer(Modifier.height(VeyraDimens.SpaceSm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(VeyraDimens.RadiusMd))
                    .background(color.copy(alpha = 0.1f))
                    .padding(VeyraDimens.SpaceMd),
            ) {
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = color,
                )
            }
        }
    }
}
