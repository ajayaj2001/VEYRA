package com.aura.data.food

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class FoodAiTest {
    private fun response(mealJson: String) = JsonObject().apply {
        add("candidates", JsonArray().apply {
            add(JsonObject().apply {
                add("content", JsonObject().apply {
                    add("parts", JsonArray().apply { add(JsonObject().apply { addProperty("text", mealJson) }) })
                })
            })
        })
    }.toString()

    @Test
    fun readsEveryItemOfAMeal() {
        val estimate = FoodAi.parseEstimate(response("""
            {"isFood": true, "name": "Dal with roti", "items": [
              {"name": "Roti", "grams": 80, "calories": 240, "proteinGrams": 7.5, "carbsGrams": 46, "fatGrams": 3},
              {"name": "Dal", "grams": 200, "calories": 230, "proteinGrams": 14, "carbsGrams": 34, "fatGrams": 4.5}
            ]}
        """))

        assertEquals("Dal with roti", estimate.name)
        assertEquals(listOf("Roti", "Dal"), estimate.items.map { it.name })
        assertEquals(80.0, estimate.items[0].grams)
        assertEquals(7.5, estimate.items[0].protein, 0.0)
        assertEquals(470.0, estimate.items.sumOf { it.calories }, 0.0)
    }

    @Test
    fun missingOrNegativeNumbersBecomeZero() {
        val item = FoodAi.parseEstimate(response("""
            {"isFood": true, "name": "", "items": [{"name": "Black coffee", "grams": 0, "calories": -4}]}
        """))

        assertEquals("Black coffee", item.name)
        assertNull(item.items.single().grams)
        assertEquals(0.0, item.items.single().calories, 0.0)
        assertEquals(0.0, item.items.single().fat, 0.0)
    }

    @Test
    fun skipsThoughtPartsInGeminiResponses() {
        val answer = com.google.gson.JsonParser.parseString(response(
            """{"isFood": true, "name": "Rice", "items": [{"name": "Rice", "calories": 130}]}""",
        )).asJsonObject
        val parts = answer.getAsJsonArray("candidates")[0].asJsonObject
            .getAsJsonObject("content").getAsJsonArray("parts")
        val output = parts.remove(0)
        parts.add(JsonObject().apply {
            addProperty("thought", true)
            addProperty("text", "Estimate portion size.")
        })
        parts.add(output)

        assertEquals(130.0, FoodAi.parseEstimate(answer.toString()).items.single().calories, 0.0)
    }

    @Test
    fun rejectsAnswersWithoutFood() {
        val notFood = assertThrows(FoodAiException::class.java) {
            FoodAi.parseEstimate(response("""{"isFood": false, "name": "", "items": []}"""))
        }
        assertEquals("No food was recognised. Try another photo or describe the meal.", notFood.message)
    }

    @Test
    fun rejectsUnreadableAnswers() {
        listOf("""{"candidates": []}""", response("not json"), "").forEach { body ->
            val error = assertThrows(FoodAiException::class.java) { FoodAi.parseEstimate(body) }
            assertEquals("Gemini’s answer couldn’t be read. Try again.", error.message)
        }
    }
}
