package com.aura.ui.screens.workout

import android.Manifest
import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aura.AuraApp
import com.aura.data.local.entity.plan
import com.aura.data.local.entity.WorkoutEntity
import com.aura.data.local.entity.WorkoutSetEntity
import com.aura.data.model.Exercise
import com.aura.data.model.ExerciseLib
import com.aura.navigation.Routes
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V
import com.aura.workout.WorkoutWeightPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal enum class WorkoutWeightUnit(val symbol: String) {
    KG("kg"), LB("lbs");

    fun fromKilograms(kg: Double) = if (this == KG) kg else kg / KILOGRAMS_PER_POUND
    fun toKilograms(value: Double) = if (this == KG) value else value * KILOGRAMS_PER_POUND

    companion object {
        private const val KILOGRAMS_PER_POUND = 0.45359237
    }
}

private enum class SetColumn(val title: String, val description: String) {
    Previous("Previous", "Weight and reps for this set in your last comparable workout with this exercise. For routines, this uses the same routine."),
    Weight("Weight", "Enter the weight you lifted. Leave it empty for a bodyweight set. The unit applies to this exercise in the current workout."),
    Reps("Reps", "Enter how many repetitions you completed. Enter a number greater than zero to mark the set complete."),
}

// ━━━ ViewModel ━━━
class ActiveWorkoutVM(app: Application) : AndroidViewModel(app) {
    private val database = (app as AuraApp).db
    private val dao = database.workoutDao()
    private val units = WorkoutWeightPreferences(app)

