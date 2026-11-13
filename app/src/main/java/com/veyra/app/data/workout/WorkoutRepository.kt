package com.veyra.app.data.workout

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val database: VeyraDatabase) {
    private val dao = database.workoutDao()

    val activeWorkout: Flow<WorkoutWithSets?> = dao.observeActiveWorkout()
    val latestWorkout: Flow<WorkoutSummary?> = dao.observeLatestWorkout()
    val workoutHistory: Flow<List<WorkoutSummary>> = dao.observeWorkoutHistory()

    suspend fun startPushWorkout(): Long = database.withTransaction {
        dao.activeWorkout()?.id ?: dao.insertWorkout(
            WorkoutEntity(title = "Push · Strength", startedAtEpochMs = System.currentTimeMillis())
        ).also { workoutId ->
            dao.insertSets(pushDaySets(workoutId))
        }
    }

    suspend fun updateWeight(setId: Long, weightKg: String) = dao.updateWeight(setId, weightKg)

    suspend fun updateReps(setId: Long, reps: String) = dao.updateReps(setId, reps)

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

    suspend fun workout(workoutId: Long): WorkoutWithSets? = dao.workoutWithSets(workoutId)

    suspend fun markHealthConnectSynced(workoutId: Long, recordId: String) {
        dao.markHealthConnectSynced(workoutId, recordId)
    }

    private fun pushDaySets(workoutId: Long): List<WorkoutSetEntity> {
        val templates = listOf(
            SetTemplate("bench", "Barbell bench press", "Barbell · Bench", 0, "70 × 10", "70", "10"),
            SetTemplate("bench", "Barbell bench press", "Barbell · Bench", 0, "75 × 8", "75", "8"),
            SetTemplate("bench", "Barbell bench press", "Barbell · Bench", 0, "75 × 7", "75", "8"),
            SetTemplate("incline_press", "Incline dumbbell press", "Dumbbells · Bench", 1, "24 × 12", "24", "12"),
            SetTemplate("incline_press", "Incline dumbbell press", "Dumbbells · Bench", 1, "26 × 10", "26", "10"),
            SetTemplate("incline_press", "Incline dumbbell press", "Dumbbells · Bench", 1, "26 × 9", "26", "10"),
        )
        return templates.groupBy { it.exerciseKey }.values.flatMap { exerciseSets ->
            exerciseSets.mapIndexed { index, template ->
                WorkoutSetEntity(
                    workoutId = workoutId,
                    exerciseKey = template.exerciseKey,
                    exerciseName = template.exerciseName,
                    equipment = template.equipment,
                    exercisePosition = template.position,
                    setNumber = index + 1,
                    previous = template.previous,
                    weightKg = template.weight,
                    reps = template.reps,
                )
            }
        }
    }

    private data class SetTemplate(
        val exerciseKey: String,
        val exerciseName: String,
        val equipment: String,
        val position: Int,
        val previous: String,
        val weight: String,
        val reps: String,
    )
}
