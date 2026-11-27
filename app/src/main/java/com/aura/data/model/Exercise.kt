package com.aura.data.model

import android.content.Context
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.IOException
import java.io.Reader

data class Exercise(
    val id: String,
    val name: String,
    /** How the exercise is logged, e.g. "Weight & Reps" or "Duration". */
    val category: String,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: String,
    val instructions: List<String> = emptyList(),
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    /** Tags and alternative names that search should match besides the name. */
    val searchTerms: List<String> = emptyList(),
    val priority: Int = 0,
)

/** The exercise library bundled with the app at assets/exercises/exercises.json. */
object ExerciseLib {
    private const val ASSET = "exercises/exercises.json"
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        // Left behind by the earlier online exercise libraries.
        File(appContext.filesDir, "exercises").deleteRecursively()
    }

    /** Most commonly used exercises first; empty when the data file (kept out of the repository) is absent. */
    val all: List<Exercise> by lazy {
        try {
            appContext.assets.open(ASSET).bufferedReader().use(::parse)
        } catch (_: IOException) {
            emptyList()
        }
    }

    private val byId by lazy { all.associateBy { it.id } }

    fun getById(id: String) = byId[id]

    internal fun parse(json: Reader): List<Exercise> =
        JsonParser.parseReader(json).asJsonArray.map { it.asJsonObject }.map { item ->
            Exercise(
                id = item.get("id").asString,
                name = item.get("name").asString,
                category = item.string("category") ?: "Exercise",
                primaryMuscles = item.strings("primaryMuscles"),
                secondaryMuscles = item.strings("secondaryMuscles"),
                equipment = item.string("equipment") ?: "Other",
                instructions = item.strings("instructions"),
                imageUrl = item.string("imageUrl"),
                videoUrl = item.string("videoUrl"),
                searchTerms = item.strings("tags") + item.strings("aliases"),
                priority = item.get("priority")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
            )
        }.sortedWith(compareByDescending<Exercise> { it.priority }.thenBy { it.name })

    private fun JsonObject.string(key: String) = get(key)?.takeIf { it.isJsonPrimitive }?.asString

    private fun JsonObject.strings(key: String): List<String> =
        get(key)?.takeIf(JsonElement::isJsonArray)?.asJsonArray?.map { it.asString }.orEmpty()
}
