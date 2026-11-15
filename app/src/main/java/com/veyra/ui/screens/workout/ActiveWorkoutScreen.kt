package com.veyra.ui.screens.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veyra.data.local.entity.WorkoutSetEntity
import com.veyra.navigation.Screen
import com.veyra.ui.components.GlassCard
import com.veyra.ui.components.VeyraButton
import com.veyra.ui.components.VeyraOutlineButton
import com.veyra.ui.theme.VeyraColors
import com.veyra.ui.theme.VeyraDimens

@Composable
fun ActiveWorkoutScreen(
    navController: NavController,
    routineId: Long?,
    viewModel: ActiveWorkoutViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initialize(routineId)
    }

    // Navigate back when workout is finished
    LaunchedEffect(state.isFinishing) {
        if (state.isFinishing) {
            navController.popBackStack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VeyraColors.Background)
            .statusBarsPadding(),
    ) {
        // Header with timer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VeyraDimens.SpaceLg, vertical = VeyraDimens.SpaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { showDiscardDialog = true }) {
                Icon(Icons.Rounded.Close, "Close", tint = VeyraColors.TextSecondary)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.workout?.name ?: "Workout",
                    style = MaterialTheme.typography.titleLarge,
                    color = VeyraColors.TextPrimary,
                )
                Text(
                    text = formatElapsed(state.elapsedSeconds),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                    ),
                    color = VeyraColors.Green,
                )
            }
            TextButton(onClick = { showFinishDialog = true }) {
                Text("Finish", color = VeyraColors.Green, fontWeight = FontWeight.Bold)
            }
        }

        // Rest timer banner
        AnimatedVisibility(visible = state.isRestTimerRunning) {
            RestTimerBanner(
                seconds = state.restTimerSeconds,
                total = state.restTimerTotal,
                onSkip = { viewModel.stopRestTimer() },
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceLg),
        ) {
            items(state.exercises) { group ->
                ExerciseSetCard(
                    group = group,
                    onCompleteSet = { viewModel.completeSet(it) },
                    onUpdateSet = { viewModel.updateSet(it) },
                    onAddSet = { viewModel.addSet(group.exercise.id) },
                    onDeleteSet = { viewModel.deleteSet(it) },
                    onRemoveExercise = { viewModel.removeExercise(group.exercise.id) },
                    onExerciseDetail = {
                        navController.navigate(Screen.ExerciseDetail.createRoute(group.exercise.id))
                    },
                    estimateOneRepMax = { w, r -> viewModel.estimateOneRepMax(w, r) },
                )
            }

            // Add exercise button
            item {
                Column(Modifier.padding(horizontal = VeyraDimens.SpaceLg)) {
                    VeyraOutlineButton(
                        text = "Add Exercise",
                        onClick = { navController.navigate(Screen.ExerciseSearch.route) },
                        color = VeyraColors.Green,
                    )
                }
            }

            item { Spacer(Modifier.height(VeyraDimens.SpaceHuge)) }
        }
    }

    // Finish dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            containerColor = VeyraColors.SurfaceElevated,
            title = { Text("Finish Workout?", color = VeyraColors.TextPrimary) },
            text = {
                Text(
                    "Your workout will be saved and exported to Health Connect.",
                    color = VeyraColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    viewModel.finishWorkout()
                }) {
                    Text("Finish", color = VeyraColors.Green)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Cancel", color = VeyraColors.TextTertiary)
                }
            },
        )
    }

    // Discard dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = VeyraColors.SurfaceElevated,
            title = { Text("Discard Workout?", color = VeyraColors.TextPrimary) },
            text = {
                Text(
                    "This will delete all progress for this workout.",
                    color = VeyraColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    viewModel.discardWorkout()
                    navController.popBackStack()
                }) {
                    Text("Discard", color = VeyraColors.Coral)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel", color = VeyraColors.TextTertiary)
                }
            },
        )
    }
}

