package com.veyra.app.data.workout

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val database: VeyraDatabase) {
    private val dao = database.workoutDao()

    val activeWorkout: Flow<WorkoutWithSets?> = dao.observeActiveWorkout()
    val latestWorkout: Flow<WorkoutSummary?> = dao.observeLatestWorkout()
    val workoutHistory: Flow<List<WorkoutSummary>> = dao.observeWorkoutHistory()
    val routines: Flow<List<RoutineWithExercises>> = dao.observeRoutines()

    suspend fun ensureSeedRoutines() = database.withTransaction {
        if (dao.routineCount() != 0) return@withTransaction
        seedRoutine("Push day", listOf("incline_press", "bench", "triceps_extension"), 0)
        seedRoutine("Pull day", listOf("lat_pulldown", "row", "biceps_curl"), 1)
        seedRoutine("Leg + hip", listOf("squat", "leg_press", "leg_curl"), 2)
    }

    suspend fun startRoutine(routineId: Long): Long = database.withTransaction {
        dao.activeWorkout()?.id ?: dao.insertWorkout(
            WorkoutEntity(
                title = dao.routine(routineId)?.routine?.name ?: "Workout",
                startedAtEpochMs = System.currentTimeMillis(),
            )
        ).also { workoutId ->
            val routine = dao.routine(routineId) ?: return@also
            val sets = routine.exercises.sortedBy(RoutineExerciseEntity::position).flatMap { exercise ->
                (1..exercise.targetSets).map { setNumber ->
                    WorkoutSetEntity(
                        workoutId = workoutId,
                        exerciseKey = exercise.exerciseKey,
                        exerciseName = exercise.exerciseName,
                        equipment = exercise.equipment,
                        exercisePosition = exercise.position,
                        setNumber = setNumber,
                        previous = dao.previousPerformance(exercise.exerciseKey, setNumber) ?: "—",
                        weightKg = exercise.defaultWeightKg,
                        reps = exercise.defaultReps,
                        restSeconds = exercise.restSeconds,
                    )
                }
            }
            dao.insertSets(sets)
        }
    }

    suspend fun startEmptyWorkout(): Long = database.withTransaction {
        dao.activeWorkout()?.id ?: dao.insertWorkout(
            WorkoutEntity(title = "Empty workout", startedAtEpochMs = System.currentTimeMillis())
        )
    }

    suspend fun createRoutine(name: String, exerciseKeys: List<String>): Long = database.withTransaction {
        val id = dao.insertRoutine(RoutineEntity(name = name, createdAtEpochMs = System.currentTimeMillis()))
        dao.insertRoutineExercises(exerciseKeys.mapIndexedNotNull { index, key ->
            WorkoutCatalog.byKey(key)?.toRoutineExercise(id, index)
        })
        id
    }

    suspend fun updateRoutine(routineId: Long, name: String, exerciseKeys: List<String>) = database.withTransaction {
        dao.updateRoutineName(routineId, name)
        dao.deleteRoutineExercises(routineId)
        dao.insertRoutineExercises(exerciseKeys.mapIndexedNotNull { index, key ->
            WorkoutCatalog.byKey(key)?.toRoutineExercise(routineId, index)
        })
    }

    suspend fun updateWeight(setId: Long, weightKg: String) = dao.updateWeight(setId, weightKg)

    suspend fun updateReps(setId: Long, reps: String) = dao.updateReps(setId, reps)

    suspend fun addSet(workoutId: Long, exerciseKey: String) {
        val workout = dao.workoutWithSets(workoutId) ?: return
        val exerciseSets = workout.sets.filter { it.exerciseKey == exerciseKey }
        val last = exerciseSets.maxByOrNull(WorkoutSetEntity::setNumber) ?: return
        val nextNumber = (dao.maxSetNumber(workoutId, exerciseKey) ?: 0) + 1
        dao.insertSet(
            last.copy(
                id = 0,
                setNumber = nextNumber,
                previous = dao.previousPerformance(exerciseKey, nextNumber) ?: "—",
                completedAtEpochMs = null,
            )
        )
    }

    suspend fun addExercise(workoutId: Long, exerciseKey: String) {
        val template = WorkoutCatalog.byKey(exerciseKey) ?: return
        val workout = dao.workoutWithSets(workoutId) ?: return
        if (workout.sets.any { it.exerciseKey == exerciseKey }) return
        val position = (dao.maxExercisePosition(workoutId) ?: -1) + 1
        dao.insertSets((1..3).map { setNumber ->
            WorkoutSetEntity(
                workoutId = workoutId,
                exerciseKey = template.key,
                exerciseName = template.name,
                equipment = template.equipment,
                exercisePosition = position,
                setNumber = setNumber,
                previous = dao.previousPerformance(exerciseKey, setNumber) ?: "—",
                weightKg = template.defaultWeightKg,
                reps = template.defaultReps,
                restSeconds = template.restSeconds,
            )
        })
    }

    suspend fun updateExerciseRest(workoutId: Long, exerciseKey: String, seconds: Int) =
        dao.updateExerciseRest(workoutId, exerciseKey, seconds)

    suspend fun updateExerciseNote(workoutId: Long, exerciseKey: String, note: String) =
        dao.updateExerciseNote(workoutId, exerciseKey, note)

    suspend fun toggleSet(workout: WorkoutEntity, set: WorkoutSetEntity) {
        val now = System.currentTimeMillis()
        val completedAt = if (set.completedAtEpochMs == null) now else null
        dao.updateSetCompletion(set.id, completedAt)
        dao.updateRestTimer(
            workout.id,
            completedAt?.let { it + set.restSeconds * 1_000L },
        )
    }

    suspend fun adjustRest(workout: WorkoutEntity, seconds: Int) {
        val now = System.currentTimeMillis()
        val current = workout.restUntilEpochMs?.coerceAtLeast(now) ?: now
        dao.updateRestTimer(workout.id, (current + seconds * 1_000L).takeIf { it > now })
    }

    suspend fun finish(workoutId: Long) {
        dao.finishWorkout(workoutId, System.currentTimeMillis())
    }

    suspend fun discard(workoutId: Long) = database.withTransaction {
        dao.deleteWorkoutSets(workoutId)
        dao.deleteWorkout(workoutId)
    }

    suspend fun workout(workoutId: Long): WorkoutWithSets? = dao.workoutWithSets(workoutId)

    suspend fun markHealthConnectSynced(workoutId: Long, recordId: String) {
        dao.markHealthConnectSynced(workoutId, recordId)
    }

    fun exerciseHistory(exerciseKey: String): Flow<List<ExerciseHistoryPoint>> =
        dao.observeExerciseHistory(exerciseKey)

    private suspend fun seedRoutine(name: String, keys: List<String>, offset: Int) {
        val id = dao.insertRoutine(
            RoutineEntity(name = name, createdAtEpochMs = System.currentTimeMillis() + offset)
        )
        dao.insertRoutineExercises(keys.mapIndexedNotNull { index, key ->
            WorkoutCatalog.byKey(key)?.toRoutineExercise(id, index)
        })
    }
}

