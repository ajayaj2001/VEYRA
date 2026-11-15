package com.veyra.ui.screens.nutrition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veyra.navigation.Screen
import com.veyra.ui.components.GlassCard
import com.veyra.ui.components.SectionHeader
import com.veyra.ui.components.VeyraButton
import com.veyra.ui.theme.VeyraColors
import com.veyra.ui.theme.VeyraDimens
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun NutritionScreen(
    navController: NavController,
    viewModel: NutritionViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VeyraColors.Background)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceMd),
    ) {
        item {
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
                        text = "Nutrition",
                        style = MaterialTheme.typography.displaySmall,
                        color = VeyraColors.TextPrimary,
                    )
                }
            }
        }

        // Date selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VeyraDimens.SpaceLg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.changeDate(state.date.minusDays(1)) }) {
                    Icon(Icons.Rounded.ChevronLeft, "Previous day", tint = VeyraColors.TextSecondary)
                }
                Text(
                    text = if (state.date == LocalDate.now()) "Today"
                    else state.date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                    style = MaterialTheme.typography.titleLarge,
                    color = VeyraColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = { viewModel.changeDate(state.date.plusDays(1)) }) {
                    Icon(Icons.Rounded.ChevronRight, "Next day", tint = VeyraColors.TextSecondary)
                }
            }
        }

        // Calorie ring + macros
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, 200)) + slideInVertically(tween(800, 200)) { 60 },
            ) {
                GlassCard(
                    modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Calorie ring
                        CalorieRing(
                            consumed = state.totalCalories,
                            goal = state.goal.calories,
                            modifier = Modifier.size(100.dp),
                        )
                        Spacer(Modifier.width(VeyraDimens.SpaceXl))
                        // Macro bars
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceSm),
                        ) {
                            MacroBar("Protein", state.totalProtein, state.goal.protein, VeyraColors.Accent)
                            MacroBar("Carbs", state.totalCarbs, state.goal.carbs, VeyraColors.Amber)
                            MacroBar("Fat", state.totalFat, state.goal.fat, VeyraColors.Coral)
                        }
                    }
                }
            }
        }

        // Meal sections
        val mealTypes = listOf("breakfast" to "Breakfast", "lunch" to "Lunch", "dinner" to "Dinner", "snack" to "Snacks")
        mealTypes.forEach { (type, label) ->
            val mealLogs = state.foodLogs.filter { it.log.mealType == type }
            item {
                SectionHeader(
                    title = label,
                    action = "Add",
                    onAction = {
                        viewModel.setMealType(type)
                        navController.navigate(Screen.FoodEntry.createRoute())
                    },
                )
            }

            if (mealLogs.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = VeyraDimens.SpaceLg)
                            .clip(RoundedCornerShape(VeyraDimens.RadiusMd))
                            .background(VeyraColors.SurfaceElevated)
                            .clickable {
                                viewModel.setMealType(type)
                                navController.navigate(Screen.FoodEntry.createRoute())
                            }
                            .padding(VeyraDimens.SpaceLg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = VeyraColors.TextTertiary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(VeyraDimens.SpaceSm))
                        Text("Add food", style = MaterialTheme.typography.bodyMedium, color = VeyraColors.TextTertiary)
                    }
                }
            }

            items(mealLogs) { logWithItem ->
                GlassCard(
                    modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = logWithItem.foodItem.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = VeyraColors.TextPrimary,
                            )
                            Text(
                                text = "${"%.0f".format(logWithItem.log.servings)} serving · ${"%.0f".format(logWithItem.foodItem.calories * logWithItem.log.servings)} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = VeyraColors.TextTertiary,
                            )
                            Text(
                                text = "P: ${"%.0f".format(logWithItem.foodItem.protein * logWithItem.log.servings)}g  C: ${"%.0f".format(logWithItem.foodItem.carbs * logWithItem.log.servings)}g  F: ${"%.0f".format(logWithItem.foodItem.fat * logWithItem.log.servings)}g",
                                style = MaterialTheme.typography.labelSmall,
                                color = VeyraColors.TextTertiary,
                            )
                        }
                        IconButton(onClick = { viewModel.deleteFoodLog(logWithItem.log) }) {
                            Icon(Icons.Rounded.Delete, "Delete", tint = VeyraColors.TextTertiary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(VeyraDimens.SpaceXxl)) }
    }
}

@Composable
private fun CalorieRing(
    consumed: Double,
    goal: Double,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = (consumed / goal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1.5f),
        animationSpec = tween(1000),
        label = "calProgress",
    )
    val remaining = (goal - consumed).coerceAtLeast(0.0)
    val color = if (consumed > goal) VeyraColors.Coral else VeyraColors.Mint

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 10.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)

            drawArc(
                color = VeyraColors.SurfaceElevated,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceAtMost(1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${remaining.toInt()}",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = color,
            )
            Text(
                text = "kcal left",
                style = MaterialTheme.typography.labelSmall,
                color = VeyraColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun MacroBar(
    label: String,
    current: Double,
    goal: Double,
    color: Color,
) {
    val progress by animateFloatAsState(
        targetValue = (current / goal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "macroProgress",
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = VeyraColors.TextTertiary)
            Text(
                "${"%.0f".format(current)} / ${"%.0f".format(goal)}g",
                style = MaterialTheme.typography.labelSmall,
                color = VeyraColors.TextSecondary,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(VeyraColors.SurfaceElevated),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}
