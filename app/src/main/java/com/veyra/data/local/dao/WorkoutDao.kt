package com.veyra.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.veyra.data.local.entity.RoutineEntity
import com.veyra.data.local.entity.RoutineExerciseEntity
import com.veyra.data.local.entity.WorkoutEntity
import com.veyra.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // Routines
    @Query("SELECT * FROM routines ORDER BY updatedAt DESC")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineById(id: Long): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    // Routine exercises
    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY sortOrder")
    suspend fun getRoutineExercises(routineId: Long): List<RoutineExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercise(exercise: RoutineExerciseEntity): Long

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: Long)

    // Workouts
    @Query("SELECT * FROM workouts WHERE isActive = 0 ORDER BY startTime DESC")
    fun getWorkoutHistory(): Flow<List<WorkoutEntity>>

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

    // Workout sets
    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId ORDER BY exerciseId, setNumber")
    fun getWorkoutSets(workoutId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId ORDER BY exerciseId, setNumber")
    suspend fun getWorkoutSetsList(workoutId: Long): List<WorkoutSetEntity>

    @Query("""
        SELECT * FROM workout_sets
        WHERE exerciseId = :exerciseId AND isCompleted = 1
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getRecentSetsForExercise(exerciseId: String, limit: Int = 50): List<WorkoutSetEntity>

    @Query("""
        SELECT MAX(weight) FROM workout_sets
        WHERE exerciseId = :exerciseId AND isCompleted = 1
    """)
    suspend fun getPersonalBestWeight(exerciseId: String): Double?

    @Query("""
        SELECT * FROM workout_sets
        WHERE exerciseId = :exerciseId AND isCompleted = 1
        ORDER BY weight DESC
        LIMIT 1
    """)
    suspend fun getPersonalBestSet(exerciseId: String): WorkoutSetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Update
    suspend fun updateSet(set: WorkoutSetEntity)

    @Delete
    suspend fun deleteSet(set: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE workoutId = :workoutId AND exerciseId = :exerciseId")
    suspend fun deleteSetsForExercise(workoutId: Long, exerciseId: String)

    // Stats
    @Query("SELECT COUNT(*) FROM workouts WHERE isActive = 0")
    suspend fun getTotalWorkoutCount(): Int

    @Query("""
        SELECT SUM(totalVolume) FROM workouts
        WHERE isActive = 0 AND startTime >= :since
    """)
    suspend fun getTotalVolumeSince(since: Long): Double?

    @Query("""
        SELECT DISTINCT exerciseId FROM workout_sets
        WHERE workoutId IN (SELECT id FROM workouts WHERE isActive = 0)
    """)
    suspend fun getAllExercisedIds(): List<String>
}