data class ExerciseTemplate(
    val key: String,
    val name: String,
    val equipment: String,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val cue: String,
    val defaultWeightKg: String,
    val defaultReps: String = "10",
    val restSeconds: Int = 90,
) {
    fun toRoutineExercise(routineId: Long, position: Int) = RoutineExerciseEntity(
        routineId = routineId,
        exerciseKey = key,
        exerciseName = name,
        equipment = equipment,
        position = position,
        defaultWeightKg = defaultWeightKg,
        defaultReps = defaultReps,
        restSeconds = restSeconds,
    )
}

object WorkoutCatalog {
    val exercises = listOf(
        ExerciseTemplate("incline_press", "Incline dumbbell press", "Dumbbells · Bench", listOf("Upper chest"), listOf("Triceps", "Front delts"), "Set the bench to 30°. Keep wrists stacked over elbows.", "10", "12"),
        ExerciseTemplate("bench", "Barbell bench press", "Barbell · Bench", listOf("Chest"), listOf("Triceps", "Front delts"), "Pin the shoulder blades and press slightly back.", "70", "8"),
        ExerciseTemplate("triceps_extension", "Triceps extension", "Dumbbell", listOf("Triceps"), listOf("Shoulders"), "Keep the upper arm still and extend smoothly.", "10", "10"),
        ExerciseTemplate("lat_pulldown", "Lat pulldown", "Cable", listOf("Lats"), listOf("Biceps", "Upper back"), "Drive elbows toward your sides without leaning back.", "45", "10"),
        ExerciseTemplate("row", "Chest-supported row", "Dumbbells · Incline bench", listOf("Upper back", "Lats"), listOf("Rear delts", "Biceps"), "Keep the chest planted and pull elbows toward the hips.", "24", "10"),
        ExerciseTemplate("biceps_curl", "Biceps curl", "Dumbbells", listOf("Biceps"), listOf("Forearms"), "Keep elbows quiet and control the lowering phase.", "10", "12"),
        ExerciseTemplate("squat", "Back squat", "Barbell · Rack", listOf("Quadriceps", "Glutes"), listOf("Adductors", "Core"), "Brace before descending and keep the bar over mid-foot.", "60", "8", 120),
        ExerciseTemplate("leg_press", "Leg press", "Machine", listOf("Quadriceps", "Glutes"), listOf("Hamstrings"), "Lower with control and keep the lower back supported.", "90", "10", 120),
        ExerciseTemplate("leg_curl", "Seated leg curl", "Machine", listOf("Hamstrings"), listOf("Calves"), "Keep hips down and squeeze through the full range.", "35", "12"),
    )

    fun byKey(key: String): ExerciseTemplate? = exercises.firstOrNull { it.key == key }
}