@Composable
private fun RestTimerBanner(
    seconds: Int,
    total: Int,
    onSkip: () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = seconds.toFloat() / total.toFloat(),
        animationSpec = tween(300),
        label = "restProgress",
    )

    GlassCard(
        modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg),
        gradientColors = listOf(
            VeyraColors.Green.copy(alpha = 0.15f),
            VeyraColors.Teal.copy(alpha = 0.1f),
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Timer,
                contentDescription = null,
                tint = VeyraColors.Green,
                modifier = Modifier.size(VeyraDimens.IconXl),
            )
            Spacer(Modifier.width(VeyraDimens.SpaceMd))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Rest Timer",
                    style = MaterialTheme.typography.labelMedium,
                    color = VeyraColors.TextTertiary,
                )
                Text(
                    text = "${seconds / 60}:%02d".format(seconds % 60),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                    ),
                    color = VeyraColors.Green,
                )
            }
            TextButton(onClick = onSkip) {
                Text("Skip", color = VeyraColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun ExerciseSetCard(
    group: WorkoutExerciseGroup,
    onCompleteSet: (WorkoutSetEntity) -> Unit,
    onUpdateSet: (WorkoutSetEntity) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (WorkoutSetEntity) -> Unit,
    onRemoveExercise: () -> Unit,
    onExerciseDetail: () -> Unit,
    estimateOneRepMax: (Double, Int) -> Double,
) {
    GlassCard(
        modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg),
    ) {
        Column {
            // Exercise header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = group.exercise.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = VeyraColors.Green,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onExerciseDetail),
                )
                IconButton(
                    onClick = onRemoveExercise,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Remove",
                        tint = VeyraColors.TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Previous best
            if (group.previousBest != null) {
                Text(
                    text = "PR: ${group.previousBest.weight}kg x ${group.previousBest.reps} " +
                        "(est. 1RM: ${"%.1f".format(estimateOneRepMax(group.previousBest.weight, group.previousBest.reps))}kg)",
                    style = MaterialTheme.typography.bodySmall,
                    color = VeyraColors.TextTertiary,
                )
            }

            Spacer(Modifier.height(VeyraDimens.SpaceMd))

            // Column headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Set", style = MaterialTheme.typography.labelSmall, color = VeyraColors.TextTertiary, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                Text("Previous", style = MaterialTheme.typography.labelSmall, color = VeyraColors.TextTertiary, modifier = Modifier.width(64.dp), textAlign = TextAlign.Center)
                Text("kg", style = MaterialTheme.typography.labelSmall, color = VeyraColors.TextTertiary, modifier = Modifier.width(64.dp), textAlign = TextAlign.Center)
                Text("Reps", style = MaterialTheme.typography.labelSmall, color = VeyraColors.TextTertiary, modifier = Modifier.width(64.dp), textAlign = TextAlign.Center)
                Box(Modifier.width(36.dp))
            }

            Spacer(Modifier.height(VeyraDimens.SpaceXs))

            // Sets
            group.sets.forEach { set ->
                SetRow(
                    set = set,
                    onUpdate = onUpdateSet,
                    onComplete = onCompleteSet,
                )
            }

            Spacer(Modifier.height(VeyraDimens.SpaceSm))

            // Add set button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VeyraDimens.CardRadiusSm))
                    .clickable(onClick = onAddSet)
                    .padding(vertical = VeyraDimens.SpaceSm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Add set",
                    tint = VeyraColors.TextTertiary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(VeyraDimens.SpaceXs))
                Text(
                    text = "Add Set",
                    style = MaterialTheme.typography.labelMedium,
                    color = VeyraColors.TextTertiary,
                )
            }
        }
    }
}

@Composable
private fun SetRow(
    set: WorkoutSetEntity,
    onUpdate: (WorkoutSetEntity) -> Unit,
    onComplete: (WorkoutSetEntity) -> Unit,
) {
    var weightText by remember(set.id) { mutableStateOf(if (set.weight > 0) set.weight.toString() else "") }
    var repsText by remember(set.id) { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }

    val bgColor = when {
        set.isCompleted -> VeyraColors.Green.copy(alpha = 0.08f)
        set.isWarmup -> VeyraColors.Amber.copy(alpha = 0.08f)
        else -> VeyraColors.Surface.copy(alpha = 0.3f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Set number
        Text(
            text = if (set.isWarmup) "W" else set.setNumber.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = if (set.isWarmup) VeyraColors.Amber else VeyraColors.TextSecondary,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center,
        )

        // Previous
        Text(
            text = "-",
            style = MaterialTheme.typography.bodySmall,
            color = VeyraColors.TextTertiary,
            modifier = Modifier.width(64.dp),
            textAlign = TextAlign.Center,
        )

        // Weight
        TextField(
            value = weightText,
            onValueChange = { v ->
                weightText = v
                val weight = v.toDoubleOrNull() ?: 0.0
                onUpdate(set.copy(weight = weight))
            },
            modifier = Modifier
                .width(64.dp)
                .height(40.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                color = VeyraColors.TextPrimary,
            ),
            placeholder = { Text("0", style = MaterialTheme.typography.bodySmall, color = VeyraColors.TextTertiary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = VeyraColors.Surface.copy(alpha = 0.5f),
                unfocusedContainerColor = VeyraColors.Surface.copy(alpha = 0.3f),
                focusedIndicatorColor = VeyraColors.Green,
                unfocusedIndicatorColor = VeyraColors.Divider,
                cursorColor = VeyraColors.Green,
            ),
        )

        // Reps
        TextField(
            value = repsText,
            onValueChange = { v ->
                repsText = v
                val reps = v.toIntOrNull() ?: 0
                onUpdate(set.copy(reps = reps))
            },
            modifier = Modifier
                .width(64.dp)
                .height(40.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                color = VeyraColors.TextPrimary,
            ),
            placeholder = { Text("0", style = MaterialTheme.typography.bodySmall, color = VeyraColors.TextTertiary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = VeyraColors.Surface.copy(alpha = 0.5f),
                unfocusedContainerColor = VeyraColors.Surface.copy(alpha = 0.3f),
                focusedIndicatorColor = VeyraColors.Green,
                unfocusedIndicatorColor = VeyraColors.Divider,
                cursorColor = VeyraColors.Green,
            ),
        )

        // Complete button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (set.isCompleted) VeyraColors.Green else VeyraColors.SurfaceElevated
                )
                .border(1.dp, if (set.isCompleted) VeyraColors.Green else VeyraColors.Divider, CircleShape)
                .clickable {
                    if (!set.isCompleted) {
                        onComplete(set.copy(
                            weight = weightText.toDoubleOrNull() ?: 0.0,
                            reps = repsText.toIntOrNull() ?: 0,
                        ))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Complete",
                tint = if (set.isCompleted) VeyraColors.Background else VeyraColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun formatElapsed(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
