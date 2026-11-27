package com.aura.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aura.AuraApp
import com.aura.data.local.entity.WorkoutEntity
import com.aura.data.local.entity.WorkoutSetEntity
import com.aura.data.model.ExerciseLib
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V

/** What a finished workout covered: totals, the muscles it worked and the sets logged per exercise. */
@Composable
internal fun WorkoutSummaryScreen(nav: NavController, workoutId: Long, celebrate: Boolean = false) {
    val dao = (LocalContext.current.applicationContext as AuraApp).db.workoutDao()
    var loaded by remember(workoutId) { mutableStateOf(false) }
    var workout by remember(workoutId) { mutableStateOf<WorkoutEntity?>(null) }
    var sets by remember(workoutId) { mutableStateOf<List<WorkoutSetEntity>>(emptyList()) }
    var gains by remember(workoutId) { mutableStateOf<List<SetWeightGain>>(emptyList()) }
    var showCongratulations by rememberSaveable(workoutId) { mutableStateOf(celebrate) }

    LaunchedEffect(workoutId) {
        val current = dao.getWorkoutById(workoutId)
        val completed = dao.getWorkoutSetsList(workoutId).filter { it.isCompleted && it.reps > 0 }
        if (current != null && !current.isActive) {
            gains = completed.groupBy { it.exerciseId }.flatMap { (id, exerciseSets) ->
                val prior = dao.previousExerciseSets(current, id)
                exerciseSets.mapNotNull { weightGain(it, prior[it.setNumber]) }
            }
        }
        sets = completed
        workout = current
        loaded = true
    }

    Column(Modifier.fillMaxSize().background(V.Black).statusBarsPadding().navigationBarsPadding()) {
        WorkoutHeader("Workout summary", { nav.popBackStack() })
        val current = workout
        if (current == null) {
            Text(if (loaded) "Workout unavailable" else "Loading workout…", Modifier.padding(AuraScreenInset), color = V.Gray)
            return@Column
        }
        val byExercise = remember(sets) { sets.groupBy { it.exerciseId }.values.toList() }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(AuraScreenInset),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(current.name, style = MaterialTheme.typography.headlineLarge, color = V.White)
                Text(workoutDate(current.startTime), style = MaterialTheme.typography.labelMedium, color = V.Gray)
            }
            VCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SummaryStat("Duration", workoutDuration(current.durationSeconds))
                    SummaryStat("Volume", "${workoutNumber(current.totalVolume)} kg")
                    SummaryStat("Sets", sets.size.toString())
                }
            }

            if (gains.isNotEmpty()) {
                WeightIncreaseSummary(gains)
            }

            MusclesWorkedCard(remember(byExercise) { byExercise.mapNotNull { ExerciseLib.getById(it.first().exerciseId) } })

            Text("Exercises", style = MaterialTheme.typography.titleLarge, color = V.White)
            byExercise.forEach { exerciseSets ->
                val best = exerciseSets.maxBy { it.weight }
                VCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${exerciseSets.size}×", color = V.Teal, style = MaterialTheme.typography.titleMedium)
                        Column {
                            Text(
                                ExerciseLib.getById(best.exerciseId)?.name ?: best.exerciseName,
                                color = V.White, style = MaterialTheme.typography.titleMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text("Best set · ${workoutNumber(best.weight)} kg × ${best.reps}", color = V.Gray)
                        }
                    }
                }
            }
        }
        AuraPrimaryButton("Done", { nav.popBackStack() }, Modifier.padding(horizontal = AuraScreenInset, vertical = 8.dp))
    }
    if (showCongratulations && loaded && workout?.isActive == false) {
        WorkoutCongratulations(sets.size, gains, onDismiss = { showCongratulations = false })
    }
}

@Composable
private fun WorkoutCongratulations(setCount: Int, gains: List<SetWeightGain>, onDismiss: () -> Unit) {
    val increasedSets = if (gains.size == 1) "1 set" else "${gains.size} sets"
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = V.SurfaceRaised,
        icon = {
            Box(Modifier.size(56.dp).background(V.Teal.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.EmojiEvents, null, Modifier.size(28.dp), tint = V.Teal)
            }
        },
        title = { Text("Congratulations!", color = V.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val completed = if (setCount == 1) "1 set" else "$setCount sets"
                Text("You showed up and finished $completed. Great work!", color = V.Gray)
                if (gains.isNotEmpty()) {
                    WeightGainBadge("More weight on $increasedSets")
                    Text("See which sets improved in your workout summary.", color = V.Gray)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("View summary", color = V.Teal) } },
    )
}

@Composable
private fun WeightIncreaseSummary(gains: List<SetWeightGain>) {
    VCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Weight increases", style = MaterialTheme.typography.titleLarge, color = V.White)
            Text("Compared with matching sets from your previous workout.", color = V.Gray,
                style = MaterialTheme.typography.bodySmall)
            gains.groupBy { it.current.exerciseId }.forEach { (id, exerciseGains) ->
                val name = ExerciseLib.getById(id)?.name
                    ?: exerciseGains.first().current.exerciseName.ifBlank { "Exercise" }
                HorizontalDivider(color = V.SurfaceBright)
                Text(name, style = MaterialTheme.typography.titleMedium, color = V.White)
                exerciseGains.forEach { gain ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        WeightGainBadge("Set ${gain.current.setNumber} · +${workoutNumber(gain.increaseKg)} kg")
                        Text(
                            "${workoutNumber(gain.previous.weight)} kg × ${gain.previous.reps} reps → " +
                                "${workoutNumber(gain.current.weight)} kg × ${gain.current.reps} reps",
                            style = MaterialTheme.typography.bodySmall, color = V.Gray,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = V.White)
        Text(label, style = MaterialTheme.typography.labelMedium, color = V.Gray)
    }
}

private fun workoutDuration(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
}
