package com.aura.data.food

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal data class FoodEstimate(val name: String, val items: List<Item>) {
    data class Item(
        val name: String,
        val grams: Double?,
        val calories: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double,
    )
}

/** A failure whose message is written for the person logging the meal. */
internal class FoodAiException(message: String) : IOException(message)

/** Estimates a meal's nutrition from a photo and/or description with the user's own Gemini API key. */
internal object FoodAi {
    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent"
    private const val KEY = "gemini_api_key"
    private const val MAX_PHOTO_EDGE = 1536
    private const val PROMPT = "You are a nutrition assistant. Identify the meal from the photo and/or the " +
        "description and list each distinct food in it with its estimated portion in grams, calories in kcal, " +
        "and protein, carbohydrate and fat in grams. Use typical recipes and portion sizes when unsure. When a " +
        "description is given, it takes priority over what the photo suggests. Give the meal a short name. If " +
        "neither the photo nor the description shows food or drink, set isFood to false and return no items."

    fun apiKey(context: Context): String? =
        runCatching { preferences(context).getString(KEY, null) }.getOrNull()?.takeIf { it.isNotBlank() }

    fun setApiKey(context: Context, key: String?) {
        preferences(context).edit().apply {
            if (key.isNullOrBlank()) remove(KEY) else putString(KEY, key.trim())
        }.apply()
    }

    suspend fun estimate(context: Context, photo: String?, description: String): FoodEstimate =
        withContext(Dispatchers.IO) {
            val key = apiKey(context)
                ?: throw FoodAiException("Add your Gemini API key in Profile → AI food estimation first.")
            val parts = JsonArray()
            if (photo != null) parts.add(JsonObject().apply {
                add("inline_data", JsonObject().apply {
                    addProperty("mime_type", "image/jpeg")
                    addProperty("data", encodePhoto(context, photo))
                })
            })
            parts.add(JsonObject().apply {
                addProperty("text", PROMPT + description.trim().let { if (it.isEmpty()) "" else "\n\nDescription: $it" })
            })
            val body = JsonObject().apply {
                add("contents", JsonArray().apply { add(JsonObject().apply { add("parts", parts) }) })
                add("generationConfig", JsonObject().apply {
                    addProperty("responseMimeType", "application/json")
                    add("responseSchema", JsonParser.parseString(RESPONSE_SCHEMA))
                })
            }

            val connection = try {
                (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 60_000
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-goog-api-key", key)
                    outputStream.use { it.write(body.toString().toByteArray()) }
                }
            } catch (_: IOException) {
                throw FoodAiException("Couldn’t reach Gemini. Check your connection and try again.")
            }
            try {
                val code = try {
                    connection.responseCode
                } catch (_: IOException) {
                    throw FoodAiException("Couldn’t reach Gemini. Check your connection and try again.")
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    throw FoodAiException(when {
                        code == 429 -> "Your Gemini key has reached its usage limit. Try again later."
                        code == 401 || code == 403 || "API_KEY_INVALID" in error || "API key not valid" in error ->
                            "Gemini rejected the API key. Check it in Profile → AI food estimation."
                        else -> "Gemini couldn’t estimate this meal (error $code). Try again."
                    })
                }
                parseEstimate(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }

    /** Reads a Gemini generateContent response whose text part is JSON matching [RESPONSE_SCHEMA]. */
    fun parseEstimate(response: String): FoodEstimate {
        val unreadable = FoodAiException("Gemini’s answer couldn’t be read. Try again.")
        val meal = try {
            val text = JsonParser.parseString(response).asJsonObject
                .getAsJsonArray("candidates")[0].asJsonObject
                .getAsJsonObject("content").getAsJsonArray("parts")
                .map { it.asJsonObject }
                .filterNot { it.get("thought")?.asBoolean == true }
                .mapNotNull { it.get("text")?.takeUnless { part -> part.isJsonNull }?.asString }
                .joinToString("")
            JsonParser.parseString(text).asJsonObject
        } catch (_: RuntimeException) {
            throw unreadable
        }
        val items = try {
            meal.getAsJsonArray("items")?.map { it.asJsonObject }.orEmpty().mapNotNull { item ->
                val name = item.get("name")?.asString?.trim().orEmpty()
                if (name.isEmpty()) return@mapNotNull null
                FoodEstimate.Item(
                    name = name,
                    grams = item.amount("grams").takeIf { it > 0 },
                    calories = item.amount("calories"),
                    protein = item.amount("proteinGrams"),
                    carbs = item.amount("carbsGrams"),
                    fat = item.amount("fatGrams"),
                )
            }
        } catch (_: RuntimeException) {
            throw unreadable
        }
        if (meal.get("isFood")?.asBoolean == false || items.isEmpty()) {
            throw FoodAiException("No food was recognised. Try another photo or describe the meal.")
        }
        val name = runCatching { meal.get("name")?.asString?.trim() }.getOrNull().orEmpty()
        return FoodEstimate(name.ifEmpty { items.first().name }, items)
    }

    // Missing or negative numbers become zero rather than failing the whole estimate.
    private fun JsonObject.amount(key: String): Double =
        get(key)?.takeIf { it.isJsonPrimitive }?.asDouble?.takeIf { it.isFinite() && it > 0 } ?: 0.0

    private fun preferences(context: Context): SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "ai_settings",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private fun encodePhoto(context: Context, photo: String): String {
        val unreadable = FoodAiException("Couldn’t read the photo. Choose it again.")
        val uri = Uri.parse(photo)
        val bitmap = try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_PHOTO_EDGE) sample *= 2
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        } ?: throw unreadable
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        return Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
    }

    private const val RESPONSE_SCHEMA = """
        {
          "type": "OBJECT",
          "properties": {
            "isFood": {"type": "BOOLEAN"},
            "name": {"type": "STRING"},
            "items": {
              "type": "ARRAY",
              "items": {
                "type": "OBJECT",
                "properties": {
                  "name": {"type": "STRING"},
                  "grams": {"type": "NUMBER"},
                  "calories": {"type": "NUMBER"},
                  "proteinGrams": {"type": "NUMBER"},
                  "carbsGrams": {"type": "NUMBER"},
                  "fatGrams": {"type": "NUMBER"}
                },
                "required": ["name", "grams", "calories", "proteinGrams", "carbsGrams", "fatGrams"]
              }
            }
          },
          "required": ["isFood", "name", "items"]
        }
    """
}
