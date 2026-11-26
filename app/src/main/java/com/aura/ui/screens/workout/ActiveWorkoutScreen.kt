package com.aura.ui.screens.workout

import android.app.Application
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ━━━ ViewModel ━━━
class ActiveWorkoutVM(app: Application) : AndroidViewModel(app) {
    private val dao = (app as AuraApp).db.workoutDao()

    data class ExGroup(val exercise: Exercise, val sets: List<WorkoutSetEntity>, val prevBest: WorkoutSetEntity?)
    data class State(
        val workout: WorkoutEntity? = null,
        val groups: List<ExGroup> = emptyList(),
        val elapsed: Int = 0,
        val restSec: Int = 0,
        val restTotal: Int = 90,
        val resting: Boolean = false,
        val finishing: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state
    private var workoutId = -1L
    private var timerJob: Job? = null
    private var restJob: Job? = null

    fun init(routineId: Long?) = viewModelScope.launch {
        var w = dao.getActiveWorkout()
        if (w == null) {
            val name = if (routineId != null) dao.getRoutineById(routineId)?.name ?: "Workout" else "Quick Workout"
            val id = dao.insertWorkout(WorkoutEntity(name = name, routineId = routineId, isActive = true))
            w = dao.getWorkoutById(id)
        }
        w?.let {
            workoutId = it.id
            _state.value = _state.value.copy(workout = it)
            loadSets()
            if (routineId != null && _state.value.groups.isEmpty()) {
                dao.getRoutineExercises(routineId).forEach { re ->
                    ExerciseLib.getById(re.exerciseId)?.let { ex -> addExercise(ex, re.targetSets) }
                }
            }
        }
        startTimer()
    }

    private suspend fun loadSets() {
        val sets = dao.getWorkoutSetsList(workoutId)
        val groups = sets.groupBy { it.exerciseId }.mapNotNull { (exId, s) ->
            val ex = ExerciseLib.getById(exId) ?: return@mapNotNull null
            val best = dao.getPersonalBestSet(exId)
            ExGroup(ex, s, best)
        }
        _state.value = _state.value.copy(groups = groups)
    }

    fun addExercise(ex: Exercise, setCount: Int = 3) = viewModelScope.launch {
        if (_state.value.groups.any { it.exercise.id == ex.id }) return@launch
        repeat(setCount) { i ->
            dao.insertSet(WorkoutSetEntity(workoutId = workoutId, exerciseId = ex.id, exerciseName = ex.name, setNumber = i + 1))
        }
        loadSets()
    }

    fun addSet(exerciseId: String) = viewModelScope.launch {
        val count = _state.value.groups.find { it.exercise.id == exerciseId }?.sets?.size ?: 0
        dao.insertSet(WorkoutSetEntity(workoutId = workoutId, exerciseId = exerciseId, setNumber = count + 1))
        loadSets()
    }

    fun updateSet(set: WorkoutSetEntity) = viewModelScope.launch { dao.updateSet(set); loadSets() }

    fun completeSet(set: WorkoutSetEntity) = viewModelScope.launch {
        dao.updateSet(set.copy(isCompleted = true, timestamp = System.currentTimeMillis()))
        haptic()
        loadSets()
    }

    fun removeExercise(exerciseId: String) = viewModelScope.launch {
        dao.deleteSetsForExercise(workoutId, exerciseId); loadSets()
    }

    fun startRest(sec: Int) {
        restJob?.cancel()
        _state.value = _state.value.copy(restSec = sec, restTotal = sec, resting = true)
        restJob = viewModelScope.launch {
            while (_state.value.restSec > 0) { delay(1000); _state.value = _state.value.copy(restSec = _state.value.restSec - 1) }
            _state.value = _state.value.copy(resting = false)
            haptic()
        }
    }

    fun skipRest() { restJob?.cancel(); _state.value = _state.value.copy(resting = false, restSec = 0) }

    fun finish() = viewModelScope.launch {
        _state.value = _state.value.copy(finishing = true)
        val sets = dao.getWorkoutSetsList(workoutId)
        val vol = sets.filter { it.isCompleted }.sumOf { it.weight * it.reps }
        val w = _state.value.workout ?: return@launch
        val end = System.currentTimeMillis()
        dao.updateWorkout(w.copy(isActive = false, endTime = end, durationSeconds = ((end - w.startTime) / 1000).toInt(), totalVolume = vol))
        timerJob?.cancel(); restJob?.cancel()
    }

    fun discard() = viewModelScope.launch {
        _state.value.workout?.let { dao.deleteWorkout(it) }
        timerJob?.cancel(); restJob?.cancel()
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

    fun e1rm(w: Double, r: Int): Double = if (r <= 0 || w <= 0) 0.0 else if (r == 1) w else w * (36.0 / (37.0 - r))
}

// ━━━ Screen ━━━
@Composable
fun ActiveWorkoutScreen(nav: NavController, routineId: Long?, vm: ActiveWorkoutVM = viewModel()) {
    val s by vm.state.collectAsState()
    var showFinish by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.init(routineId) }
    LaunchedEffect(s.finishing) { if (s.finishing) nav.popBackStack() }

    Column(Modifier.fillMaxSize().background(V.Black).statusBarsPadding()) {
        // Top bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { showDiscard = true }) { Icon(Icons.Rounded.Close, "Close", tint = V.Gray) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.workout?.name ?: "Workout", style = MaterialTheme.typography.titleLarge, color = V.White)
            }
            TextButton(onClick = { showFinish = true }) { Text("Finish", color = V.Teal, fontWeight = FontWeight.Bold) }
        }

