package com.veyra.app.data.workout

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val database: VeyraDatabase) {
    private val dao = database.workoutDao()

    val activeWorkout: Flow<WorkoutWithSets?> = dao.observeActiveWorkout()
    val latestWorkout: Flow<WorkoutSummary?> = dao.observeLatestWorkout()
    val workoutHistory: Flow<List<WorkoutSummary>> = dao.observeWorkoutHistory()
    val routines: Flow<List<RoutineWithExercises>> = dao.observeRoutines()
    val recentExerciseKeys: Flow<List<String>> = dao.observeRecentExerciseKeys()

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

    suspend fun addExercises(workoutId: Long, exerciseKeys: List<String>) {
        exerciseKeys.forEach { addExercise(workoutId, it) }
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

    fun exerciseHistorySets(exerciseKey: String): Flow<List<ExerciseHistorySet>> =
        dao.observeExerciseHistorySets(exerciseKey)

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
    val instructions: List<String>,
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
        ExerciseTemplate("incline_press", "Incline dumbbell press", "Dumbbells · Bench", listOf("Upper chest"), listOf("Triceps", "Front delts"), "Set the bench to 30°. Keep wrists stacked over elbows.", listOf("Set an incline bench between 30 and 45 degrees and sit with a dumbbell on each thigh.", "Kick the dumbbells into position as you lie back, then plant both feet firmly.", "Retract your shoulder blades and hold the dumbbells beside the upper chest.", "Press upward while keeping wrists stacked over elbows.", "Lower with control until the elbows are just below the bench line."), "10", "12"),
        ExerciseTemplate("bench", "Barbell bench press", "Barbell · Bench", listOf("Chest"), listOf("Triceps", "Front delts"), "Pin the shoulder blades and press slightly back.", listOf("Lie with eyes under the bar and plant your feet.", "Retract the shoulder blades and grip slightly wider than shoulder width.", "Unrack over the shoulders and lower toward the lower chest.", "Press up and slightly back without lifting the hips."), "70", "8"),
        ExerciseTemplate("triceps_extension", "Triceps extension", "Dumbbell", listOf("Triceps"), listOf("Shoulders"), "Keep the upper arm still and extend smoothly.", listOf("Hold one dumbbell overhead with both hands.", "Keep the elbows pointing forward and lower behind the head.", "Extend the elbows fully without flaring them outward."), "10", "10"),
        ExerciseTemplate("lat_pulldown", "Lat pulldown", "Cable", listOf("Lats"), listOf("Biceps", "Upper back"), "Drive elbows toward your sides without leaning back.", listOf("Set the thigh pad firmly and take a shoulder-width grip.", "Brace the torso with a slight backward lean.", "Pull the bar toward the upper chest by driving the elbows down.", "Return slowly until the lats are fully lengthened."), "45", "10"),
        ExerciseTemplate("row", "Chest-supported row", "Dumbbells · Incline bench", listOf("Upper back", "Lats"), listOf("Rear delts", "Biceps"), "Keep the chest planted and pull elbows toward the hips.", listOf("Set a bench to a low incline and lie chest-down.", "Let the arms hang while keeping the neck neutral.", "Pull the elbows toward the hips without shrugging.", "Pause, then lower until the shoulder blades spread."), "24", "10"),
        ExerciseTemplate("biceps_curl", "Biceps curl", "Dumbbells", listOf("Biceps"), listOf("Forearms"), "Keep elbows quiet and control the lowering phase.", listOf("Stand tall with dumbbells at your sides.", "Keep elbows close to the torso and curl without swinging.", "Squeeze at the top and lower for two to three seconds."), "10", "12"),
        ExerciseTemplate("squat", "Back squat", "Barbell · Rack", listOf("Quadriceps", "Glutes"), listOf("Adductors", "Core"), "Brace before descending and keep the bar over mid-foot.", listOf("Set the bar across the upper back and brace before unracking.", "Stand around shoulder width with toes slightly out.", "Sit down between the hips while keeping the bar over mid-foot.", "Drive the floor away and keep the torso braced."), "60", "8", 120),
        ExerciseTemplate("leg_press", "Leg press", "Machine", listOf("Quadriceps", "Glutes"), listOf("Hamstrings"), "Lower with control and keep the lower back supported.", listOf("Place the feet shoulder-width on the platform.", "Release the safety and lower until the knees reach a comfortable depth.", "Press through the whole foot without locking the knees."), "90", "10", 120),
        ExerciseTemplate("leg_curl", "Seated leg curl", "Machine", listOf("Hamstrings"), listOf("Calves"), "Keep hips down and squeeze through the full range.", listOf("Align the machine pivot with the knee joint.", "Secure the thigh pad and begin with straight legs.", "Curl the pad down, pause, and return under control."), "35", "12"),
    )

    fun byKey(key: String): ExerciseTemplate? = exercises.firstOrNull { it.key == key }
}
