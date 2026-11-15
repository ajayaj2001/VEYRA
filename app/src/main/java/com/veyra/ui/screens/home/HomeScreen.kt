package com.veyra.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        Box(Modifier.fillMaxSize().background(VeyraColors.Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VeyraColors.Green)
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
        // Samsung Health-style header
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VeyraDimens.ScreenPadding)
                    .padding(top = VeyraDimens.SpaceXl),
            ) {
                Text(
                    text = "Veyra",
                    style = MaterialTheme.typography.headlineLarge,
                    color = VeyraColors.TextPrimary,
                )
                Spacer(Modifier.height(VeyraDimens.SpaceXs))
                Text(
                    text = state.greeting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeyraColors.TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(VeyraDimens.SpaceXxl))

        // Daily Activity card — Samsung style with colored dot bullets
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800, 200)) + slideInVertically(tween(800, 200)) { 60 },
        ) {
            GlassCard(modifier = Modifier.padding(horizontal = VeyraDimens.ScreenPadding)) {
                Column {
                    Text(
                        text = "Daily activity",
                        style = MaterialTheme.typography.titleSmall,
                        color = VeyraColors.TextSecondary,
                    )
                    Spacer(Modifier.height(VeyraDimens.SpaceLg))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceMd)) {
                            // Steps with green dot
                            DailyMetricRow(
                                color = VeyraColors.Steps,
                                value = "%,d".format(state.healthData.steps),
                                unit = "steps",
                            )
                            // Heart rate with red dot
                            DailyMetricRow(
                                color = VeyraColors.HeartRate,
                                value = state.healthData.heartRateAvg.toString(),
                                unit = "bpm",
                            )
                            // Calories with purple dot
                            DailyMetricRow(
                                color = VeyraColors.Calories,
                                value = state.healthData.calories.toInt().toString(),
                                unit = "kcal",
                            )
                        }
                        // Activity rings (simplified)
                        ScoreRing(
                            score = state.strainScore,
                            label = "Activity",
                            color = VeyraColors.Green,
                            size = 90.dp,
                            strokeWidth = 8.dp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(VeyraDimens.CardGap))

        // Steps card — Samsung style with progress bar and chart
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800, 300)) + slideInVertically(tween(800, 300)) { 60 },
        ) {
            GlassCard(modifier = Modifier.padding(horizontal = VeyraDimens.ScreenPadding)) {
                Column {
                    Text(
                        text = "Steps",
                        style = MaterialTheme.typography.titleSmall,
                        color = VeyraColors.TextSecondary,
                    )
                    Spacer(Modifier.height(VeyraDimens.SpaceSm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column {
                            Text(
                                text = "%,d".format(state.healthData.steps),
                                style = MaterialTheme.typography.displayLarge,
                                color = VeyraColors.TextPrimary,
                            )
                            Text(
                                text = "steps",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VeyraColors.TextSecondary,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "1.52 km",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VeyraColors.TextSecondary,
                            )
                            Text(
                                text = "${state.healthData.calories.toInt()} kcal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VeyraColors.TextSecondary,
                            )
                        }
                    }
                    Spacer(Modifier.height(VeyraDimens.SpaceLg))
                    // Progress bar — Samsung style
                    StepsProgressBar(
                        current = state.healthData.steps,
                        goal = 10000,
                    )
                    Spacer(Modifier.height(VeyraDimens.SpaceLg))
                    // Mini bar chart
                    WeeklyBarChart(
                        values = listOf(3200f, 5100f, 7800f, 4200f, 8500f, 6300f, state.healthData.steps.toFloat()),
                        color = VeyraColors.Green,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(VeyraDimens.CardGap))

        // Score cards row
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800, 400)) + slideInVertically(tween(800, 400)) { 60 },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VeyraDimens.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(VeyraDimens.CardGap),
            ) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ScoreRing(
                            score = state.recoveryScore,
                            label = "Recovery",
                            color = scoreColor(state.recoveryScore),
                            size = 80.dp,
                            strokeWidth = 7.dp,
                        )
                    }
                }
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ScoreRing(
                            score = state.sleepScore,
                            label = "Sleep",
                            color = scoreColor(state.sleepScore),
                            size = 80.dp,
                            strokeWidth = 7.dp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(VeyraDimens.CardGap))

        // Recovery insight
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800, 500)) + slideInVertically(tween(800, 500)) { 60 },
        ) {
            GlassCard(modifier = Modifier.padding(horizontal = VeyraDimens.ScreenPadding)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(scoreColor(state.recoveryScore)),
                        )
                        Spacer(Modifier.width(VeyraDimens.SpaceSm))
                        Text(
                            text = "Recovery",
                            style = MaterialTheme.typography.titleLarge,
                            color = VeyraColors.TextPrimary,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${state.recoveryScore}/100",
                            style = MaterialTheme.typography.headlineMedium,
                            color = scoreColor(state.recoveryScore),
                        )
                    }
                    Spacer(Modifier.height(VeyraDimens.SpaceMd))
                    Text(
                        text = state.recoveryExplanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeyraColors.TextSecondary,
                    )
                    Spacer(Modifier.height(VeyraDimens.SpaceSm))
                    // Coaching tip — Samsung-style subtle green tint
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(VeyraDimens.CardRadiusSm))
                            .background(VeyraColors.GreenSoft)
                            .padding(VeyraDimens.SpaceMd),
                    ) {
                        Text(
                            text = state.recoveryTip,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = VeyraColors.Green,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(VeyraDimens.CardGap))

        // Sleep insight
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800, 600)) + slideInVertically(tween(800, 600)) { 60 },
        ) {
            GlassCard(modifier = Modifier.padding(horizontal = VeyraDimens.ScreenPadding)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(VeyraColors.Sleep),
                        )
                        Spacer(Modifier.width(VeyraDimens.SpaceSm))
                        Text("Sleep", style = MaterialTheme.typography.titleLarge, color = VeyraColors.TextPrimary)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${state.healthData.sleepDurationMinutes / 60}h ${state.healthData.sleepDurationMinutes % 60}m",
                            style = MaterialTheme.typography.headlineMedium,
                            color = VeyraColors.Sleep,
                        )
                    }
                    Spacer(Modifier.height(VeyraDimens.SpaceMd))
                    Text(state.sleepExplanation, style = MaterialTheme.typography.bodyMedium, color = VeyraColors.TextSecondary)
                    Spacer(Modifier.height(VeyraDimens.SpaceSm))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(VeyraDimens.CardRadiusSm))
                            .background(VeyraColors.Blue.copy(alpha = 0.12f))
                            .padding(VeyraDimens.SpaceMd),
                    ) {
                        Text(state.sleepTip, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = VeyraColors.Blue)
                    }
                }
            }
        }

        Spacer(Modifier.height(VeyraDimens.SpaceXxl))

        // Health Metrics grid
        SectionHeader(title = "Other data from this period")
        Spacer(Modifier.height(VeyraDimens.SpaceSm))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800, 700)) + slideInVertically(tween(800, 700)) { 60 },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = VeyraDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(VeyraDimens.CardGap),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VeyraDimens.CardGap),
                ) {
                    MetricCard(
                        icon = Icons.Rounded.Favorite,
                        label = "Heart Rate",
                        value = state.healthData.heartRateAvg.toString(),
                        unit = "bpm",
                        color = VeyraColors.HeartRate,
                        modifier = Modifier.weight(1f),
                        subtitle = "${state.healthData.heartRateMin}-${state.healthData.heartRateMax}",
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
                    horizontalArrangement = Arrangement.spacedBy(VeyraDimens.CardGap),
                ) {
                    MetricCard(
                        icon = Icons.Rounded.Psychology,
                        label = "HRV",
                        value = state.healthData.hrvRmssd.toInt().toString(),
                        unit = "ms",
                        color = VeyraColors.HRV,
                        modifier = Modifier.weight(1f),
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
private fun DailyMetricRow(
    color: Color,
    value: String,
    unit: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            // Small inner circle for Samsung bullet style
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f)),
            )
        }
        Spacer(Modifier.width(VeyraDimens.SpaceMd))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = VeyraColors.TextPrimary,
        )
        Spacer(Modifier.width(VeyraDimens.SpaceSm))
        Text(
            text = unit,
            style = MaterialTheme.typography.bodyMedium,
            color = VeyraColors.TextSecondary,
        )
    }
}

