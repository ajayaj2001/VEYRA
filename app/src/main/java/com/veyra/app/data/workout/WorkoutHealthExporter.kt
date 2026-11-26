package com.veyra.app.data.workout

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneId

class WorkoutHealthExporter(private val context: Context) {
    val availability: Int
        get() = HealthConnectClient.getSdkStatus(context)

    val writePermission: String = HealthPermission.getWritePermission(ExerciseSessionRecord::class)

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun hasWritePermission(): Boolean =
        client.permissionController.getGrantedPermissions().contains(writePermission)

    suspend fun export(workout: WorkoutWithSets): String {
        val record = workout.toExerciseSessionRecord(ZoneId.systemDefault())
        return client.insertRecords(listOf(record)).recordIdsList.single()
    }
}

internal fun WorkoutWithSets.toExerciseSessionRecord(zone: ZoneId): ExerciseSessionRecord {
    val start = Instant.ofEpochMilli(workout.startedAtEpochMs)
    val recordedEnd = workout.finishedAtEpochMs
        ?.let(Instant::ofEpochMilli)
        ?: Instant.now()
    val end = recordedEnd.takeIf { it.isAfter(start) } ?: start.plusSeconds(1)
    val completedSets = sets.filter { it.completedAtEpochMs != null }
    val volume = WorkoutMetrics.volumeKg(sets)
    val exerciseNames = completedSets.distinctBy(WorkoutSetEntity::exerciseKey)
        .joinToString { it.exerciseName }
    val notes = buildString {
        append("${completedSets.size}/${sets.size} sets")
        append(" · ${"%.0f".format(volume)} kg volume")
        if (exerciseNames.isNotBlank()) append(" · $exerciseNames")
    }
    return ExerciseSessionRecord(
        startTime = start,
        startZoneOffset = zone.rules.getOffset(start),
        endTime = end,
        endZoneOffset = zone.rules.getOffset(end),
        metadata = Metadata(
            clientRecordId = "veyra-workout-${workout.id}",
            recordingMethod = Metadata.RECORDING_METHOD_MANUAL_ENTRY,
        ),
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
        title = workout.title,
        notes = notes,
    )
}
