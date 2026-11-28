package com.aura.ui.screens.workout

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.room.withTransaction
import com.aura.AuraApp
import com.aura.data.local.entity.*
import com.aura.data.model.ExerciseLib
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.AuraTextField
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.math.BigDecimal

internal data class DraftSet(val weight: String = "", val reps: String = "10") {
    fun parsed(): PlannedSet? {
        val number = weight.replace(',', '.').toDoubleOrNull()
        if (weight.isNotBlank() && (number == null || !number.isFinite() || number < 0)) return null
        val count = reps.toIntOrNull()?.takeIf { it > 0 } ?: return null
        return PlannedSet(number, count)
    }
}

internal data class DraftExercise(
    val id: String,
    val sets: List<DraftSet> = List(3) { DraftSet() },
    val rest: String = "",
    val note: String = "",
) {
    fun plan(): RoutinePlan? {
        val seconds = if (rest.isBlank()) 0 else rest.toIntOrNull()?.takeIf { it >= 0 } ?: return null
        val parsedSets = sets.map { it.parsed() ?: return null }
        return RoutinePlan(parsedSets, seconds, note.trim())
    }
}

internal class RoutineEditorVM(application: Application) : AndroidViewModel(application) {
    private val app = application as AuraApp
    private val dao = app.db.workoutDao()
    private var original: RoutineEntity? = null
    var name by mutableStateOf("")
    var exercises by mutableStateOf<List<DraftExercise>>(emptyList())
        private set
    var isReady by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    suspend fun load(id: Long) {
        if (isReady) return
        val routine = dao.getRoutineById(id)
        if (routine == null || (routine.cloudOwnerUid != null && routine.cloudOwnerUid != app.workoutCloud.ownerUid)) {
            error = "This routine is unavailable."
            return
        }
        original = routine
        name = routine.name
        exercises = dao.getRoutineExercises(id).map { exercise ->
            val plan = exercise.plan()
            DraftExercise(exercise.exerciseId, plan.sets.map { set ->
                DraftSet(set.weightKg?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty(), set.reps.toString())
            }.ifEmpty { listOf(DraftSet()) }, plan.restSeconds.takeIf { it > 0 }?.toString().orEmpty(), plan.note)
        }
        isReady = true
    }

    fun add(ids: List<String>) {
        val existing = exercises.map { it.id }.toSet()
        exercises = exercises + ids.distinct().filterNot { it in existing }.map { DraftExercise(it) }
    }

    fun edit(id: String, transform: (DraftExercise) -> DraftExercise) {
        exercises = exercises.map { if (it.id == id) transform(it) else it }
    }

    fun remove(id: String) { exercises = exercises.filterNot { it.id == id } }

    fun move(index: Int, offset: Int) {
        val target = index + offset
        if (target !in exercises.indices) return
        exercises = exercises.toMutableList().apply { add(target, removeAt(index)) }
    }

    fun save(onSaved: () -> Unit) {
        val routine = original ?: return
        if (saving) return
        val title = name.trim()
        if (title.isEmpty()) {
            error = "Enter a routine name."
            return
        }
        val planned = exercises.map { exercise ->
            val plan = exercise.plan()
            if (plan == null) {
                error = "Enter valid weights, positive reps, and rest in seconds."
                return
            }
            exercise.id to plan
        }

        saving = true
        error = null
        viewModelScope.launch {
            try {
                app.db.withTransaction {
                    val stored = checkNotNull(dao.getRoutineById(routine.id))
                    check(stored.updatedAt == routine.updatedAt)
                    check(stored.cloudOwnerUid == null || stored.cloudOwnerUid == app.workoutCloud.ownerUid)
                    dao.updateRoutinePlan(routine.id, title, System.currentTimeMillis())
                    dao.clearRoutineExercises(routine.id)
                    planned.forEachIndexed { index, (exerciseId, plan) ->
                        dao.insertRoutineExercise(RoutineExerciseEntity(
                            routineId = routine.id,
                            exerciseId = exerciseId,
                            sortOrder = index,
                            targetSets = plan.sets.size,
                            targetReps = plan.sets.first().reps,
                            planJson = plan.encode(),
                        ))
                    }
                }
                app.workoutCloud.sync()
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "Couldn’t update the routine. If it changed elsewhere, reopen the editor."
            } finally {
                saving = false
            }
        }
    }
}

