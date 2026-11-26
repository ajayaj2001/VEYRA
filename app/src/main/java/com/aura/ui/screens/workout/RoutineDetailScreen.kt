package com.aura.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aura.AuraApp
import com.aura.data.local.entity.RoutineEntity
import com.aura.data.local.entity.RoutineExerciseEntity
import com.aura.data.model.ExerciseLib
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V

@Composable
internal fun RoutineDetailScreen(nav: NavController, routineId: Long) {
    val dao = (LocalContext.current.applicationContext as AuraApp).db.workoutDao()
    val workoutHistory by remember(dao) { dao.getWorkoutHistory() }.collectAsState(initial = emptyList())
    val active by remember(dao) { dao.observeActiveWorkout() }.collectAsState(initial = null)
    var loaded by remember(routineId) { mutableStateOf(false) }
    var routine by remember(routineId) { mutableStateOf<RoutineEntity?>(null) }
    var exercises by remember(routineId) { mutableStateOf<List<RoutineExerciseEntity>>(emptyList()) }
    var metric by rememberSaveable(routineId) { mutableIntStateOf(0) }
    val sessions = workoutHistory.filter { it.routineId == routineId }.take(30).asReversed()

    LaunchedEffect(routineId) {
        routine = dao.getRoutineById(routineId)
        exercises = dao.getRoutineExercises(routineId)
        loaded = true
    }

    Column(Modifier.fillMaxSize().background(V.Black).statusBarsPadding().navigationBarsPadding()) {
        WorkoutHeader("Routine details", { nav.popBackStack() })
        val current = routine
        if (current == null) {
            Text(if (loaded) "Routine unavailable" else "Loading routine…", Modifier.padding(AuraScreenInset), color = V.Gray)
            return@Column
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(AuraScreenInset),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(current.name, style = MaterialTheme.typography.headlineLarge, color = V.White)
            if (current.description.isNotBlank()) Text(current.description, color = V.Gray)
            VCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Routine progress", style = MaterialTheme.typography.titleLarge, color = V.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WorkoutMetricFilter("Volume", metric == 0) { metric = 0 }
                        WorkoutMetricFilter("Duration", metric == 1) { metric = 1 }
                    }
                    val points = sessions.map { workout ->
                        workout.startTime to if (metric == 0) workout.totalVolume else workout.durationSeconds / 60.0
                    }
                    ProgressChart(points, if (metric == 0) "kg" else "min", "Complete this routine to see progress.")
                }
            }

            Text("Planned exercises", style = MaterialTheme.typography.titleLarge, color = V.White)
            if (exercises.isEmpty()) Text("No exercises in this routine yet.", color = V.Gray)
            exercises.forEach { entry ->
                val exercise = ExerciseLib.getById(entry.exerciseId)
                VCard(onClick = exercise?.let { { nav.navigate("exercise/${it.id}") } }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ExerciseGlyph()
                        Column {
                            Text(exercise?.name ?: "Exercise unavailable", color = V.White, style = MaterialTheme.typography.titleMedium)
                            Text("${entry.targetSets} sets · ${entry.targetReps} reps", color = V.Gray)
                        }
                    }
                }
            }
        }
        AuraPrimaryButton(
            if (active == null) "Start routine" else "Resume workout",
            onClick = {
                nav.navigate(if (active == null) "workout/active?routineId=$routineId" else "workout/active")
            },
            modifier = Modifier.padding(horizontal = AuraScreenInset, vertical = 8.dp),
        )
    }
}