        // Stats row
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            val sets = s.groups.flatMap { it.sets }
            val completed = sets.count { it.isCompleted }
            val vol = sets.filter { it.isCompleted }.sumOf { it.weight * it.reps }
            StatChip("Exercises", "${s.groups.size}", V.Teal)
            StatChip("Volume", "%.0f kg".format(vol), V.White)
            StatChip("Sets", "$completed", V.White)
        }

        // Exercise groups
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(s.groups) { group ->
                ExerciseBlock(group, vm)
            }
            item {
                AuraPrimaryButton("+ Add Exercise", onClick = { nav.navigate("exercise/search") },
                    modifier = Modifier.padding(horizontal = AuraScreenInset))
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = AuraScreenInset), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(9999.dp)).background(V.SurfaceRaised)
                        .clickable { showDiscard = true }, contentAlignment = Alignment.Center) {
                        Text("Discard", style = MaterialTheme.typography.labelLarge, color = V.Red)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
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
            confirmButton = { TextButton(onClick = { showDiscard = false; vm.discard(); nav.popBackStack() }) { Text("Discard", color = V.Red) } },
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
private fun ExerciseBlock(group: ActiveWorkoutVM.ExGroup, vm: ActiveWorkoutVM) {
    VCard(Modifier.padding(horizontal = AuraScreenInset)) {
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(group.exercise.name, style = MaterialTheme.typography.titleLarge, color = V.Teal, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.removeExercise(group.exercise.id) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Delete, "Remove", tint = V.Gray, modifier = Modifier.size(16.dp))
                }
            }
            if (group.prevBest != null) {
                Text("PR: ${group.prevBest.weight}kg × ${group.prevBest.reps}", style = MaterialTheme.typography.bodySmall, color = V.Gray)
            }
            Spacer(Modifier.height(8.dp))

            // Headers
            Row(Modifier.fillMaxWidth()) {
                Text("SET", Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = V.Gray, textAlign = TextAlign.Center)
                Text("PREV", Modifier.width(68.dp), style = MaterialTheme.typography.labelSmall, color = V.Gray, textAlign = TextAlign.Center)
                Text("KG", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = V.Gray, textAlign = TextAlign.Center)
                Text("REPS", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = V.Gray, textAlign = TextAlign.Center)
                Spacer(Modifier.width(36.dp))
            }
            Spacer(Modifier.height(4.dp))

            group.sets.forEach { set -> SetRow(set, vm) }

            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(V.SurfaceRaised)
                    .clickable { vm.addSet(group.exercise.id) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("+ Add Set", style = MaterialTheme.typography.labelMedium, color = V.Gray)
            }
        }
    }
}

@Composable
private fun SetRow(set: WorkoutSetEntity, vm: ActiveWorkoutVM) {
    var wt by remember(set.id) { mutableStateOf(if (set.weight > 0) set.weight.toString() else "") }
    var rp by remember(set.id) { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }
    val bg = if (set.isCompleted) V.Teal.copy(alpha = 0.06f) else V.SurfaceRaised.copy(alpha = 0.3f)

    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp)).background(bg).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(set.setNumber.toString(), Modifier.width(40.dp), style = MaterialTheme.typography.labelLarge, color = V.Gray, textAlign = TextAlign.Center)
        Text("-", Modifier.width(68.dp), style = MaterialTheme.typography.bodySmall, color = V.Gray, textAlign = TextAlign.Center)
        MiniInput(wt, { wt = it; vm.updateSet(set.copy(weight = it.toDoubleOrNull() ?: 0.0)) }, Modifier.weight(1f))
        MiniInput(rp, { rp = it; vm.updateSet(set.copy(reps = it.toIntOrNull() ?: 0)) }, Modifier.weight(1f), KeyboardType.Number)
        Box(
            Modifier.size(32.dp).clip(CircleShape)
                .background(if (set.isCompleted) V.Teal else V.SurfaceRaised)
                .then(if (!set.isCompleted) Modifier.border(1.dp, V.SurfaceBright, CircleShape) else Modifier)
                .clickable { if (!set.isCompleted) vm.completeSet(set.copy(weight = wt.toDoubleOrNull() ?: 0.0, reps = rp.toIntOrNull() ?: 0)) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Check, null, tint = if (set.isCompleted) V.Black else V.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun MiniInput(value: String, onChange: (String) -> Unit, modifier: Modifier, kt: KeyboardType = KeyboardType.Decimal) {
    val style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.padding(horizontal = 4.dp).height(40.dp)
            .clip(RoundedCornerShape(8.dp)).background(V.Black),
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
