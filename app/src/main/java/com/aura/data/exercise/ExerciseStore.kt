package com.aura.data.exercise

import android.content.Context
import com.aura.BuildConfig
import com.aura.data.model.Exercise
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/**
 * Exercise library from the open-source ExerciseDB (1,500 exercises with GIFs, no key), plus muscle
 * diagrams from AscendAPI's Muscle Visualizer through RapidAPI (100 images a month on the free plan).
 * Everything is kept on disk so routines and history work offline and nothing is fetched twice.
 */
internal object ExerciseStore {
    private const val EXERCISES_URL = "https://oss.exercisedb.dev/api/v1/exercises?limit=25"
    private const val MUSCLE_HOST = "muscle-visualizer-api.p.rapidapi.com"
    private const val MAX_AGE_MS = 30 * 24 * 60 * 60 * 1000L
    // The exercise service throttles bursts, so pages are spaced out and throttled ones retried.
    private const val PAGE_GAP_MS = 400L
    private const val THROTTLE_RETRIES = 8
    private const val TARGET_COLOR = "#E74C3C"
    private const val SECONDARY_COLOR = "#F39C12"

    private val known = ConcurrentHashMap<String, Exercise>()
    private val catalogLock = Mutex()
    private val ready = CountDownLatch(1)
    private lateinit var dir: File

    @Volatile
    private var catalog: List<Exercise> = emptyList()

    fun init(context: Context) {
        dir = File(context.filesDir, "exercises").apply { mkdirs() }
        // The catalog is over a megabyte of JSON, so it is read off the main thread.
        thread(name = "exercise-store") {
            try {
                // Exercises saved from the earlier paid-API library stay resolvable for old workouts.
                dir.listFiles { file -> file.name.startsWith("detail-") }?.forEach { file ->
                    runCatching { parse(JsonParser.parseString(file.readText()).asJsonObject) }
                        .getOrNull()?.let { known[it.id] = it }
                }
                val saved = catalogFile.takeIf { it.exists() } ?: partialFile
                runCatching { publish(parseList(JsonParser.parseString(saved.readText()).asJsonArray)) }
            } finally {
                ready.countDown()
            }
        }
    }

    fun get(id: String): Exercise? {
        ready.await()
        return known[id]
    }

    /** Whatever part of the library is already on this device. */
    fun cached(): List<Exercise> {
        ready.await()
        return catalog
    }

    /**
     * The full exercise list. The first download takes a minute or two because the service is
     * throttled; [onProgress] receives the list as it grows, and an interrupted download resumes.
     */
    suspend fun loadCatalog(onProgress: (List<Exercise>) -> Unit = {}): List<Exercise> = catalogLock.withLock {
        withContext(Dispatchers.IO) {
            ready.await()
            if (catalogFile.isFresh()) return@withContext catalog
            val firstDownload = !catalogFile.exists()
            try {
                var cursor = cursorFile.takeIf { it.exists() && partialFile.exists() }?.readText()
                val items = if (cursor == null) JsonArray() else JsonParser.parseString(partialFile.readText()).asJsonArray
                do {
                    val page = requestPage(EXERCISES_URL + (cursor?.let { "&after=${it.encoded()}" } ?: ""))
                    page.getAsJsonArray("data").forEach(items::add)
                    val meta = page.getAsJsonObject("meta")
                    cursor = meta?.get("nextCursor")?.takeIf { meta.get("hasNextPage")?.asBoolean == true }?.asString
                    partialFile.writeText(items.toString())
                    if (cursor != null) cursorFile.writeText(cursor)
                    // A refresh keeps showing the complete old list until the new one is whole.
                    if (firstDownload) onProgress(publish(parseList(items)))
                    if (cursor != null) delay(PAGE_GAP_MS)
                } while (cursor != null)
                cursorFile.delete()
                catalogFile.delete()
                if (!partialFile.renameTo(catalogFile)) throw IOException("Couldn’t save the exercise library")
                publish(parseList(items)).also(onProgress)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                catalog.ifEmpty { throw e }
            } catch (e: RuntimeException) {
                // An unreadable page or partial file: start over next time rather than stay stuck.
                partialFile.delete()
                cursorFile.delete()
                catalog.ifEmpty { throw IOException("Unreadable exercise library response", e) }
            }
        }
    }

