package com.aura.data.local.dao

import androidx.room.*
import com.aura.data.local.entity.*
import kotlinx.coroutines.flow.Flow

data class RoutineWithExercises(
    @Embedded val routine: RoutineEntity,
    @Relation(parentColumn = "id", entityColumn = "routineId")
    val exercises: List<RoutineExerciseEntity>,
)

data class WorkoutWithSets(
    @Embedded val workout: WorkoutEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutId")
    val sets: List<WorkoutSetEntity>,
)

@Dao
interface WorkoutDao {
    // Routines
    @Query("SELECT * FROM routines ORDER BY updatedAt DESC")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Transaction
    @Query("SELECT * FROM routines ORDER BY updatedAt DESC, id DESC")
    fun observeRoutinesWithExercises(): Flow<List<RoutineWithExercises>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineById(id: Long): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY sortOrder")
    suspend fun getRoutineExercises(routineId: Long): List<RoutineExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercise(exercise: RoutineExerciseEntity): Long

    @Query("DELETE FROM routine_exercises WHERE id = :id")
    suspend fun deleteRoutineExercise(id: Long)

    // Workouts
    @Query("SELECT * FROM workouts WHERE isActive = 0 ORDER BY startTime DESC")
    fun getWorkoutHistory(): Flow<List<WorkoutEntity>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE isActive = 0 AND routineId = :routineId ORDER BY startTime DESC, id DESC")
    fun observeRoutineSessions(routineId: Long): Flow<List<WorkoutWithSets>>

    @Transaction
    @Query("""
        SELECT * FROM workouts
        WHERE isActive = 0 AND EXISTS (
            SELECT 1 FROM workout_sets
            WHERE workoutId = workouts.id AND exerciseId = :exerciseId AND isCompleted = 1 AND reps > 0
        )
        ORDER BY startTime DESC, id DESC LIMIT 50
    """)
    fun observeExerciseSessions(exerciseId: String): Flow<List<WorkoutWithSets>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE isActive = 1 ORDER BY id LIMIT 1")
    fun observeActiveWorkoutWithSets(): Flow<WorkoutWithSets?>

    @Query("SELECT * FROM workouts WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveWorkout(): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE isActive = 1 LIMIT 1")
    fun observeActiveWorkout(): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutById(id: Long): WorkoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Query("UPDATE routines SET updatedAt = MAX(updatedAt + 1, :now) WHERE id = :id")
    suspend fun touchRoutine(id: Long, now: Long)

    @Query("SELECT * FROM routines WHERE cloudOwnerUid = :uid AND cloudId IS NOT NULL AND (cloudSyncedRevision IS NULL OR cloudSyncedRevision != updatedAt)")
    suspend fun pendingRoutines(uid: String): List<RoutineEntity>

    @Query("UPDATE routines SET cloudSyncedRevision = :revision WHERE id = :id AND updatedAt = :revision")
    suspend fun markRoutineSynced(id: Long, revision: Long)

    @Query("SELECT * FROM workouts WHERE cloudOwnerUid = :uid AND cloudId IS NOT NULL AND isActive = 0 AND endTime IS NOT NULL AND cloudSyncedRevision IS NULL")
    suspend fun pendingWorkouts(uid: String): List<WorkoutEntity>

    @Query("UPDATE workouts SET cloudSyncedRevision = :revision WHERE id = :id AND endTime = :revision")
    suspend fun markWorkoutSynced(id: Long, revision: Long)

    // Sets
    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId ORDER BY id")
    fun getWorkoutSets(workoutId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId ORDER BY id")
    suspend fun getWorkoutSetsList(workoutId: Long): List<WorkoutSetEntity>

    @Query("SELECT workout_sets.* FROM workout_sets INNER JOIN workouts ON workouts.id = workout_sets.workoutId WHERE exerciseId = :exerciseId AND isCompleted = 1 AND workouts.isActive = 0 ORDER BY workouts.startTime DESC, workout_sets.setNumber LIMIT :limit")
    suspend fun getRecentSetsForExercise(exerciseId: String, limit: Int = 50): List<WorkoutSetEntity>

    @Query("""
        SELECT * FROM workout_sets
        WHERE exerciseId = :exerciseId AND isCompleted = 1 AND reps > 0
        AND workoutId = (
            SELECT w.id FROM workouts w
            WHERE w.isActive = 0
                AND (w.startTime < :startTime OR (w.startTime = :startTime AND w.id < :workoutId))
                AND (:routineId IS NULL OR w.routineId = :routineId)
                AND EXISTS (
                    SELECT 1 FROM workout_sets s
                    WHERE s.workoutId = w.id AND s.exerciseId = :exerciseId
                        AND s.isCompleted = 1 AND s.reps > 0
                )
            ORDER BY w.startTime DESC, w.id DESC
            LIMIT 1
        )
        ORDER BY setNumber, id
    """)
    suspend fun getPreviousExerciseSets(
        exerciseId: String, workoutId: Long, startTime: Long, routineId: Long?,
    ): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE exerciseId = :exerciseId AND isCompleted = 1 ORDER BY weight DESC LIMIT 1")
    suspend fun getPersonalBestSet(exerciseId: String): WorkoutSetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Query("""
        UPDATE workout_sets SET weight = COALESCE(:weight, weight), reps = COALESCE(:reps, reps)
        WHERE id = :id AND workoutId = :workoutId AND isCompleted = 0
    """)
    suspend fun editIncompleteSet(workoutId: Long, id: Long, weight: Double?, reps: Int?)

    @Query("""
        UPDATE workout_sets
        SET isCompleted = :completed, timestamp = CASE WHEN :completed THEN :now ELSE timestamp END
        WHERE id = :id AND workoutId = :workoutId AND isCompleted != :completed
            AND EXISTS (SELECT 1 FROM workouts WHERE workouts.id = :workoutId AND isActive = 1)
            AND (:completed = 0 OR (reps > 0 AND weight >= 0 AND weight <= :maxWeight))
    """)
    suspend fun setCompletion(workoutId: Long, id: Long, completed: Boolean, now: Long, maxWeight: Double = Double.MAX_VALUE): Int

    @Delete
    suspend fun deleteSet(set: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE workoutId = :workoutId AND exerciseId = :exerciseId")
    suspend fun deleteSetsForExercise(workoutId: Long, exerciseId: String)

    // Stats
    @Query("SELECT COUNT(*) FROM workouts WHERE isActive = 0")
    suspend fun getTotalWorkoutCount(): Int
}
