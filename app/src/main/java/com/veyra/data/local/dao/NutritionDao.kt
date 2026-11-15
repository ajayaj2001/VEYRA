package com.veyra.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.veyra.data.local.entity.FoodItemEntity
import com.veyra.data.local.entity.FoodLogEntity
import com.veyra.data.local.entity.MealEntity
import com.veyra.data.local.entity.MealItemEntity
import com.veyra.data.local.entity.NutritionGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {
    // Food items
    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun getAllFoodItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteFoods(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%'")
    suspend fun searchFoodItems(query: String): List<FoodItemEntity>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getFoodItemById(id: Long): FoodItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(item: FoodItemEntity): Long

    @Update
    suspend fun updateFoodItem(item: FoodItemEntity)

    @Delete
    suspend fun deleteFoodItem(item: FoodItemEntity)

    // Food logs
    @Query("SELECT * FROM food_logs WHERE date = :date ORDER BY timestamp ASC")
    fun getFoodLogsForDate(date: String): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getFoodLogsForDateList(date: String): List<FoodLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(log: FoodLogEntity): Long

    @Delete
    suspend fun deleteFoodLog(log: FoodLogEntity)

    // Meals
    @Query("SELECT * FROM meals ORDER BY createdAt DESC")
    fun getAllMeals(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMealById(id: Long): MealEntity?

    @Query("SELECT * FROM meal_items WHERE mealId = :mealId")
    suspend fun getMealItems(mealId: Long): List<MealItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealItem(item: MealItemEntity): Long

    @Delete
    suspend fun deleteMeal(meal: MealEntity)

    // Goals
    @Query("SELECT * FROM nutrition_goals WHERE id = 1")
    fun getNutritionGoal(): Flow<NutritionGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutritionGoal(goal: NutritionGoalEntity)
}