    data class ExGroup(val exercise: Exercise, val sets: List<WorkoutSetEntity>)
    data class State(
        val workout: WorkoutEntity? = null,
        val groups: List<ExGroup> = emptyList(),
        val elapsed: Int = 0,
        val restSec: Int = 0,
        val resting: Boolean = false,
        val saving: Boolean = false,
        val closed: Boolean = false,
        /** Set when the workout was finished rather than discarded, so its summary can be shown. */
        val finishedId: Long? = null,
        val error: String? = null,
        val poundExercises: Set<String> = emptySet(),
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state
    private val writes = Mutex()
    private var timerJob: Job? = null
    private var restJob: Job? = null
    private var setObserver: Job? = null

    fun initialize(routineId: Long?) = changeWorkout("Couldn’t open the workout. Go back and try again.") {
        if (_state.value.workout != null) return@changeWorkout
        val workout = database.withTransaction {
            dao.getActiveWorkout() ?: createWorkout(routineId)
        }
        _state.value = _state.value.copy(poundExercises = units.poundExercises(workout.id))
        refreshSets(workout)
        setObserver = viewModelScope.launch {
            dao.getWorkoutSets(workout.id).collect {
                writes.withLock {
                    if (!_state.value.closed) refreshSets(workout)
                }
            }
        }
        startTimer()
    }

    private suspend fun createWorkout(routineId: Long?): WorkoutEntity {
        val routine = routineId?.let { dao.getRoutineById(it) }
        val uid = getApplication<AuraApp>().workoutCloud.ownerUid
        val workout = WorkoutEntity(
            name = routine?.name ?: "Quick Workout",
            routineId = routine?.id,
            cloudOwnerUid = uid,
            cloudId = uid?.let { java.util.UUID.randomUUID().toString() },
        )
        val created = workout.copy(id = dao.insertWorkout(workout))
        routine?.let {
            dao.getRoutineExercises(it.id).forEach { planned ->
                val exercise = ExerciseLib.getById(planned.exerciseId) ?: return@forEach
                val plan = planned.plan()
                plan.sets.forEachIndexed { index, set ->
                    dao.insertSet(WorkoutSetEntity(
                        workoutId = created.id,
                        exerciseId = exercise.id,
                        exerciseName = exercise.name,
                        setNumber = index + 1,
                        weight = set.weightKg ?: 0.0,
                        reps = set.reps,
                        restSeconds = if (planned.planJson == null) 90 else plan.restSeconds,
                    ))
                }
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

    fun addExercises(exercises: List<Exercise>) = editWorkout { workout ->
        val existing = _state.value.groups.map { it.exercise.id }.toSet()
        database.withTransaction {
            exercises.distinctBy { it.id }.filterNot { it.id in existing }.forEach { exercise ->
                insertExercise(workout.id, exercise, setCount = 3)
            }
        }
    }

    fun setPounds(exerciseId: String, usePounds: Boolean) = changeWorkout("Couldn’t change the weight unit.") {
        val state = _state.value
        val workoutId = state.workout?.id ?: return@changeWorkout
        if (state.groups.none { it.exercise.id == exerciseId }) return@changeWorkout
        val updated = if (usePounds) state.poundExercises + exerciseId else state.poundExercises - exerciseId
        units.save(workoutId, updated)
        _state.value = state.copy(poundExercises = updated)
        getApplication<AuraApp>().workoutNotification.refresh()
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

    fun updateSet(id: Long, weight: Double? = null, reps: Int? = null) = editWorkout { workout ->
        dao.editIncompleteSet(workout.id, id, weight, reps)
    }

    fun toggleCompleted(id: Long): Job {
        val set = _state.value.groups.firstNotNullOfOrNull { group -> group.sets.firstOrNull { it.id == id } }
        return editWorkout { workout ->
            if (set == null) return@editWorkout
            val completed = !set.isCompleted
            val changed = dao.setCompletion(workout.id, id, completed, System.currentTimeMillis())
            if (changed > 0 && completed) haptic()
        }
    }

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
        if (saved) {
            getApplication<AuraApp>().workoutCloud.sync()
            _state.value = _state.value.copy(finishedId = workout.id)
            closeWorkout()
        } else _state.value = _state.value.copy(saving = false, error = "Complete at least one set before finishing.")
    }

    fun discard() = changeWorkout("Couldn’t discard the workout. Try again.") {
        val workout = _state.value.workout ?: return@changeWorkout
        _state.value = _state.value.copy(saving = true, error = null)
        dao.deleteWorkout(workout)
        closeWorkout()
    }

    private fun closeWorkout() {
        _state.value.workout?.let { units.save(it.id, emptySet()) }
        timerJob?.cancel()
        restJob?.cancel()
        setObserver?.cancel()
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
    selectedExerciseIds: List<String> = emptyList(),
    onExerciseHandled: () -> Unit = {},
) {
    val s by vm.state.collectAsState()
    val notification = (LocalContext.current.applicationContext as AuraApp).workoutNotification
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notification.refresh() }
    LaunchedEffect(s.workout?.id) {
        if (s.workout != null && notification.shouldRequestPermission()) {
            notification.markPermissionRequested()
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    var showFinish by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showRest by remember { mutableStateOf(false) }
    val shortWindow = LocalConfiguration.current.screenHeightDp < 480
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var explanation by remember { mutableStateOf<Pair<String, SetColumn>?>(null) }

    LaunchedEffect(routineId, selectedExerciseIds) {
        vm.initialize(routineId).join()
        if (selectedExerciseIds.isNotEmpty()) {
            vm.addExercises(selectedExerciseIds.mapNotNull(ExerciseLib::getById)).join()
            onExerciseHandled()
        }
    }
    LaunchedEffect(s.closed) {
        if (s.closed) {
            val finished = s.finishedId
            if (finished == null) nav.popBackStack()
            else nav.navigate("workout/summary/$finished?celebrate=true") { popUpTo(Routes.WORKOUT) }
        }
    }
    BackHandler(enabled = s.saving) {}

    val workoutStats: @Composable () -> Unit = {
        val completed = s.groups.flatMap { it.sets }.filter { it.isCompleted && it.reps > 0 }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AuraScreenInset, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = V.SurfaceRaised,
        ) {
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatChip("Duration", formatTime(s.elapsed), V.Teal)
                StatChip("Volume", "${workoutNumber(completed.sumOf { it.weight * it.reps })} kg", V.White)
                StatChip("Sets", completed.size.toString(), V.White)
            }
        }

    }
    val workoutActions: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(horizontal = AuraScreenInset, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AuraPrimaryButton(
                "Add exercise",
                { openExercisePicker(nav, s.groups.map { it.exercise.id }) },
                enabled = s.workout != null && !s.saving,
            )
            TextButton(onClick = { showDiscard = true }, enabled = !s.saving && s.workout != null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Discard workout", color = V.Red)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(V.Canvas).statusBarsPadding().navigationBarsPadding().imePadding()) {
        WorkoutHeader("Log workout", { if (!s.saving) nav.popBackStack() }) {
            IconButton(onClick = { showRest = true }) {
                Icon(Icons.Rounded.Timer, "Rest timer", tint = V.Teal)
            }
            Button(
                onClick = { showFinish = true },
                enabled = s.workout != null && !s.saving,
                colors = ButtonDefaults.buttonColors(containerColor = V.TealDim, contentColor = V.ButtonText),
            ) {
                Text("Finish")
            }
        }

        if (!shortWindow && !keyboardOpen) workoutStats()
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
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = AuraScreenInset, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (shortWindow && !keyboardOpen) item { workoutStats() }
            if (s.groups.isEmpty()) item {
                VCard {
                    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExerciseGlyph(56.dp)
                        Text("Get started", style = MaterialTheme.typography.headlineMedium, color = V.White)
                        Text("Add an exercise to start your workout.", style = MaterialTheme.typography.bodyMedium,
                            color = V.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
            items(s.groups, key = { it.exercise.id }) { group ->
                val exerciseId = group.exercise.id
                val unit = if (exerciseId in s.poundExercises) WorkoutWeightUnit.LB else WorkoutWeightUnit.KG
                ExerciseBlock(group, s.workout, vm, unit, { explanation = exerciseId to it },
                    onDetails = { nav.navigate("exercise/${group.exercise.id}") })
            }
            if (shortWindow && !keyboardOpen) item { workoutActions() }
        }
        if (!shortWindow && !keyboardOpen) {
            HorizontalDivider(color = V.SurfaceRaised)
            workoutActions()
        }
    }

    explanation?.let { (exerciseId, column) ->
        val unit = if (exerciseId in s.poundExercises) WorkoutWeightUnit.LB else WorkoutWeightUnit.KG
        SetColumnSheet(column, unit, s.groups.firstOrNull { it.exercise.id == exerciseId }?.exercise?.name, onUnit = { vm.setPounds(exerciseId, it == WorkoutWeightUnit.LB) },
            onClose = { explanation = null })
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
    unit: WorkoutWeightUnit,
    onExplain: (SetColumn) -> Unit,
    onDetails: () -> Unit,
) {
    val dao = (LocalContext.current.applicationContext as AuraApp).db.workoutDao()
    var previous by remember(group.exercise.id, workout?.id) {
        mutableStateOf<Map<Int, WorkoutSetEntity>>(emptyMap())
    }
    LaunchedEffect(group.exercise.id, workout?.id) {
        previous = if (workout == null) emptyMap() else dao.previousExerciseSets(workout, group.exercise.id)
    }

    VCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).clickable(onClick = onDetails).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExerciseThumbnail(group.exercise.imageUrl)
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
                Box(Modifier.width(36.dp).height(48.dp), contentAlignment = Alignment.Center) {
                    Text("#", color = V.Gray, style = MaterialTheme.typography.labelMedium)
                }
                SetHeading("Previous", Modifier.weight(1.35f)) { onExplain(SetColumn.Previous) }
                SetHeading(unit.symbol, Modifier.weight(1f)) { onExplain(SetColumn.Weight) }
                SetHeading("Reps", Modifier.weight(1f)) { onExplain(SetColumn.Reps) }
                Spacer(Modifier.width(48.dp))
            }

            group.sets.forEach { set ->
                key(set.id) {
                    val prior = previous[set.setNumber]
                    SetRow(set, prior, group.exercise.name, vm, unit, onPrevious = { onExplain(SetColumn.Previous) })
                }
            }
            OutlinedButton(
                onClick = { vm.addSet(group.exercise.id) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, V.SurfaceBright),
            ) {
                Icon(Icons.Rounded.Add, null, Modifier.size(20.dp), tint = V.Teal)
                Spacer(Modifier.width(8.dp))
                Text("Add set", color = V.Teal, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SetHeading(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = V.Teal, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetColumnSheet(
    column: SetColumn,
    unit: WorkoutWeightUnit,
    exerciseName: String?,
    onUnit: (WorkoutWeightUnit) -> Unit,
    onClose: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = V.SurfaceRaised, contentColor = V.White) {
        Column(Modifier.fillMaxWidth().padding(start = AuraScreenInset, end = AuraScreenInset, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(column.title, style = MaterialTheme.typography.headlineMedium, color = V.White)
            Text(column.description, style = MaterialTheme.typography.bodyMedium, color = V.Gray)
            if (column == SetColumn.Weight) {
                exerciseName?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = V.White) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkoutWeightUnit.entries.forEach { option ->
                        WorkoutMetricFilter(option.symbol, option == unit) { onUnit(option) }
                    }
                }
            }
            TextButton(onClick = onClose) { Text("Done") }
        }
    }
}

@Composable
private fun SetRow(
    set: WorkoutSetEntity,
    prior: WorkoutSetEntity?,
    exerciseName: String,
    vm: ActiveWorkoutVM,
    unit: WorkoutWeightUnit,
    onPrevious: () -> Unit,
) {
    var wt by rememberSaveable(set.id, unit) {
        val displayed = unit.fromKilograms(set.weight)
        mutableStateOf(if (set.weight > 0) java.math.BigDecimal.valueOf(displayed)
            .setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() else "")
    }
    var rp by rememberSaveable(set.id) { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }
    val weight = parseWeight(wt)
    val reps = rp.toIntOrNull()?.takeIf { it > 0 }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (set.isCompleted) V.Teal.copy(alpha = 0.1f) else V.SurfaceRaised),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.width(36.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(set.setNumber.toString(), color = V.White, style = MaterialTheme.typography.labelMedium)
            weightGain(set, prior)?.let { gain ->
                val increase = workoutNumber(unit.fromKilograms(gain.increaseKg))
                Icon(
                    Icons.Rounded.EmojiEvents,
                    "Set ${set.setNumber} · +$increase ${unit.symbol}",
                    Modifier.size(14.dp),
                    tint = V.Amber,
                )
            }
        }
        Box(
            Modifier.weight(1.35f).heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
                .clickable(onClickLabel = "Explain previous set", role = Role.Button, onClick = onPrevious),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                prior?.let { "${workoutNumber(unit.fromKilograms(it.weight))} × ${it.reps}" } ?: "—",
                style = MaterialTheme.typography.labelMedium,
                color = V.Gray,
                textAlign = TextAlign.Center,
            )
        }
        MiniInput(wt, { text ->
            val parsed = parseWeight(text)
            if (parsed != null || text == "." || text == ",") {
                wt = text
                vm.updateSet(set.id, weight = unit.toKilograms(parsed ?: 0.0))
            }
        }, Modifier.weight(1f), "Set ${set.setNumber} weight (${unit.symbol})", enabled = !set.isCompleted)
        MiniInput(rp, { text ->
            val parsed = text.toIntOrNull()
            if (text.isEmpty() || (parsed != null && parsed >= 0 && text.all(Char::isDigit))) {
                rp = text
                vm.updateSet(set.id, reps = parsed ?: 0)
            }
        }, Modifier.weight(1f), "Set ${set.setNumber} reps", KeyboardType.Number, enabled = !set.isCompleted)
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
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
        modifier = modifier.height(50.dp)
            .clip(RoundedCornerShape(10.dp)).background(V.Canvas)
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
