package com.veyra.ui.screens.workout

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veyra.VeyraApp
import com.veyra.data.local.entity.WorkoutEntity
import com.veyra.data.local.entity.WorkoutSetEntity
import com.veyra.data.model.Exercise
import com.veyra.data.model.ExerciseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

data class ActiveWorkoutState(
    val workout: WorkoutEntity? = null,
    val exercises: List<WorkoutExerciseGroup> = emptyList(),
    val elapsedSeconds: Int = 0,
    val restTimerSeconds: Int = 0,
    val restTimerTotal: Int = 90,
    val isRestTimerRunning: Boolean = false,
    val isFinishing: Boolean = false,
)

data class WorkoutExerciseGroup(
    val exercise: Exercise,
    val sets: List<WorkoutSetEntity>,
    val previousBest: WorkoutSetEntity? = null,
)

class ActiveWorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as VeyraApp
    private val dao = app.database.workoutDao()
    private val healthManager = app.healthConnectManager

    private val _state = MutableStateFlow(ActiveWorkoutState())
    val state: StateFlow<ActiveWorkoutState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var restTimerJob: Job? = null
    private var workoutId: Long = -1

    fun initialize(routineId: Long?) {
        viewModelScope.launch {
            // Check for existing active workout
            var workout = dao.getActiveWorkout()
            if (workout == null) {
                val name = if (routineId != null) {
                    dao.getRoutineById(routineId)?.name ?: "Workout"
                } else "Quick Workout"

                val id = dao.insertWorkout(
                    WorkoutEntity(name = name, routineId = routineId, isActive = true)
                )
                workout = dao.getWorkoutById(id)
            }

            workout?.let {
                workoutId = it.id
                _state.value = _state.value.copy(workout = it)

                // Load existing sets
                loadSets()

                // Load routine exercises if starting from routine
                if (routineId != null && _state.value.exercises.isEmpty()) {
                    val routineExercises = dao.getRoutineExercises(routineId)
                    for (re in routineExercises) {
                        val exercise = ExerciseRepository.getById(re.exerciseId) ?: continue
                        addExerciseToWorkout(exercise, re.targetSets)
                    }
                }
            }

            startTimer()
        }
    }

    private suspend fun loadSets() {
        val sets = dao.getWorkoutSetsList(workoutId)
        val grouped = sets.groupBy { it.exerciseId }
        val exerciseGroups = grouped.mapNotNull { (exerciseId, setSets) ->
            val exercise = ExerciseRepository.getById(exerciseId) ?: return@mapNotNull null
            val previousBest = dao.getPersonalBestSet(exerciseId)
            WorkoutExerciseGroup(exercise, setSets, previousBest)
        }
        _state.value = _state.value.copy(exercises = exerciseGroups)
    }

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            addExerciseToWorkout(exercise, 3)
        }
    }

    private suspend fun addExerciseToWorkout(exercise: Exercise, setCount: Int) {
        val existingSets = _state.value.exercises
            .find { it.exercise.id == exercise.id }?.sets?.size ?: 0
        if (existingSets > 0) return

        for (i in 1..setCount) {
            dao.insertSet(
                WorkoutSetEntity(
                    workoutId = workoutId,
                    exerciseId = exercise.id,
                    setNumber = i,
                )
            )
        }
        loadSets()
    }

    fun addSet(exerciseId: String) {
        viewModelScope.launch {
            val currentSets = _state.value.exercises
                .find { it.exercise.id == exerciseId }?.sets?.size ?: 0
            dao.insertSet(
                WorkoutSetEntity(
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    setNumber = currentSets + 1,
                )
            )
            loadSets()
        }
    }

    fun updateSet(set: WorkoutSetEntity) {
        viewModelScope.launch {
            dao.updateSet(set)
            loadSets()
        }
    }

    fun completeSet(set: WorkoutSetEntity) {
        viewModelScope.launch {
            dao.updateSet(set.copy(isCompleted = true, timestamp = System.currentTimeMillis()))
            triggerHaptic()
            loadSets()
            startRestTimer(set.restSeconds)
        }
    }

    fun deleteSet(set: WorkoutSetEntity) {
        viewModelScope.launch {
            dao.deleteSet(set)
            loadSets()
        }
    }

    fun removeExercise(exerciseId: String) {
        viewModelScope.launch {
            dao.deleteSetsForExercise(workoutId, exerciseId)
            loadSets()
        }
    }

    fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        _state.value = _state.value.copy(
            restTimerSeconds = seconds,
            restTimerTotal = seconds,
            isRestTimerRunning = true,
        )
        restTimerJob = viewModelScope.launch {
            while (_state.value.restTimerSeconds > 0) {
                delay(1000)
                _state.value = _state.value.copy(
                    restTimerSeconds = _state.value.restTimerSeconds - 1,
                )
            }
            _state.value = _state.value.copy(isRestTimerRunning = false)
            triggerHaptic()
        }
    }

    fun stopRestTimer() {
        restTimerJob?.cancel()
        _state.value = _state.value.copy(isRestTimerRunning = false, restTimerSeconds = 0)
    }

    fun finishWorkout() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isFinishing = true)
            val sets = dao.getWorkoutSetsList(workoutId)
            val totalVolume = sets.filter { it.isCompleted }.sumOf { it.weight * it.reps }
            val workout = _state.value.workout ?: return@launch
            val endTime = System.currentTimeMillis()
            val duration = ((endTime - workout.startTime) / 1000).toInt()

            dao.updateWorkout(
                workout.copy(
                    isActive = false,
                    endTime = endTime,
                    durationSeconds = duration,
                    totalVolume = totalVolume,
                )
            )

            // Export to Health Connect
            try {
                healthManager.writeExerciseSession(
                    title = workout.name,
                    startTime = Instant.ofEpochMilli(workout.startTime),
                    endTime = Instant.ofEpochMilli(endTime),
                )
            } catch (_: Exception) {}

            timerJob?.cancel()
            restTimerJob?.cancel()
        }
    }

    fun discardWorkout() {
        viewModelScope.launch {
            _state.value.workout?.let { dao.deleteWorkout(it) }
            timerJob?.cancel()
            restTimerJob?.cancel()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val workout = _state.value.workout ?: break
                val elapsed = ((System.currentTimeMillis() - workout.startTime) / 1000).toInt()
                _state.value = _state.value.copy(elapsedSeconds = elapsed)
                delay(1000)
            }
        }
    }

    private fun triggerHaptic() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getApplication<Application>()
                    .getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getApplication<Application>()
                    .getSystemService(Vibrator::class.java)
            }
            vibrator?.vibrate(
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } catch (_: Exception) {}
    }

    fun estimateOneRepMax(weight: Double, reps: Int): Double {
        if (reps <= 0 || weight <= 0) return 0.0
        if (reps == 1) return weight
        // Brzycki formula
        return weight * (36.0 / (37.0 - reps))
    }
}
