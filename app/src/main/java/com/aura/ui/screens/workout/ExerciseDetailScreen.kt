package com.aura.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.aura.data.local.entity.WorkoutEntity
import java.text.DateFormat
import java.util.Date
import android.net.Uri
import android.media.MediaPlayer
import android.widget.VideoView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aura.AuraApp
import com.aura.data.local.entity.WorkoutSetEntity
import com.aura.data.model.Exercise
import com.aura.data.model.ExerciseLib
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V

private data class ExerciseSession(val workout: WorkoutEntity, val sets: List<WorkoutSetEntity>)

private fun exerciseSetGains(history: List<ExerciseSession>): Map<Long, Double> {
    val previousByRoutine = mutableMapOf<Long, Map<Int, WorkoutSetEntity>>()
    var previous = emptyMap<Int, WorkoutSetEntity>()
    return buildMap {
        history.asReversed().forEach { session ->
            val routineId = session.workout.routineId
            val baseline = if (routineId == null) previous else previousByRoutine[routineId].orEmpty()
            session.sets.forEach { set ->
                weightGain(set, baseline[set.setNumber])?.let { put(set.id, it.increaseKg) }
            }
            previous = session.sets.associateBy { it.setNumber }
            if (routineId != null) previousByRoutine[routineId] = previous
        }
    }
}

@Composable
fun ExerciseDetailScreen(
    nav: NavController,
    exerciseId: String,
    onAdd: (() -> Unit)? = null,
    isSelected: Boolean = false,
) {
    val exercise = remember(exerciseId) { ExerciseLib.getById(exerciseId) }
    val dao = (LocalContext.current.applicationContext as AuraApp).db.workoutDao()
    var tab by rememberSaveable(exerciseId) { mutableIntStateOf(0) }
    val sessions by remember(exerciseId, dao) { dao.observeExerciseSessions(exerciseId) }.collectAsState(initial = null)
    val history = remember(sessions, exerciseId) {
        sessions?.map { session ->
            ExerciseSession(session.workout, session.sets.filter {
                it.exerciseId == exerciseId && it.isCompleted && it.reps > 0
            }.sortedBy { it.setNumber })
        }
    }

    Column(Modifier.fillMaxSize().background(V.Canvas).statusBarsPadding().navigationBarsPadding()) {
        WorkoutHeader(exercise?.name ?: "Exercise", { nav.popBackStack() })
        if (exercise == null) {
            Text("Exercise unavailable", Modifier.padding(AuraScreenInset), color = V.Gray)
            return@Column
        }
        TabRow(selectedTabIndex = tab, containerColor = V.Canvas, contentColor = V.Teal) {
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
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                if (tab != 1) ExerciseMedia(exercise)
                Column(Modifier.padding(AuraScreenInset), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (tab) {
                        0 -> ExerciseOverview(exercise, history)
                        1 -> ExerciseHistory(exercise, history) { nav.navigate("workout/summary/$it") }
                        2 -> Technique(exercise)
                    }
                }
            }
        }
        if (onAdd != null) {
            AuraPrimaryButton(if (isSelected) "Deselect exercise" else "Select exercise", onAdd, Modifier.padding(horizontal = AuraScreenInset, vertical = 8.dp))
        }
    }
}

@Composable
private fun ExerciseOverview(exercise: Exercise, history: List<ExerciseSession>?) {
    VCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.headlineMedium, color = V.White)
            Text("${exercise.category} · ${exercise.equipment}", style = MaterialTheme.typography.bodyMedium, color = V.Gray)
            HorizontalDivider(color = V.SurfaceBright)
            Text("Primary  ·  ${exercise.primaryMuscles.joinToString()}", color = V.White)
            if (exercise.secondaryMuscles.isNotEmpty()) {
                Text("Secondary  ·  ${exercise.secondaryMuscles.joinToString()}", color = V.Gray)
            }
        }
    }

    MusclesWorkedCard(remember(exercise) { listOf(exercise) })

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
                session.workout.startTime to when (metric) {
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
private fun ExerciseMedia(exercise: Exercise) {
    var videoFailed by remember(exercise.id) { mutableStateOf(false) }
    if (exercise.videoUrl == null && exercise.imageUrl == null) return
    Column(Modifier.fillMaxWidth().background(V.SurfaceRaised)) {
        when {
            exercise.videoUrl != null && !videoFailed ->
                ExerciseVideo(exercise.videoUrl, exercise.imageUrl) { videoFailed = true }
            exercise.imageUrl != null -> BoxWithConstraints(Modifier.fillMaxWidth().background(V.White)) {
                AsyncImage(
                    exercise.imageUrl, "${exercise.name} demonstration",
                    Modifier.fillMaxWidth().height(maxWidth.coerceAtMost(420.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                )
            }
            else -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Demonstration unavailable", color = V.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
        HorizontalDivider(color = V.SurfaceBright)
    }
}

/** Silent, looping demonstration clip; [onError] lets the caller fall back to the still image. */
@Composable
private fun ExerciseVideo(url: String, poster: String?, onError: () -> Unit) {
    var ratio by remember(url) { mutableFloatStateOf(1f) }
    var rendering by remember(url) { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxWidth().background(V.White), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    setOnPreparedListener { player ->
                        if (player.videoWidth > 0 && player.videoHeight > 0) {
                            ratio = player.videoWidth.toFloat() / player.videoHeight
                        }
                        player.isLooping = true
                        player.setVolume(0f, 0f)
                        start()
                    }
                    setOnInfoListener { _, what, _ ->
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) rendering = true
                        false
                    }
                    setOnErrorListener { _, _, _ -> onError(); true }
                    setVideoURI(Uri.parse(url))
                }
            },
            modifier = Modifier.fillMaxWidth().height((maxWidth / ratio).coerceAtMost(420.dp)),
            onRelease = { it.stopPlayback() },
        )
        if (!rendering) {
            Box(Modifier.matchParentSize().background(V.White), contentAlignment = Alignment.Center) {
                if (poster != null) {
                    AsyncImage(poster, null, Modifier.matchParentSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                }
                CircularProgressIndicator(Modifier.align(Alignment.BottomEnd).padding(12.dp).size(20.dp),
                    color = V.Teal, strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun ExerciseHistory(exercise: Exercise, history: List<ExerciseSession>?, onWorkout: (Long) -> Unit) {
    if (history.isNullOrEmpty()) {
        Text(if (history == null) "Loading workout history…" else "No completed workouts with this exercise yet.", color = V.Gray)
        return
    }
    val gains = remember(history) { exerciseSetGains(history) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    history.forEach { session ->
        VCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClickLabel = "View workout summary") { onWorkout(session.workout.id) },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(session.workout.name, style = MaterialTheme.typography.titleLarge, color = V.White)
                        Text(dateFormat.format(Date(session.workout.startTime)), style = MaterialTheme.typography.labelMedium, color = V.Gray)
                    }
                    WorkoutDetailChevron()
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExerciseThumbnail(exercise.imageUrl)
                    Text(exercise.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = V.White)
                }
                WorkoutSetTable(session.sets.map { SetTableRow(it.setNumber, it.weight, it.reps, gains[it.id]) })
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
}
