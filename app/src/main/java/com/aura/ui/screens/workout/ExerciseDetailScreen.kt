package com.aura.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aura.AuraApp
import com.aura.data.exercise.ExerciseStore
import java.io.File
import com.aura.data.local.entity.WorkoutSetEntity
import com.aura.data.model.Exercise
import com.aura.data.model.ExerciseLib
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V

private data class ExerciseSession(val name: String, val date: Long, val sets: List<WorkoutSetEntity>)

@Composable
fun ExerciseDetailScreen(nav: NavController, exerciseId: String, onAdd: (() -> Unit)? = null) {
    val exercise = remember(exerciseId) { ExerciseLib.getById(exerciseId) }
    val dao = (LocalContext.current.applicationContext as AuraApp).db.workoutDao()
    var tab by rememberSaveable(exerciseId) { mutableIntStateOf(0) }
    var history by remember(exerciseId) { mutableStateOf<List<ExerciseSession>?>(null) }

    LaunchedEffect(exerciseId) {
        history = dao.getRecentSetsForExercise(exerciseId).groupBy { it.workoutId }.mapNotNull { (id, sets) ->
            val workout = dao.getWorkoutById(id) ?: return@mapNotNull null
            if (workout.isActive) return@mapNotNull null
            ExerciseSession(workout.name, workout.startTime, sets.sortedBy { it.setNumber })
        }.sortedByDescending { it.date }
    }

    Column(Modifier.fillMaxSize().background(V.Black).statusBarsPadding().navigationBarsPadding()) {
        WorkoutHeader(exercise?.name ?: "Exercise", { nav.popBackStack() })
        if (exercise == null) {
            Text("Exercise unavailable", Modifier.padding(AuraScreenInset), color = V.Gray)
            return@Column
        }
        TabRow(selectedTabIndex = tab, containerColor = V.Black, contentColor = V.Teal) {
            listOf("Overview", "History", "Technique").forEachIndexed { index, title ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    selectedContentColor = V.Teal,
                    unselectedContentColor = V.Gray,
                    text = { Text(title, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
        key(tab) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(AuraScreenInset),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (tab) {
                    0 -> ExerciseOverview(exercise, history)
                    1 -> ExerciseHistory(history)
                    2 -> Technique(exercise)
                }
            }
        }
        if (onAdd != null) {
            AuraPrimaryButton("Add to workout", onAdd, Modifier.padding(horizontal = AuraScreenInset, vertical = 8.dp))
        }
    }
}

@Composable
private fun ExerciseOverview(exercise: Exercise, history: List<ExerciseSession>?) {
    VCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Demonstrations are GIFs, which the app's image loader animates.
            if (exercise.imageUrl == null) ExerciseGlyph(52.dp) else AsyncImage(
                exercise.imageUrl, "${exercise.name} demonstration",
                Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(V.White),
            )
            Text(exercise.name, style = MaterialTheme.typography.headlineMedium, color = V.White)
            Text("${exercise.category} · ${exercise.equipment}", style = MaterialTheme.typography.bodyMedium, color = V.Gray)
            exercise.overview?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = V.Gray) }
            HorizontalDivider(color = V.SurfaceBright)
            Text("Primary  ·  ${exercise.primaryMuscles.joinToString()}", color = V.White)
            if (exercise.secondaryMuscles.isNotEmpty()) {
                Text("Secondary  ·  ${exercise.secondaryMuscles.joinToString()}", color = V.Gray)
            }
        }
    }

    val muscles by produceState<File?>(null, exercise.id) { value = ExerciseStore.muscleImage(exercise) }
    muscles?.let { image ->
        VCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Muscles worked", style = MaterialTheme.typography.titleLarge, color = V.White)
                // The diagram comes on a white background, so it sits on a white panel.
                AsyncImage(
                    image, "Body diagram highlighting ${exercise.primaryMuscles.joinToString()}",
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(V.White),
                )
            }
        }
    }

    var metric by rememberSaveable { mutableIntStateOf(0) }
    VCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Progress", style = MaterialTheme.typography.titleLarge, color = V.White)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Top weight", "Est. 1RM", "Volume").forEachIndexed { index, label ->
                    WorkoutMetricFilter(label, metric == index) { metric = index }
                }
            }
            val points = history.orEmpty().asReversed().map { session ->
                session.date to when (metric) {
                    0 -> session.sets.maxOf { it.weight }
                    1 -> session.sets.maxOf { if (it.reps <= 1) it.weight else it.weight * (1 + it.reps / 30.0) }
                    else -> session.sets.sumOf { it.weight * it.reps }
                }
            }
            ProgressChart(points, "kg", if (history == null) "Loading workout history…" else "Complete a workout with this exercise to see progress.")
        }
    }
}

@Composable
private fun ExerciseHistory(history: List<ExerciseSession>?) {
    if (history.isNullOrEmpty()) {
        Text(if (history == null) "Loading workout history…" else "No completed workouts with this exercise yet.", color = V.Gray)
        return
    }
    history.forEach { session ->
        VCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(session.name, style = MaterialTheme.typography.titleLarge, color = V.White)
                Text(workoutDate(session.date), style = MaterialTheme.typography.labelMedium, color = V.Gray)
                HorizontalDivider(color = V.SurfaceBright)
                session.sets.forEach { set ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Set ${set.setNumber}", color = V.Gray)
                        Text("${workoutNumber(set.weight)} kg × ${set.reps}", color = V.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun Technique(exercise: Exercise) {
    Text("How to perform", style = MaterialTheme.typography.headlineMedium, color = V.White)
    if (exercise.instructions.isEmpty()) Text("Technique instructions are unavailable.", color = V.Gray)
    exercise.instructions.forEachIndexed { index, step ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Text("${index + 1}", Modifier.width(28.dp), color = V.Teal, style = MaterialTheme.typography.titleMedium)
            Text(step, Modifier.weight(1f), color = V.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (exercise.tips.isNotEmpty()) {
        VCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Coaching cues", style = MaterialTheme.typography.titleLarge, color = V.White)
                exercise.tips.forEach { Text(it, color = V.Gray) }
            }
        }
    }
}
