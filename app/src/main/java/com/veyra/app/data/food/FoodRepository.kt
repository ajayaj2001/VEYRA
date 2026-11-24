package com.veyra.app.data.food

import androidx.room.withTransaction
import com.veyra.app.data.workout.VeyraDatabase
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

class FoodRepository(private val database: VeyraDatabase) {
    private val dao = database.foodDao()

    fun entriesFor(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Flow<List<FoodWithIngredients>> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.observeEntries(start, end)
    }

    suspend fun log(draft: FoodDraft, loggedAtEpochMs: Long = System.currentTimeMillis()) =
        database.withTransaction {
            val entryId = dao.insertEntry(
                FoodEntryEntity(
                    name = draft.name,
                    loggedAtEpochMs = loggedAtEpochMs,
                    amountGrams = draft.amountGrams,
                    calories = draft.calories,
                    carbsGrams = draft.carbsGrams,
                    proteinGrams = draft.proteinGrams,
                    fatGrams = draft.fatGrams,
                    healthScore = draft.healthScore,
                )
            )
            dao.insertIngredients(draft.ingredients.mapIndexed { index, ingredient ->
                FoodIngredientEntity(
                    foodEntryId = entryId,
                    name = ingredient.name,
                    amountGrams = ingredient.amountGrams,
                    calories = ingredient.calories,
                    carbsGrams = ingredient.carbsGrams,
                    proteinGrams = ingredient.proteinGrams,
                    fatGrams = ingredient.fatGrams,
                    position = index,
                )
            })
        }

    suspend fun delete(entryId: Long) = database.withTransaction {
        dao.deleteIngredients(entryId)
        dao.deleteEntry(entryId)
    }
}
