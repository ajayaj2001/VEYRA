package com.veyra.app.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodCatalogTest {
    @Test
    fun `favorite foods contain valid non-negative nutrition`() {
        assertTrue(FoodCatalog.favorites.isNotEmpty())
        FoodCatalog.favorites.forEach { food ->
            assertTrue(food.name.isNotBlank())
            assertTrue(food.amountGrams > 0)
            assertTrue(food.calories >= 0)
            assertTrue(food.carbsGrams >= 0)
            assertTrue(food.proteinGrams >= 0)
            assertTrue(food.fatGrams >= 0)
        }
    }

    @Test
    fun `composite favorite calories equal ingredient calories`() {
        val meal = FoodCatalog.favorites.first { it.ingredients.isNotEmpty() }

        assertEquals(meal.calories, meal.ingredients.sumOf { it.calories })
        assertEquals(meal.amountGrams, meal.ingredients.sumOf { it.amountGrams }, 0.001)
    }
}
