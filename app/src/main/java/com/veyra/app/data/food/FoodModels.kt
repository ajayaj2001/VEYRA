package com.veyra.app.data.food

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "food_entries", indices = [Index("loggedAtEpochMs")])
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val loggedAtEpochMs: Long,
    val amountGrams: Double,
    val calories: Int,
    val carbsGrams: Double,
    val proteinGrams: Double,
    val fatGrams: Double,
    val healthScore: Int? = null,
)

@Entity(tableName = "food_ingredients", indices = [Index("foodEntryId")])
data class FoodIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodEntryId: Long,
    val name: String,
    val amountGrams: Double,
    val calories: Int,
    val carbsGrams: Double,
    val proteinGrams: Double,
    val fatGrams: Double,
    val position: Int,
)

data class FoodWithIngredients(
    @Embedded val food: FoodEntryEntity,
    @Relation(parentColumn = "id", entityColumn = "foodEntryId")
    val ingredients: List<FoodIngredientEntity>,
)

data class FoodIngredientDraft(
    val name: String,
    val amountGrams: Double,
    val calories: Int,
    val carbsGrams: Double,
    val proteinGrams: Double,
    val fatGrams: Double,
)

data class FoodDraft(
    val name: String,
    val amountGrams: Double,
    val calories: Int,
    val carbsGrams: Double,
    val proteinGrams: Double,
    val fatGrams: Double,
    val healthScore: Int? = null,
    val ingredients: List<FoodIngredientDraft> = emptyList(),
)

data class DailyNutrition(
    val calories: Int,
    val carbsGrams: Double,
    val proteinGrams: Double,
    val fatGrams: Double,
)

object FoodCatalog {
    val favorites = listOf(
        FoodDraft("Apple juice", 250.0, 108, 26.0, 0.3, 0.2, 5),
        FoodDraft(
            "Wheat bread with peanut butter and honey", 120.0, 408, 50.2, 14.7, 18.2, 6,
            ingredients = listOf(
                FoodIngredientDraft("Wheat bread", 80.0, 200, 36.0, 7.2, 3.2),
                FoodIngredientDraft("Peanut butter", 30.0, 178, 6.0, 7.5, 15.0),
                FoodIngredientDraft("Honey", 10.0, 30, 8.2, 0.0, 0.0),
            ),
        ),
        FoodDraft("Curd", 150.0, 147, 7.0, 8.0, 9.0, 8),
        FoodDraft("Coffee with hot milk", 180.0, 58, 5.5, 3.2, 2.6, 6),
        FoodDraft("Protein shake", 450.0, 754, 88.0, 55.0, 20.0, 7),
    )
}
