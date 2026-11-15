package com.veyra.app.data.workout

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM routines ORDER BY createdAtEpochMs, id")
    fun observeRoutines(): Flow<List<RoutineWithExercises>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId LIMIT 1")
    suspend fun routine(routineId: Long): RoutineWithExercises?

    @Query("SELECT COUNT(*) FROM routines")
    suspend fun routineCount(): Int

    @Insert
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Insert
    suspend fun insertRoutineExercises(exercises: List<RoutineExerciseEntity>)

    @Query("UPDATE routines SET name = :name WHERE id = :routineId")
    suspend fun updateRoutineName(routineId: Long, name: String)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: Long)

    @Transaction
    @Query("SELECT * FROM workouts WHERE finishedAtEpochMs IS NULL ORDER BY startedAtEpochMs DESC LIMIT 1")
    fun observeActiveWorkout(): Flow<WorkoutWithSets?>

    @Query(
        """
        SELECT w.id, w.title, w.startedAtEpochMs, w.finishedAtEpochMs,
               SUM(CASE WHEN s.completedAtEpochMs IS NOT NULL THEN 1 ELSE 0 END) AS completedSets,
               COUNT(s.id) AS totalSets,
               COALESCE(SUM(CASE WHEN s.completedAtEpochMs IS NOT NULL
                   THEN CAST(s.weightKg AS REAL) * CAST(s.reps AS INTEGER) ELSE 0 END), 0) AS volumeKg,
               w.healthConnectId
        FROM workouts w LEFT JOIN workout_sets s ON w.id = s.workoutId
        WHERE w.finishedAtEpochMs IS NOT NULL
        GROUP BY w.id
        ORDER BY w.finishedAtEpochMs DESC LIMIT 1
        """
    )
    fun observeLatestWorkout(): Flow<WorkoutSummary?>

    @Query(
        """
        SELECT w.id, w.title, w.startedAtEpochMs, w.finishedAtEpochMs,
               SUM(CASE WHEN s.completedAtEpochMs IS NOT NULL THEN 1 ELSE 0 END) AS completedSets,
               COUNT(s.id) AS totalSets,
               COALESCE(SUM(CASE WHEN s.completedAtEpochMs IS NOT NULL
                   THEN CAST(s.weightKg AS REAL) * CAST(s.reps AS INTEGER) ELSE 0 END), 0) AS volumeKg,
               w.healthConnectId
        FROM workouts w LEFT JOIN workout_sets s ON w.id = s.workoutId
        WHERE w.finishedAtEpochMs IS NOT NULL
        GROUP BY w.id
        ORDER BY w.finishedAtEpochMs DESC
        """
    )
    fun observeWorkoutHistory(): Flow<List<WorkoutSummary>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId LIMIT 1")
    suspend fun workoutWithSets(workoutId: Long): WorkoutWithSets?

    @Query("SELECT * FROM workouts WHERE finishedAtEpochMs IS NULL ORDER BY startedAtEpochMs DESC LIMIT 1")
    suspend fun activeWorkout(): WorkoutEntity?

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Insert
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Insert
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Query("SELECT * FROM workout_sets WHERE id = :setId LIMIT 1")
    suspend fun set(setId: Long): WorkoutSetEntity?

    @Query("SELECT MAX(setNumber) FROM workout_sets WHERE workoutId = :workoutId AND exerciseKey = :exerciseKey")
    suspend fun maxSetNumber(workoutId: Long, exerciseKey: String): Int?

    @Query("SELECT MAX(exercisePosition) FROM workout_sets WHERE workoutId = :workoutId")
    suspend fun maxExercisePosition(workoutId: Long): Int?

    @Query("UPDATE workout_sets SET weightKg = :weightKg WHERE id = :setId")
    suspend fun updateWeight(setId: Long, weightKg: String)

    @Query("UPDATE workout_sets SET reps = :reps WHERE id = :setId")
    suspend fun updateReps(setId: Long, reps: String)

    @Query("UPDATE workout_sets SET restSeconds = :seconds WHERE workoutId = :workoutId AND exerciseKey = :exerciseKey")
    suspend fun updateExerciseRest(workoutId: Long, exerciseKey: String, seconds: Int)

    @Query("UPDATE workout_sets SET exerciseNote = :note WHERE workoutId = :workoutId AND exerciseKey = :exerciseKey")
    suspend fun updateExerciseNote(workoutId: Long, exerciseKey: String, note: String)

    @Query("UPDATE workout_sets SET completedAtEpochMs = :completedAt WHERE id = :setId")
    suspend fun updateSetCompletion(setId: Long, completedAt: Long?)

    @Query("UPDATE workouts SET restUntilEpochMs = :restUntil WHERE id = :workoutId")
    suspend fun updateRestTimer(workoutId: Long, restUntil: Long?)

    @Query("UPDATE workouts SET finishedAtEpochMs = :finishedAt, restUntilEpochMs = NULL WHERE id = :workoutId")
    suspend fun finishWorkout(workoutId: Long, finishedAt: Long)

    @Query("DELETE FROM workout_sets WHERE workoutId = :workoutId")
    suspend fun deleteWorkoutSets(workoutId: Long)

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkout(workoutId: Long)

    @Query("UPDATE workouts SET healthConnectId = :recordId WHERE id = :workoutId")
    suspend fun markHealthConnectSynced(workoutId: Long, recordId: String)

    @Query(
        """
        SELECT s.weightKg || ' × ' || s.reps FROM workout_sets s
        INNER JOIN workouts w ON w.id = s.workoutId
        WHERE s.exerciseKey = :exerciseKey AND s.setNumber = :setNumber
          AND s.completedAtEpochMs IS NOT NULL AND w.finishedAtEpochMs IS NOT NULL
        ORDER BY w.finishedAtEpochMs DESC LIMIT 1
        """
    )
    suspend fun previousPerformance(exerciseKey: String, setNumber: Int): String?

    @Query(
        """
        SELECT w.finishedAtEpochMs,
               MAX(CAST(s.weightKg AS REAL)) AS bestWeightKg,
               SUM(CAST(s.weightKg AS REAL) * CAST(s.reps AS INTEGER)) AS volumeKg
        FROM workout_sets s INNER JOIN workouts w ON w.id = s.workoutId
        WHERE s.exerciseKey = :exerciseKey AND s.completedAtEpochMs IS NOT NULL
          AND w.finishedAtEpochMs IS NOT NULL
        GROUP BY w.id ORDER BY w.finishedAtEpochMs
        """
    )
    fun observeExerciseHistory(exerciseKey: String): Flow<List<ExerciseHistoryPoint>>
}
