package com.aura.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aura.AuraApp
import androidx.room.withTransaction
import com.aura.data.local.dao.WorkoutWithSets
import com.aura.data.local.entity.RoutineEntity
import com.aura.data.local.entity.RoutineExerciseEntity
import com.aura.data.model.ExerciseLib
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
internal fun RoutineDetailScreen(
    nav: NavController,
    routineId: Long,
    selectedExerciseIds: List<String> = emptyList(),
    onExerciseHandled: () -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as AuraApp
    val database = app.db
    val dao = database.workoutDao()
    val sessions by remember(dao, routineId) { dao.observeRoutineSessions(routineId) }.collectAsState(initial = emptyList())
    val active by remember(dao) { dao.observeActiveWorkout() }.collectAsState(initial = null)
    var loaded by remember(routineId) { mutableStateOf(false) }
    var routine by remember(routineId) { mutableStateOf<RoutineEntity?>(null) }
    var exercises by remember(routineId) { mutableStateOf<List<RoutineExerciseEntity>>(emptyList()) }
    var editing by rememberSaveable(routineId) { mutableStateOf(false) }
    val lastSets = remember(sessions) {
        buildMap {
            sessions.forEach { session ->
                session.sets.filter { it.isCompleted && it.reps > 0 }.groupBy { it.exerciseId }
                    .forEach { (id, sets) -> putIfAbsent(id, sets.sortedBy { it.setNumber }) }
            }
        }
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(routineId, selectedExerciseIds) {
        routine = dao.getRoutineById(routineId)
        if (selectedExerciseIds.isNotEmpty()) {
            if (routine?.let { it.cloudOwnerUid == null || it.cloudOwnerUid == app.workoutCloud.ownerUid } == true) {
                database.withTransaction {
                    val planned = dao.getRoutineExercises(routineId)
                    val existing = planned.map { it.exerciseId }.toSet()
                    val nextOrder = (planned.maxOfOrNull { it.sortOrder } ?: -1) + 1
                    val additions = selectedExerciseIds.distinct().filterNot { it in existing }
                    additions.forEachIndexed { index, id ->
                        dao.insertRoutineExercise(RoutineExerciseEntity(
                            routineId = routineId, exerciseId = id, sortOrder = nextOrder + index,
                        ))
                    }
                    if (additions.isNotEmpty()) dao.touchRoutine(routineId, System.currentTimeMillis())
                }
                app.workoutCloud.sync()
            }
            onExerciseHandled()
        }
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
        val canEdit = current.cloudOwnerUid == null || current.cloudOwnerUid == app.workoutCloud.ownerUid
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(AuraScreenInset),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(current.name, style = MaterialTheme.typography.headlineLarge, color = V.White)
            if (current.description.isNotBlank()) Text(current.description, color = V.Gray)
            AuraPrimaryButton(
                if (active == null) "Start routine" else "Resume workout",
                onClick = {
                    nav.navigate(if (active == null) "workout/active?routineId=$routineId" else "workout/active")
                },
            )
            RoutineProgress(sessions)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Exercises", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = V.White)
                if (canEdit && exercises.isNotEmpty()) {
                    TextButton(onClick = { editing = !editing }) {
                        Text(if (editing) "Done" else "Edit routine", color = V.Teal)
                    }
                }
            }
            if (exercises.isEmpty()) Text("No exercises in this routine yet.", color = V.Gray)
            exercises.forEach { entry ->
                val exercise = ExerciseLib.getById(entry.exerciseId)
                val latest = lastSets[entry.exerciseId].orEmpty()
                val rows = if (latest.isEmpty()) {
                    List(entry.targetSets.coerceAtLeast(0)) { SetTableRow(it + 1, null, entry.targetReps) }
                } else {
                    latest.map { SetTableRow(it.setNumber, it.weight, it.reps) }
                }
                VCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                Modifier.weight(1f).heightIn(min = 48.dp).then(
                                    if (exercise == null) Modifier else Modifier.clickable(
                                        role = Role.Button, onClickLabel = "View exercise details",
                                    ) { nav.navigate("exercise/${exercise.id}") },
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ExerciseThumbnail(exercise?.imageUrl)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(exercise?.name ?: "Exercise unavailable", color = V.White, style = MaterialTheme.typography.titleMedium)
                                    Text(if (latest.isEmpty()) "Planned sets" else "Last logged sets", color = V.Gray, style = MaterialTheme.typography.labelMedium)
                                }
                                if (exercise != null && !editing) WorkoutDetailChevron()
                            }
                            if (canEdit && editing) IconButton(onClick = {
                                scope.launch {
                                    database.withTransaction {
                                        dao.deleteRoutineExercise(entry.id)
                                        dao.touchRoutine(routineId, System.currentTimeMillis())
                                    }
                                    app.workoutCloud.sync()
                                    exercises = dao.getRoutineExercises(routineId)
                                }
                            }) {
                                Icon(Icons.Rounded.Close, "Remove ${exercise?.name ?: "exercise"} from routine", tint = V.Gray)
                            }
                        }
                        WorkoutSetTable(rows)
                    }
                }
            }
            if (canEdit && (editing || exercises.isEmpty())) {
                WorkoutAction("Add exercise", Icons.Rounded.Add, { openExercisePicker(nav, exercises.map { it.exerciseId }) }, Modifier.fillMaxWidth())
            }
            MusclesWorkedCard(remember(exercises) { exercises.mapNotNull { ExerciseLib.getById(it.exerciseId) } })
        }
    }
}