    /** Front and back body diagram with the worked muscles highlighted; fetched once per exercise. */
    suspend fun muscleImage(exercise: Exercise): File? = withContext(Dispatchers.IO) {
        if (!exercise.remote || BuildConfig.RAPIDAPI_KEY.isEmpty()) return@withContext null
        val file = File(dir, "muscles-${exercise.id}.jpg")
        if (file.length() > 0) return@withContext file
        val target = exercise.primaryMuscles.toMuscleParam() ?: return@withContext null
        val secondary = exercise.secondaryMuscles.toMuscleParam()
        // The free plan only serves small JPEGs on the default background.
        val style = "gender=male&background=transparent&size=small&format=jpeg"
        val path = if (secondary == null) {
            "/api/v1/visualize?muscles=$target&color=${TARGET_COLOR.encoded()}&$style"
        } else {
            "/api/v1/visualize/workout?targetMuscles=$target&targetMusclesColor=${TARGET_COLOR.encoded()}" +
                "&secondaryMuscles=$secondary&secondaryMusclesColor=${SECONDARY_COLOR.encoded()}&$style"
        }
        try {
            val connection = (URL("https://$MUSCLE_HOST$path").openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("x-rapidapi-host", MUSCLE_HOST)
                setRequestProperty("x-rapidapi-key", BuildConfig.RAPIDAPI_KEY)
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                val temp = File(dir, "${file.name}.tmp")
                connection.inputStream.use { input -> temp.outputStream().use(input::copyTo) }
                if (temp.renameTo(file)) file else null
            } finally {
                connection.disconnect()
            }
        } catch (_: IOException) {
            null
        }
    }

    private val catalogFile get() = File(dir, "oss-catalog.json")
    private val partialFile get() = File(dir, "oss-catalog.partial.json")
    private val cursorFile get() = File(dir, "oss-catalog.cursor")

    private fun File.isFresh() = exists() && System.currentTimeMillis() - lastModified() < MAX_AGE_MS

    private fun publish(exercises: List<Exercise>): List<Exercise> {
        exercises.forEach { known[it.id] = it }
        catalog = exercises
        return exercises
    }

