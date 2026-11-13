package com.veyra.app.data.workout

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
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

    @Query("UPDATE workout_sets SET weightKg = :weightKg WHERE id = :setId")
    suspend fun updateWeight(setId: Long, weightKg: String)

    @Query("UPDATE workout_sets SET reps = :reps WHERE id = :setId")
    suspend fun updateReps(setId: Long, reps: String)

    @Query("UPDATE workout_sets SET completedAtEpochMs = :completedAt WHERE id = :setId")
    suspend fun updateSetCompletion(setId: Long, completedAt: Long?)

    @Query("UPDATE workouts SET restUntilEpochMs = :restUntil WHERE id = :workoutId")
    suspend fun updateRestTimer(workoutId: Long, restUntil: Long?)

    @Query("UPDATE workouts SET finishedAtEpochMs = :finishedAt, restUntilEpochMs = NULL WHERE id = :workoutId")
    suspend fun finishWorkout(workoutId: Long, finishedAt: Long)

    @Query("UPDATE workouts SET healthConnectId = :recordId WHERE id = :workoutId")
    suspend fun markHealthConnectSynced(workoutId: Long, recordId: String)
}
