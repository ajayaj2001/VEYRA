package com.veyra.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.health.connect.client.HealthConnectClient
import com.veyra.app.data.workout.VeyraDatabase
import com.veyra.app.data.workout.WorkoutEntity
import com.veyra.app.data.workout.WorkoutRepository
import com.veyra.app.data.workout.WorkoutSetEntity
import com.veyra.app.data.workout.WorkoutSummary
import com.veyra.app.data.workout.WorkoutHealthExporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveWorkoutState(
    val workout: WorkoutEntity,
    val exercises: List<ActiveExercise>,
)

data class ActiveExercise(
    val key: String,
    val name: String,
    val equipment: String,
    val sets: List<WorkoutSetEntity>,
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(VeyraDatabase.get(application))
    private val healthExporter = WorkoutHealthExporter(application)

    val healthWritePermission: Set<String> = setOf(healthExporter.writePermission)

    val activeWorkout: StateFlow<ActiveWorkoutState?> = repository.activeWorkout
        .map { relation ->
            relation?.let {
                ActiveWorkoutState(
                    workout = it.workout,
                    exercises = it.sets
                        .groupBy(WorkoutSetEntity::exerciseKey)
                        .values
                        .sortedBy { sets -> sets.first().exercisePosition }
                        .map { sets ->
                            val first = sets.first()
                            ActiveExercise(
                                key = first.exerciseKey,
                                name = first.exerciseName,
                                equipment = first.equipment,
                                sets = sets.sortedBy(WorkoutSetEntity::setNumber),
                            )
                        },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val latestWorkout: StateFlow<WorkoutSummary?> = repository.latestWorkout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val workoutHistory: StateFlow<List<WorkoutSummary>> = repository.workoutHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun startPushWorkout(onStarted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.startPushWorkout()
            onStarted()
        }
    }

    fun updateWeight(set: WorkoutSetEntity, value: String) {
        if (value.length <= 6 && value.count { it == '.' } <= 1 && value.all { it.isDigit() || it == '.' }) {
            viewModelScope.launch { repository.updateWeight(set.id, value) }
        }
    }

    fun updateReps(set: WorkoutSetEntity, value: String) {
        if (value.length <= 3 && value.all(Char::isDigit)) {
            viewModelScope.launch { repository.updateReps(set.id, value) }
        }
    }

    fun toggleSet(set: WorkoutSetEntity) {
        val workout = activeWorkout.value?.workout ?: return
        viewModelScope.launch { repository.toggleSet(workout, set) }
    }

    fun adjustRest(seconds: Int) {
        val workout = activeWorkout.value?.workout ?: return
        viewModelScope.launch { repository.adjustRest(workout, seconds) }
    }

    fun checkHealthWriteAccess(onResult: (hasPermission: Boolean, canRequest: Boolean) -> Unit) {
        viewModelScope.launch {
            val available = healthExporter.availability == HealthConnectClient.SDK_AVAILABLE
            val granted = available && runCatching { healthExporter.hasWritePermission() }.getOrDefault(false)
            onResult(granted, available)
        }
    }

    fun finish(exportToHealthConnect: Boolean, onFinished: () -> Unit) {
        val workoutId = activeWorkout.value?.workout?.id ?: return
        viewModelScope.launch {
            repository.finish(workoutId)
            if (exportToHealthConnect) {
                repository.workout(workoutId)?.let { workout ->
                    runCatching { healthExporter.export(workout) }
                        .onSuccess { recordId -> repository.markHealthConnectSynced(workoutId, recordId) }
                }
            }
            onFinished()
        }
    }

    fun syncWorkout(workoutId: Long) {
        viewModelScope.launch {
            val workout = repository.workout(workoutId) ?: return@launch
            runCatching { healthExporter.export(workout) }
                .onSuccess { recordId -> repository.markHealthConnectSynced(workoutId, recordId) }
        }
    }
}
