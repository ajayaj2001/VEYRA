package com.veyra.ui.screens.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veyra.navigation.Screen
import com.veyra.ui.components.GlassCard
import com.veyra.ui.components.GradientCard
import com.veyra.ui.components.SectionHeader
import com.veyra.ui.components.VeyraButton
import com.veyra.ui.components.VeyraOutlineButton
import com.veyra.ui.theme.VeyraColors
import com.veyra.ui.theme.VeyraDimens
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutScreen(
    navController: NavController,
    viewModel: WorkoutViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var visible by remember { mutableStateOf(false) }
    var showNewRoutineDialog by remember { mutableStateOf(false) }

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
                        text = "Workout",
                        style = MaterialTheme.typography.displaySmall,
                        color = VeyraColors.TextPrimary,
                    )
                    Text(
                        text = "${state.totalWorkouts} workouts completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeyraColors.TextTertiary,
                    )
                }
            }
        }

        // Active workout banner
        if (state.activeWorkout != null) {
            item {
                GradientCard(
                    modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg),
                    startColor = VeyraColors.Coral,
                    endColor = VeyraColors.Teal,
                    onClick = {
                        navController.navigate(Screen.WorkoutActive.createRoute())
                    },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(VeyraDimens.IconXl),
                        )
                        Spacer(Modifier.width(VeyraDimens.SpaceMd))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Workout in Progress",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = state.activeWorkout!!.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = "Resume",
                            tint = Color.White,
                            modifier = Modifier.size(VeyraDimens.IconXl),
                        )
                    }
                }
            }
        }

        // Quick start
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, 200)) + slideInVertically(tween(800, 200)) { 60 },
            ) {
                Column(Modifier.padding(horizontal = VeyraDimens.SpaceLg)) {
                    Spacer(Modifier.height(VeyraDimens.SpaceSm))
                    VeyraButton(
                        text = "Start Empty Workout",
                        onClick = {
                            viewModel.startEmptyWorkout()
                            navController.navigate(Screen.WorkoutActive.createRoute())
                        },
                    )
                }
            }
        }

        // Routines section
        item {
            SectionHeader(
                title = "My Routines",
                action = "New",
                onAction = { showNewRoutineDialog = true },
            )
        }

        if (state.routines.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Rounded.FitnessCenter,
                            contentDescription = null,
                            tint = VeyraColors.TextTertiary,
                            modifier = Modifier.size(VeyraDimens.IconHuge),
                        )
                        Spacer(Modifier.height(VeyraDimens.SpaceMd))
                        Text(
                            text = "No routines yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = VeyraColors.TextSecondary,
                        )
                        Text(
                            text = "Create a routine to quickly start workouts",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeyraColors.TextTertiary,
                        )
                    }
                }
            }
        }

        items(state.routines) { routine ->
            GlassCard(
                modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg),
                onClick = {
                    navController.navigate(Screen.WorkoutActive.createRoute(routine.id))
                },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = routine.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = VeyraColors.TextPrimary,
                        )
                        if (routine.description.isNotEmpty()) {
                            Text(
                                text = routine.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = VeyraColors.TextTertiary,
                            )
                        }
                    }
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Start",
                        tint = VeyraColors.Green,
                    )
                }
            }
        }

        // Recent workouts
        if (state.recentWorkouts.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recent Workouts",
                    action = "See All",
                    onAction = { navController.navigate(Screen.WorkoutHistory.route) },
                )
            }

            items(state.recentWorkouts) { workout ->
                GlassCard(
                    modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = null,
                            tint = VeyraColors.TextTertiary,
                            modifier = Modifier.size(VeyraDimens.IconMd),
                        )
                        Spacer(Modifier.width(VeyraDimens.SpaceMd))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = workout.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = VeyraColors.TextPrimary,
                            )
                            Text(
                                text = formatWorkoutDate(workout.startTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = VeyraColors.TextTertiary,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatDuration(workout.durationSeconds),
                                style = MaterialTheme.typography.labelMedium,
                                color = VeyraColors.Green,
                            )
                            Text(
                                text = "%.0f kg".format(workout.totalVolume),
                                style = MaterialTheme.typography.bodySmall,
                                color = VeyraColors.TextTertiary,
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(VeyraDimens.SpaceXxl)) }
    }

    // New routine dialog
    if (showNewRoutineDialog) {
        var routineName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewRoutineDialog = false },
            containerColor = VeyraColors.SurfaceElevated,
            title = {
                Text("New Routine", color = VeyraColors.TextPrimary)
            },
            text = {
                TextField(
                    value = routineName,
                    onValueChange = { routineName = it },
                    placeholder = { Text("Routine name") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = VeyraColors.Surface,
                        unfocusedContainerColor = VeyraColors.Surface,
                        focusedTextColor = VeyraColors.TextPrimary,
                        unfocusedTextColor = VeyraColors.TextPrimary,
                        cursorColor = VeyraColors.Green,
                        focusedIndicatorColor = VeyraColors.Green,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (routineName.isNotBlank()) {
                            viewModel.createRoutine(routineName.trim())
                            showNewRoutineDialog = false
                        }
                    },
                ) {
                    Text("Create", color = VeyraColors.Green)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewRoutineDialog = false }) {
                    Text("Cancel", color = VeyraColors.TextTertiary)
                }
            },
        )
    }
}

private fun formatWorkoutDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