private enum class RoutineMetric(val label: String, val unit: String) {
    Volume("Volume", "kg"),
    Reps("Reps", "reps"),
    Duration("Duration", "min");

    fun value(session: WorkoutWithSets): Double = when (this) {
        Volume -> session.workout.totalVolume
        Reps -> session.sets.filter { it.isCompleted && it.reps > 0 }.sumOf { it.reps.toDouble() }
        Duration -> session.workout.durationSeconds / 60.0
    }
}

private enum class RoutineRange(val label: String, val months: Int?) {
    Quarter("Last 3 months", 3),
    Year("Last year", 12),
    All("All time", null),
}

@Composable
private fun RoutineProgress(sessions: List<WorkoutWithSets>) {
    var metric by rememberSaveable { mutableStateOf(RoutineMetric.Volume) }
    var range by rememberSaveable { mutableStateOf(RoutineRange.Quarter) }
    var showRanges by remember { mutableStateOf(false) }
    val points = remember(sessions, range, metric) {
        val cutoff = range.months?.let { months ->
            Calendar.getInstance().apply { add(Calendar.MONTH, -months) }.timeInMillis
        } ?: Long.MIN_VALUE
        sessions.asReversed().filter { it.workout.startTime >= cutoff }
            .map { it.workout.startTime to metric.value(it) }
    }
    VCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Routine progress", style = MaterialTheme.typography.titleLarge, color = V.White)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    points.lastOrNull()?.let { (date, value) ->
                        Text("${workoutNumber(value)} ${metric.unit}", style = MaterialTheme.typography.titleMedium, color = V.White)
                        Text(workoutDate(date), style = MaterialTheme.typography.labelMedium, color = V.Gray)
                    }
                }
                Box {
                    TextButton(onClick = { showRanges = true }) {
                        Text(range.label, color = V.Teal)
                        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = V.Teal)
                    }
                    DropdownMenu(expanded = showRanges, onDismissRequest = { showRanges = false }) {
                        RoutineRange.entries.forEach { option ->
                            DropdownMenuItem(text = { Text(option.label) }, onClick = {
                                range = option
                                showRanges = false
                            })
                        }
                    }
                }
            }
            ProgressChart(
                points,
                metric.unit,
                if (sessions.isEmpty()) "Complete this routine to see progress." else "No workouts in this date range.",
                showLatestValue = false,
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoutineMetric.entries.forEach { option ->
                    WorkoutMetricFilter(option.label, metric == option) { metric = option }
                }
            }
        }
    }
}