@Composable
private fun StepsProgressBar(
    current: Long,
    goal: Int,
) {
    val progress by animateFloatAsState(
        targetValue = (current.toFloat() / goal).coerceIn(0f, 1f),
        animationSpec = tween(1000),
        label = "stepsProgress",
    )

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(VeyraDimens.ProgressHeight)
                .clip(RoundedCornerShape(VeyraDimens.ProgressRadius))
                .background(VeyraColors.ProgressTrack),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(VeyraDimens.ProgressHeight)
                    .clip(RoundedCornerShape(VeyraDimens.ProgressRadius))
                    .background(VeyraColors.ProgressFill),
            )
        }
        Spacer(Modifier.height(VeyraDimens.SpaceXs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("0", style = MaterialTheme.typography.labelSmall, color = VeyraColors.TextTertiary)
            Text("%,d".format(goal), style = MaterialTheme.typography.labelSmall, color = VeyraColors.TextTertiary)
        }
    }
}

@Composable
private fun WeeklyBarChart(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val maxVal = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val barSpacing = size.width / values.size
            val barWidth = barSpacing * 0.45f

            values.forEachIndexed { index, value ->
                val barHeight = (value / maxVal * size.height * 0.85f)
                val x = index * barSpacing + (barSpacing - barWidth) / 2
                drawRoundRect(
                    color = if (index == values.lastIndex) color else color.copy(alpha = 0.5f),
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }
        }
        Spacer(Modifier.height(VeyraDimens.SpaceXs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            days.forEachIndexed { index, day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index == days.lastIndex) VeyraColors.Green else VeyraColors.TextTertiary,
                )
            }
        }
    }
}
