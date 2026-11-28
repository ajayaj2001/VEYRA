package com.aura.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
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
import com.aura.data.local.entity.RoutineCloudDeletion
import com.aura.data.local.entity.plan
import com.aura.data.model.ExerciseLib
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

@Composable
internal fun RoutineDetailScreen(
    nav: NavController,
    routineId: Long,
) {
    val app = LocalContext.current.applicationContext as AuraApp
    val database = app.db
    val dao = database.workoutDao()
    val sessions by remember(dao, routineId) { dao.observeRoutineSessions(routineId) }.collectAsState(initial = emptyList())
    val active by remember(dao) { dao.observeActiveWorkout() }.collectAsState(initial = null)
    val routineData by remember(dao, routineId) { dao.observeRoutine(routineId) }.collectAsState(initial = null)
    val exercises = routineData?.firstOrNull()?.exercises.orEmpty().sortedBy { it.sortOrder }
    val openEditor = { nav.navigate("workout/routine/$routineId/edit") }
    val lastSets = remember(sessions) {
        buildMap {
            sessions.forEach { session ->
                session.sets.filter { it.isCompleted && it.reps > 0 }.groupBy { it.exerciseId }
                    .forEach { (id, sets) -> putIfAbsent(id, sets.sortedBy { it.setNumber }) }
            }
        }
    }

    val scope = rememberCoroutineScope()
    var confirmDelete by rememberSaveable(routineId) { mutableStateOf(false) }
    var deleting by remember(routineId) { mutableStateOf(false) }
    var deleteError by remember(routineId) { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var duplicating by remember { mutableStateOf(false) }
    var duplicateError by remember { mutableStateOf(false) }
    val current = routineData?.firstOrNull()?.routine
    val canEdit = current != null &&
        (current.cloudOwnerUid == null || current.cloudOwnerUid == app.workoutCloud.ownerUid)

    Column(Modifier.fillMaxSize().background(V.Canvas).statusBarsPadding().navigationBarsPadding()) {
        WorkoutHeader("Routine details", { nav.popBackStack() }) {
            if (current != null) Box {
                IconButton(onClick = { showMenu = true }, enabled = !deleting && !duplicating) {
                    Icon(Icons.Rounded.MoreVert, "Routine options", tint = V.Teal)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (canEdit) DropdownMenuItem(text = { Text("Edit routine") }, onClick = {
                        showMenu = false
                        openEditor()
                    })
                    if (canEdit) DropdownMenuItem(text = { Text("Duplicate routine") }, onClick = {
                        showMenu = false
                        duplicating = true
                        duplicateError = false
                        scope.launch {
                            try {
                                val copyId = database.withTransaction {
                                    val source = checkNotNull(dao.getRoutineById(routineId))
                                    check(source.cloudOwnerUid == null || source.cloudOwnerUid == app.workoutCloud.ownerUid)
                                    val uid = app.workoutCloud.ownerUid
                                    val now = System.currentTimeMillis()
                                    val id = dao.insertRoutine(source.copy(
                                        id = 0,
                                        name = "${source.name} (copy)",
                                        createdAt = now,
                                        updatedAt = now,
                                        cloudOwnerUid = uid,
                                        cloudId = uid?.let { UUID.randomUUID().toString() },
                                        cloudSyncedRevision = null,
                                    ))
                                    dao.getRoutineExercises(routineId).forEach { exercise ->
                                        dao.insertRoutineExercise(exercise.copy(id = 0, routineId = id))
                                    }
                                    id
                                }
                                app.workoutCloud.sync()
                                nav.navigate("workout/routine/$copyId")
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                duplicateError = true
                            } finally {
                                duplicating = false
                            }
                        }
                    })
                    if (canEdit) DropdownMenuItem(text = { Text("Delete routine", color = V.Red) }, onClick = {
                        showMenu = false
                        confirmDelete = true
                    })
                }
            }
        }
        if (current == null) {
            Text(if (routineData == null) "Loading routine…" else "Routine unavailable", Modifier.padding(AuraScreenInset), color = V.Gray)
            return@Column
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(AuraScreenInset),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(current.name, style = MaterialTheme.typography.headlineLarge, color = V.White)
            if (current.description.isNotBlank()) Text(current.description, color = V.Gray)
            if (duplicateError) Text("Couldn’t duplicate the routine. Try again.", color = V.Error)
            AuraPrimaryButton(
                if (active == null) "Start routine" else "Resume workout",
                onClick = {
                    nav.navigate(if (active == null) "workout/active?routineId=$routineId" else "workout/active")
                },
            )
            RoutineProgress(sessions)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Exercises", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = V.White)
                if (canEdit) TextButton(onClick = openEditor) {
                    Text("Edit routine", color = V.Teal)
                }
            }
            if (exercises.isEmpty()) Text("No exercises in this routine yet.", color = V.Gray)
            exercises.forEach { entry ->
                val exercise = ExerciseLib.getById(entry.exerciseId)
                val latest = lastSets[entry.exerciseId].orEmpty()
                val plan = entry.plan()
                val showPlan = latest.isEmpty() || entry.planJson != null
                val rows = if (showPlan) {
                    plan.sets.mapIndexed { index, set -> SetTableRow(index + 1, set.weightKg, set.reps) }
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
                                    Text(if (showPlan) "Planned sets" else "Last logged sets", color = V.Gray, style = MaterialTheme.typography.labelMedium)
                                }
                                if (exercise != null) WorkoutDetailChevron()
                            }
                        }
                        if (plan.restSeconds > 0) Text("Rest · ${plan.restSeconds} sec", color = V.Gray)
                        if (plan.note.isNotBlank()) Text(plan.note, color = V.Gray)
                        WorkoutSetTable(rows)
                    }
                }
            }
            MusclesWorkedCard(remember(exercises) { exercises.mapNotNull { ExerciseLib.getById(it.exerciseId) } })
        }
    }
    if (confirmDelete && current != null && canEdit) {
        AlertDialog(
            onDismissRequest = { if (!deleting) confirmDelete = false },
            containerColor = V.SurfaceRaised,
            title = { Text("Delete routine?", color = V.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Delete ${current.name}? Your workout history will still be available.", color = V.White)
                    if (deleteError) Text("Couldn’t delete the routine. Try again.", color = V.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }, enabled = !deleting) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(enabled = !deleting, onClick = {
                    deleting = true
                    deleteError = false
                    scope.launch {
                        try {
                            database.withTransaction {
                                val stored = dao.getRoutineById(routineId) ?: return@withTransaction
                                val ownerUid = stored.cloudOwnerUid
                                check(ownerUid == null || ownerUid == app.workoutCloud.ownerUid)
                                if (ownerUid != null && stored.cloudId != null) {
                                    dao.queueRoutineCloudDeletion(RoutineCloudDeletion(ownerUid, stored.cloudId))
                                }
                                dao.deleteRoutine(stored)
                            }
                            app.workoutCloud.sync()
                            confirmDelete = false
                            nav.popBackStack()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            deleteError = true
                        } finally {
                            deleting = false
                        }
                    }
                }) { Text("Delete", color = V.Red) }
            },
        )
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
