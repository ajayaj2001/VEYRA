package com.aura.data.local.entity

import org.json.JSONArray
import org.json.JSONObject

internal data class PlannedSet(val weightKg: Double?, val reps: Int)

internal data class RoutinePlan(
    val sets: List<PlannedSet>,
    val restSeconds: Int = 0,
    val note: String = "",
) {
    fun encode(): String = JSONObject().apply {
        put("restSeconds", restSeconds)
        put("note", note)
        put("sets", JSONArray().apply {
            sets.forEach { set ->
                put(JSONObject().put("weightKg", set.weightKg ?: JSONObject.NULL).put("reps", set.reps))
            }
        })
    }.toString()

    fun cloudFields(): Map<String, Any> = mapOf(
        "restSeconds" to restSeconds,
        "note" to note,
        "sets" to sets.map { mapOf("weightKg" to it.weightKg, "reps" to it.reps) },
    )
}

internal fun RoutineExerciseEntity.plan(): RoutinePlan {
    val saved = planJson?.let { encoded ->
        runCatching {
            val plan = JSONObject(encoded)
            val sets = plan.getJSONArray("sets")
            RoutinePlan(
                sets = List(sets.length()) { index ->
                    val set = sets.getJSONObject(index)
                    PlannedSet(if (set.isNull("weightKg")) null else set.getDouble("weightKg"), set.getInt("reps"))
                }.also { require(it.isNotEmpty()) },
                restSeconds = plan.optInt("restSeconds"),
                note = plan.optString("note"),
            )
        }.getOrNull()
    }
    return saved ?: RoutinePlan(List(targetSets.coerceAtLeast(0)) { PlannedSet(null, targetReps) })
}