@Composable
internal fun RoutineEditorScreen(
    nav: NavController,
    routineId: Long,
    selectedIds: List<String>,
    onSelectionHandled: () -> Unit,
    vm: RoutineEditorVM = viewModel(),
) {
    LaunchedEffect(routineId, selectedIds) {
        vm.load(routineId)
        if (vm.isReady && selectedIds.isNotEmpty()) {
            vm.add(selectedIds)
            onSelectionHandled()
        }
    }
    BackHandler(enabled = vm.saving) {}
    Column(Modifier.fillMaxSize().background(V.Canvas).statusBarsPadding().navigationBarsPadding().imePadding()) {
        WorkoutHeader("Edit routine", { if (!vm.saving) nav.popBackStack() }) {
            TextButton(onClick = { nav.popBackStack() }, enabled = !vm.saving) { Text("Cancel") }
            Button(
                onClick = { vm.save { nav.popBackStack() } },
                enabled = vm.isReady && !vm.saving,
                colors = ButtonDefaults.buttonColors(containerColor = V.TealDim, contentColor = V.ButtonText),
            ) { Text("Update") }
        }
        vm.error?.let { Text(it, Modifier.padding(horizontal = AuraScreenInset, vertical = 8.dp), color = V.Error) }
        if (!vm.isReady) return@Column
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(AuraScreenInset),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { AuraTextField(vm.name, { vm.name = it }, "Routine name") }
            itemsIndexed(vm.exercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                DraftExerciseCard(
                    exercise = exercise,
                    onChange = { updated -> vm.edit(exercise.id) { updated } },
                    onUp = { vm.move(index, -1) },
                    onDown = { vm.move(index, 1) },
                    canMoveUp = index > 0,
                    canMoveDown = index < vm.exercises.lastIndex,
                    onRemove = { vm.remove(exercise.id) },
                )
            }
        }
        AuraPrimaryButton(
            "Add exercise",
            onClick = { openExercisePicker(nav, vm.exercises.map { it.id }) },
            modifier = Modifier.padding(horizontal = AuraScreenInset, vertical = 8.dp),
            enabled = !vm.saving,
        )
    }
}

@Composable
private fun DraftExerciseCard(
    exercise: DraftExercise,
    onChange: (DraftExercise) -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRemove: () -> Unit,
) {
    val info = ExerciseLib.getById(exercise.id)
    var menu by remember { mutableStateOf(false) }
    var showRest by remember { mutableStateOf(exercise.rest.isNotBlank()) }
    var showNote by remember { mutableStateOf(exercise.note.isNotBlank()) }
    VCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExerciseThumbnail(info?.imageUrl)
                Text(info?.name ?: "Exercise unavailable", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = V.White)
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Rounded.MoreVert, "Exercise options", tint = V.Gray)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Move up") }, enabled = canMoveUp, onClick = { menu = false; onUp() })
                        DropdownMenuItem(text = { Text("Move down") }, enabled = canMoveDown, onClick = { menu = false; onDown() })
                        DropdownMenuItem(text = { Text("Remove exercise", color = V.Red) }, onClick = { menu = false; onRemove() })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkoutAction(exercise.rest.takeIf { it.isNotBlank() }?.let { "Rest · $it s" } ?: "Rest",
                    Icons.Rounded.Timer, { showRest = !showRest }, Modifier.weight(1f))
                WorkoutAction("Note", Icons.Rounded.Edit, { showNote = !showNote }, Modifier.weight(1f))
            }
            if (showRest) AuraTextField(
                exercise.rest, { onChange(exercise.copy(rest = it)) }, "Rest in seconds",
                keyboardType = KeyboardType.Number,
            )
            if (showNote) AuraTextField(exercise.note, { onChange(exercise.copy(note = it)) }, "Exercise note")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Set", Modifier.width(28.dp), style = MaterialTheme.typography.labelMedium, color = V.Gray)
                Text("Weight (kg)", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = V.Gray)
                Text("Reps", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = V.Gray)
                Spacer(Modifier.width(48.dp))
            }
            exercise.sets.forEachIndexed { index, set ->
                fun replace(new: DraftSet) {
                    onChange(exercise.copy(sets = exercise.sets.toMutableList().apply { this[index] = new }))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", Modifier.width(28.dp), color = V.White)
                    PlannedSetInput(set.weight, { replace(set.copy(weight = it)) }, "Weight", Modifier.weight(1f), KeyboardType.Decimal)
                    PlannedSetInput(set.reps, { replace(set.copy(reps = it)) }, "Reps", Modifier.weight(1f), KeyboardType.Number)
                    IconButton(
                        enabled = exercise.sets.size > 1,
                        onClick = { onChange(exercise.copy(sets = exercise.sets.filterIndexed { i, _ -> i != index })) },
                    ) { Icon(Icons.Rounded.Close, "Remove set ${index + 1}") }
                }
            }
            WorkoutAction(
                "Add set", Icons.Rounded.Add,
                { onChange(exercise.copy(sets = exercise.sets + exercise.sets.last())) },
                Modifier.fillMaxWidth(), prominent = true,
            )
        }
    }
}

@Composable
private fun PlannedSetInput(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier,
    keyboard: KeyboardType,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        singleLine = true,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.labelMedium, color = V.Gray) },
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = V.Teal,
            unfocusedBorderColor = V.SurfaceBright,
            focusedTextColor = V.White,
            unfocusedTextColor = V.White,
        ),
    )
}
