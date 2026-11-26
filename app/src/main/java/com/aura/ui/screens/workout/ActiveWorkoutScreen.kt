package com.aura.ui.screens.workout

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.room.withTransaction
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aura.AuraApp
import com.aura.data.local.entity.WorkoutEntity
import com.aura.data.local.entity.WorkoutSetEntity
import com.aura.data.model.Exercise
import com.aura.data.model.ExerciseLib
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ━━━ ViewModel ━━━
class ActiveWorkoutVM(app: Application) : AndroidViewModel(app) {
    private val database = (app as AuraApp).db
    private val dao = database.workoutDao()

    data class ExGroup(val exercise: Exercise, val sets: List<WorkoutSetEntity>)
    data class State(
        val workout: WorkoutEntity? = null,
        val groups: List<ExGroup> = emptyList(),
        val elapsed: Int = 0,
        val restSec: Int = 0,
        val resting: Boolean = false,
        val saving: Boolean = false,
        val closed: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state
    private val writes = Mutex()
    private var timerJob: Job? = null
    private var restJob: Job? = null

    fun initialize(routineId: Long?) = changeWorkout("Couldn’t open the workout. Go back and try again.") {
        if (_state.value.workout != null) return@changeWorkout
        val workout = database.withTransaction {
            dao.getActiveWorkout() ?: createWorkout(routineId)
        }
        refreshSets(workout)
        startTimer()
    }

    private suspend fun createWorkout(routineId: Long?): WorkoutEntity {
        val routine = routineId?.let { dao.getRoutineById(it) }
        val workout = WorkoutEntity(name = routine?.name ?: "Quick Workout", routineId = routine?.id)
        val created = workout.copy(id = dao.insertWorkout(workout))
        routine?.let {
            dao.getRoutineExercises(it.id).forEach { planned ->
                val exercise = ExerciseLib.getById(planned.exerciseId) ?: return@forEach
                insertExercise(created.id, exercise, planned.targetSets, planned.targetReps)
            }
        }
        return created
    }

    private suspend fun refreshSets(workout: WorkoutEntity) {
        val groups = dao.getWorkoutSetsList(workout.id).groupBy { it.exerciseId }.mapNotNull { (id, sets) ->
            ExerciseLib.getById(id)?.let { ExGroup(it, sets.sortedBy { set -> set.setNumber }) }
        }
        _state.value = _state.value.copy(workout = workout, groups = groups)
    }

    private suspend fun insertExercise(workoutId: Long, exercise: Exercise, setCount: Int, reps: Int = 0) {
        repeat(setCount) { index ->
            dao.insertSet(WorkoutSetEntity(
                workoutId = workoutId,
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                setNumber = index + 1,
                reps = reps,
            ))
        }
    }

    fun addExercise(exercise: Exercise) = editWorkout { workout ->
        if (_state.value.groups.none { it.exercise.id == exercise.id }) {
            database.withTransaction { insertExercise(workout.id, exercise, setCount = 3) }
        }
    }

    fun addSet(exerciseId: String) = editWorkout { workout ->
        val group = _state.value.groups.firstOrNull { it.exercise.id == exerciseId } ?: return@editWorkout
        dao.insertSet(WorkoutSetEntity(
            workoutId = workout.id,
            exerciseId = exerciseId,
            exerciseName = group.exercise.name,
            setNumber = (group.sets.maxOfOrNull { it.setNumber } ?: 0) + 1,
        ))
    }

    fun updateSet(id: Long, weight: Double, reps: Int) = editWorkout {
        val set = findSet(id) ?: return@editWorkout
        if (!set.isCompleted) dao.updateSet(set.copy(weight = weight, reps = reps))
    }

    fun toggleCompleted(id: Long) = editWorkout {
        val set = findSet(id) ?: return@editWorkout
        if (set.isCompleted) {
            dao.updateSet(set.copy(isCompleted = false))
        } else if (set.reps > 0 && set.weight.isFinite() && set.weight >= 0) {
            dao.updateSet(set.copy(isCompleted = true, timestamp = System.currentTimeMillis()))
            haptic()
        }
    }

    private fun findSet(id: Long): WorkoutSetEntity? =
        _state.value.groups.firstNotNullOfOrNull { group -> group.sets.firstOrNull { it.id == id } }

    fun removeExercise(exerciseId: String) = editWorkout { workout ->
        dao.deleteSetsForExercise(workout.id, exerciseId)
    }

    private fun editWorkout(edit: suspend (WorkoutEntity) -> Unit) =
        changeWorkout("Couldn’t save this change. Try again.") {
            val workout = _state.value.workout ?: return@changeWorkout
            if (_state.value.saving) return@changeWorkout
            edit(workout)
            refreshSets(workout)
        }

    private fun changeWorkout(error: String, action: suspend () -> Unit) = viewModelScope.launch {
        writes.withLock {
            if (_state.value.closed) return@withLock
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = _state.value.copy(saving = false, error = error)
            }
        }
    }

