package com.veyra.ui.screens.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veyra.VeyraApp
import com.veyra.data.local.entity.RoutineEntity
import com.veyra.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WorkoutListState(
    val routines: List<RoutineEntity> = emptyList(),
    val recentWorkouts: List<WorkoutEntity> = emptyList(),
    val activeWorkout: WorkoutEntity? = null,
    val totalWorkouts: Int = 0,
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as VeyraApp).database
    private val dao = db.workoutDao()

    private val _state = MutableStateFlow(WorkoutListState())
    val state: StateFlow<WorkoutListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            dao.getAllRoutines().collect { routines ->
                _state.value = _state.value.copy(routines = routines)
            }
        }
        viewModelScope.launch {
            dao.getWorkoutHistory().collect { workouts ->
                _state.value = _state.value.copy(recentWorkouts = workouts.take(5))
            }
        }
        viewModelScope.launch {
            dao.observeActiveWorkout().collect { active ->
                _state.value = _state.value.copy(activeWorkout = active)
            }
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(totalWorkouts = dao.getTotalWorkoutCount())
        }
    }

    fun createRoutine(name: String) {
        viewModelScope.launch {
            dao.insertRoutine(RoutineEntity(name = name))
        }
    }

    fun deleteRoutine(routine: RoutineEntity) {
        viewModelScope.launch {
            dao.deleteRoutine(routine)
        }
    }

    fun startEmptyWorkout(): Long {
        var id = 0L
        viewModelScope.launch {
            id = dao.insertWorkout(
                WorkoutEntity(name = "Quick Workout", isActive = true)
            )
        }
        return id
    }
}
