package com.aura.data.local.dao

import androidx.room.*
import com.aura.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {
    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun getAllFoodItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :q || '%' OR brand LIKE '%' || :q || '%'")
    suspend fun searchFoods(q: String): List<FoodItemEntity>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getFoodById(id: Long): FoodItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(item: FoodItemEntity): Long

    @Update
    suspend fun updateFood(item: FoodItemEntity)

    @Query("SELECT * FROM food_logs WHERE date = :date ORDER BY timestamp ASC")
    fun getLogsForDate(date: String): Flow<List<FoodLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FoodLogEntity): Long

    @Delete
    suspend fun deleteLog(log: FoodLogEntity)

    @Query("SELECT * FROM nutrition_goals WHERE id = 1")
    fun getGoal(): Flow<NutritionGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: NutritionGoalEntity)
}
