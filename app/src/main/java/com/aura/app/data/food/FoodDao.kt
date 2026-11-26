package com.aura.app.data.food

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Transaction
    @Query("SELECT * FROM food_entries WHERE loggedAtEpochMs >= :start AND loggedAtEpochMs < :end ORDER BY loggedAtEpochMs DESC")
    fun observeEntries(start: Long, end: Long): Flow<List<FoodWithIngredients>>

    @Insert
    suspend fun insertEntry(entry: FoodEntryEntity): Long

    @Insert
    suspend fun insertIngredients(ingredients: List<FoodIngredientEntity>)

    @Query("DELETE FROM food_ingredients WHERE foodEntryId = :entryId")
    suspend fun deleteIngredients(entryId: Long)

    @Query("DELETE FROM food_entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)
}