    fun startRest(sec: Int) {
        restJob?.cancel()
        _state.value = _state.value.copy(restSec = sec, resting = true)
        restJob = viewModelScope.launch {
            while (_state.value.restSec > 0) { delay(1000); _state.value = _state.value.copy(restSec = _state.value.restSec - 1) }
            _state.value = _state.value.copy(resting = false)
            haptic()
        }
    }

    fun skipRest() { restJob?.cancel(); _state.value = _state.value.copy(resting = false, restSec = 0) }

    fun finish() = changeWorkout("Couldn’t finish the workout. Try again.") {
        val workout = _state.value.workout ?: return@changeWorkout
        _state.value = _state.value.copy(saving = true, error = null)
        val saved = database.withTransaction {
            val completed = dao.getWorkoutSetsList(workout.id).filter { it.isCompleted && it.reps > 0 }
            if (completed.isEmpty()) return@withTransaction false
            val end = System.currentTimeMillis()
            dao.updateWorkout(workout.copy(
                isActive = false,
                endTime = end,
                durationSeconds = ((end - workout.startTime) / 1000).coerceAtLeast(0).toInt(),
                totalVolume = completed.sumOf { it.weight * it.reps },
            ))
            true
        }
        if (saved) closeWorkout()
        else _state.value = _state.value.copy(saving = false, error = "Complete at least one set before finishing.")
    }

    fun discard() = changeWorkout("Couldn’t discard the workout. Try again.") {
        val workout = _state.value.workout ?: return@changeWorkout
        _state.value = _state.value.copy(saving = true, error = null)
        dao.deleteWorkout(workout)
        closeWorkout()
    }

    private fun closeWorkout() {
        timerJob?.cancel()
        restJob?.cancel()
        _state.value = _state.value.copy(saving = false, closed = true)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val w = _state.value.workout ?: break
                _state.value = _state.value.copy(elapsed = ((System.currentTimeMillis() - w.startTime) / 1000).toInt())
                delay(1000)
            }
        }
    }

    private fun haptic() {
        try {
            val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                getApplication<Application>().getSystemService(VibratorManager::class.java)?.defaultVibrator
            else @Suppress("DEPRECATION") getApplication<Application>().getSystemService(Vibrator::class.java)
            v?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }

}