    private suspend fun requestPage(url: String): JsonObject {
        repeat(THROTTLE_RETRIES) {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
            }
            try {
                when (val code = connection.responseCode) {
                    HttpURLConnection.HTTP_OK ->
                        return JsonParser.parseString(connection.inputStream.bufferedReader().use { it.readText() }).asJsonObject
                    429 -> delay((connection.getHeaderField("Retry-After")?.toLongOrNull() ?: 10L).coerceIn(1, 30) * 1000)
                    else -> throw IOException("Exercise library returned $code")
                }
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Exercise library is busy")
    }

    private fun parseList(items: JsonArray) =
        items.mapNotNull { runCatching { parse(it.asJsonObject) }.getOrNull() }.distinctBy { it.id }

    private fun parse(item: JsonObject): Exercise? {
        val id = item.string("exerciseId") ?: return null
        val name = item.string("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val images = item.get("imageUrls")?.takeIf { it.isJsonObject }?.asJsonObject
        val bodyParts = item.strings("bodyParts").map { it.titleCase() }
        return Exercise(
            id = id,
            name = name.titleCase(),
            category = item.string("exerciseType")?.titleCase() ?: bodyParts.firstOrNull() ?: "Exercise",
            primaryMuscles = item.strings("targetMuscles").map { it.titleCase() },
            secondaryMuscles = item.strings("secondaryMuscles").map { it.titleCase() },
            equipment = item.strings("equipments").firstOrNull()?.titleCase() ?: "Other",
            instructions = item.strings("instructions").map { it.replace(StepPrefix, "") },
            tips = item.strings("exerciseTips"),
            bodyParts = bodyParts,
            imageUrl = item.string("gifUrl") ?: images?.string("720p") ?: item.string("imageUrl"),
            overview = item.string("overview"),
            remote = true,
        )
    }

    private val StepPrefix = Regex("^Step:\\d+\\s*")

    private fun JsonObject.string(key: String) =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString

    private fun JsonObject.strings(key: String): List<String> =
        get(key)?.takeIf(JsonElement::isJsonArray)?.asJsonArray
            ?.mapNotNull { element -> element.takeIf { it.isJsonPrimitive }?.asString }.orEmpty()

    private fun String.titleCase() =
        split(' ').joinToString(" ") { word -> word.lowercase().replaceFirstChar(Char::uppercase) }

    private fun String.encoded() = URLEncoder.encode(this, "UTF-8")

    /** Comma-separated names the visualizer can draw, or null when it knows none of them. */
    private fun List<String>.toMuscleParam(): String? = map { it.uppercase() }
        .map { MuscleAliases[it] ?: it }.filter { it in DrawableMuscles }.distinct()
        .takeIf { it.isNotEmpty() }?.joinToString(",")?.encoded()

    // ExerciseDB names the visualizer has no shape for, mapped to the nearest one it does.
    private val MuscleAliases = mapOf(
        "STERNOCLEIDOMASTOID" to "NECK",
        "HANDS" to "GRIP MUSCLES",
        "FEET" to "CALVES",
        "ANKLES" to "CALVES",
        "ANKLE STABILIZERS" to "CALVES",
    )

    // The visualizer's own list (GET /api/v1/muscles); anything else is rejected with a 400.
    private val DrawableMuscles = setOf(
        "ABDOMINALS", "ABS", "ABDUCTORS", "ADDUCTORS", "ADDUCTOR LONGUS", "ADDUCTOR BREVIS", "ADDUCTOR MAGNUS",
        "BACK", "BICEPS", "BICEPS BRACHII", "BRACHIALIS", "BRACHIORADIALIS", "CALVES", "CHEST", "CORE",
        "DEEP HIP EXTERNAL ROTATORS", "DELTOIDS", "DELTS", "ANTERIOR DELTOID", "LATERAL DELTOID",
        "POSTERIOR DELTOID", "REAR DELTOIDS", "ERECTOR SPINAE", "FOREARMS", "GASTROCNEMIUS", "GLUTES",
        "GLUTEUS MAXIMUS", "GLUTEUS MEDIUS", "GLUTEUS MINIMUS", "GRIP MUSCLES", "GROIN", "HAMSTRINGS",
        "HIP FLEXORS", "ILIOPSOAS", "INFRASPINATUS", "INNER THIGHS", "LATISSIMUS DORSI", "LATS",
        "LEVATOR SCAPULAE", "LOWER BACK", "OBLIQUES", "PECTINEUS", "PECTORALS",
        "PECTORALIS MAJOR CLAVICULAR HEAD", "PECTORALIS MAJOR STERNAL HEAD", "POPLITEUS", "QUADRICEPS", "QUADS",
        "RECTUS ABDOMINIS", "RHOMBOIDS", "ROTATOR CUFF", "SARTORIUS", "SERRATUS ANTERIOR", "SERRATUS ANTE",
        "SHINS", "SHOULDERS", "SOLEUS", "SPINE", "SPLENIUS", "SUBSCAPULARIS", "TENSOR FASCIAE LATAE",
        "TERES MAJOR", "TERES MINOR", "TIBIALIS ANTERIOR", "TRANSVERSUS ABDOMINIS", "TRAPEZIUS", "TRAPS",
        "TRAPEZIUS LOWER FIBERS", "TRAPEZIUS MIDDLE FIBERS", "TRAPEZIUS UPPER FIBERS", "TRICEPS",
        "TRICEPS BRACHII", "UPPER BACK", "UPPER CHEST", "WRIST EXTENSORS", "WRIST FLEXORS", "WRISTS", "NECK", "LEGS",
    )
}