// ━━━ Screen ━━━
@Composable
fun ActiveWorkoutScreen(
    nav: NavController,
    routineId: Long?,
    vm: ActiveWorkoutVM = viewModel(),
    selectedExerciseId: String? = null,
    onExerciseHandled: () -> Unit = {},
) {
    val s by vm.state.collectAsState()
    var showFinish by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showRest by remember { mutableStateOf(false) }

    LaunchedEffect(routineId, selectedExerciseId) {
        vm.initialize(routineId).join()
        selectedExerciseId?.let { id ->
            ExerciseLib.getById(id)?.let { vm.addExercise(it).join() }
            onExerciseHandled()
        }
    }
    LaunchedEffect(s.closed) { if (s.closed) nav.popBackStack() }
    BackHandler(enabled = s.saving) {}

    Column(Modifier.fillMaxSize().background(V.Black).statusBarsPadding().navigationBarsPadding().imePadding()) {
        WorkoutHeader("Log workout", { if (!s.saving) nav.popBackStack() }) {
            IconButton(onClick = { showRest = true }) {
                Icon(Icons.Rounded.Timer, "Rest timer", tint = V.Teal)
            }
            Button(
                onClick = { showFinish = true },
                enabled = s.workout != null && !s.saving,
                colors = ButtonDefaults.buttonColors(containerColor = V.Teal, contentColor = V.Black),
            ) {
                Text("Finish")
            }
        }

        val completed = s.groups.flatMap { it.sets }.filter { it.isCompleted && it.reps > 0 }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AuraScreenInset, vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = V.SurfaceRaised,
        ) {
            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatChip("Duration", formatTime(s.elapsed), V.Teal)
                StatChip("Volume", "${workoutNumber(completed.sumOf { it.weight * it.reps })} kg", V.White)
                StatChip("Sets", completed.size.toString(), V.White)
            }
        }

        if (s.resting) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = AuraScreenInset),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Rest · ${formatTime(s.restSec)}", Modifier.weight(1f), color = V.Teal)
                TextButton(onClick = vm::skipRest) { Text("Skip rest") }
            }
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = AuraScreenInset, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (s.groups.isEmpty()) item {
                Text("Add your first exercise to start logging sets.", color = V.Gray)
            }
            items(s.groups, key = { it.exercise.id }) { group ->
                ExerciseBlock(group, s.workout, vm, onDetails = { nav.navigate("exercise/${group.exercise.id}") })
            }
            item {
                AuraPrimaryButton("Add exercise", { nav.navigate("exercise/search") }, enabled = s.workout != null)
            }
            item {
                TextButton(onClick = { showDiscard = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Discard workout", color = V.Red)
                }
            }
        }
    }

    s.error?.let { error ->
        AlertDialog(
            onDismissRequest = vm::clearError,
            title = { Text("Workout") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = vm::clearError) { Text("OK") } },
        )
    }

    if (showRest) {
        AlertDialog(
            onDismissRequest = { showRest = false },
            title = { Text("Rest timer") },
            text = {
                Column {
                    listOf(30, 60, 90, 120).forEach { seconds ->
                        TextButton(onClick = { showRest = false; vm.startRest(seconds) }) {
                            Text(if (seconds < 60) "$seconds seconds" else "${seconds / 60.0} minutes")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showRest = false }) { Text("Cancel") } },
        )
    }

    // Dialogs
    if (showFinish) {
        AlertDialog(
            onDismissRequest = { showFinish = false }, containerColor = V.SurfaceRaised,
            title = { Text("Finish Workout?", color = V.White) },
            text = { Text("Your workout will be saved.", color = V.Gray) },
            confirmButton = { TextButton(onClick = { showFinish = false; vm.finish() }) { Text("Finish", color = V.Teal) } },
            dismissButton = { TextButton(onClick = { showFinish = false }) { Text("Cancel", color = V.Gray) } },
        )
    }
    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false }, containerColor = V.SurfaceRaised,
            title = { Text("Discard Workout?", color = V.White) },
            text = { Text("All progress will be lost.", color = V.Gray) },
            confirmButton = { TextButton(onClick = { showDiscard = false; vm.discard() }) { Text("Discard", color = V.Red) } },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Cancel", color = V.Gray) } },
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = V.Gray)
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
private fun ExerciseBlock(
    group: ActiveWorkoutVM.ExGroup,
    workout: WorkoutEntity?,
    vm: ActiveWorkoutVM,
    onDetails: () -> Unit,
) {
    val dao = (LocalContext.current.applicationContext as AuraApp).db.workoutDao()
    var previous by remember(group.exercise.id, workout?.id) {
        mutableStateOf<Map<Int, WorkoutSetEntity>>(emptyMap())
    }
    LaunchedEffect(group.exercise.id, workout?.id) {
        val priorSets = dao.getRecentSetsForExercise(group.exercise.id).filter { it.workoutId != workout?.id }
        val lastWorkout = priorSets.firstOrNull()?.workoutId
        previous = priorSets.filter { it.workoutId == lastWorkout }.associateBy { it.setNumber }
    }

    VCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).clickable(onClick = onDetails).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExerciseGlyph()
                    Column {
                        Text(group.exercise.name, style = MaterialTheme.typography.titleMedium, color = V.White)
                        Text(group.exercise.equipment, style = MaterialTheme.typography.labelSmall, color = V.Gray)
                    }
                }
                var menu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Rounded.MoreVert, "Exercise options", tint = V.Gray)
                    }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Exercise details") }, onClick = { menu = false; onDetails() })
                        DropdownMenuItem(text = { Text("Remove exercise", color = V.Red) },
                            onClick = { menu = false; vm.removeExercise(group.exercise.id) })
                    }
                }
            }
            HorizontalDivider(color = V.SurfaceBright)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SetHeading("#", Modifier.width(28.dp))
                SetHeading("Previous", Modifier.weight(1.2f))
                SetHeading("kg", Modifier.weight(1f))
                SetHeading("Reps", Modifier.weight(1f))
                Spacer(Modifier.width(44.dp))
            }

            group.sets.forEach { set ->
                key(set.id) { SetRow(set, previous[set.setNumber], group.exercise.name, vm) }
            }
            WorkoutAction("Add set", Icons.Rounded.Add, { vm.addSet(group.exercise.id) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SetHeading(text: String, modifier: Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelSmall, color = V.Gray, textAlign = TextAlign.Center)
}

@Composable
private fun SetRow(set: WorkoutSetEntity, prior: WorkoutSetEntity?, exerciseName: String, vm: ActiveWorkoutVM) {
    var wt by rememberSaveable(set.id) { mutableStateOf(if (set.weight > 0) set.weight.toString() else "") }
    var rp by rememberSaveable(set.id) { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }
    val weight = parseWeight(wt)
    val reps = rp.toIntOrNull()?.takeIf { it > 0 }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (set.isCompleted) V.Teal.copy(alpha = 0.1f) else V.SurfaceRaised),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(set.setNumber.toString(), Modifier.width(28.dp), color = V.White, textAlign = TextAlign.Center)
        Text(
            prior?.let { "${workoutNumber(it.weight)} × ${it.reps}" } ?: "—",
            Modifier.weight(1.2f),
            style = MaterialTheme.typography.labelSmall,
            color = V.Gray,
            textAlign = TextAlign.Center,
        )
        MiniInput(wt, { text ->
            val parsed = parseWeight(text)
            if (parsed != null || text == "." || text == ",") {
                wt = text
                vm.updateSet(set.id, parsed ?: 0.0, rp.toIntOrNull() ?: 0)
            }
        }, Modifier.weight(1f), "Set ${set.setNumber} weight", enabled = !set.isCompleted)
        MiniInput(rp, { text ->
            val parsed = text.toIntOrNull()
            if (text.isEmpty() || (parsed != null && parsed >= 0 && text.all(Char::isDigit))) {
                rp = text
                vm.updateSet(set.id, parseWeight(wt) ?: 0.0, parsed ?: 0)
            }
        }, Modifier.weight(1f), "Set ${set.setNumber} reps", KeyboardType.Number, enabled = !set.isCompleted)
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                .background(if (set.isCompleted) V.Teal else V.SurfaceBright)
                .clickable(enabled = set.isCompleted || (weight != null && reps != null), role = Role.Button) {
                    vm.toggleCompleted(set.id)
                }.semantics {
                    contentDescription = if (set.isCompleted) "Undo completion for set ${set.setNumber}, $exerciseName"
                    else "Complete set ${set.setNumber}, $exerciseName"
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Check, null, tint = if (set.isCompleted) V.Black else V.Gray, modifier = Modifier.size(18.dp))
        }
    }
}

private fun parseWeight(value: String): Double? = if (value.isBlank()) 0.0
else value.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }

@Composable
private fun MiniInput(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier,
    label: String,
    kt: KeyboardType = KeyboardType.Decimal,
    enabled: Boolean,
) {
    val style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        modifier = modifier.height(42.dp)
            .clip(RoundedCornerShape(10.dp)).background(V.Black)
            .border(1.dp, V.SurfaceBright, RoundedCornerShape(10.dp))
            .semantics { contentDescription = label },
        textStyle = style.copy(color = V.White),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = kt),
        cursorBrush = SolidColor(V.Teal),
        decorationBox = { input ->
            Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                if (value.isEmpty()) Text("0", style = style, color = V.Gray)
                input()
            }
        },
    )
}

private fun formatTime(sec: Int): String {
    val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
