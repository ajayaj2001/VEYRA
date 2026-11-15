package com.veyra.health

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HealthData(
    val steps: Long = 0,
    val calories: Double = 0.0,
    val activeCalories: Double = 0.0,
    val restingHeartRate: Long = 0,
    val heartRateAvg: Long = 0,
    val heartRateMin: Long = 0,
    val heartRateMax: Long = 0,
    val hrvRmssd: Double = 0.0,
    val sleepDurationMinutes: Long = 0,
    val sleepStages: Map<Int, Long> = emptyMap(),
)

class HealthConnectManager(private val context: Context) {

    private val client: HealthConnectClient? by lazy {
        try {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
    )

    fun isAvailable(): Boolean = client != null

    suspend fun hasAllPermissions(): Boolean {
        val c = client ?: return false
        val granted = c.permissionController.getGrantedPermissions()
        return permissions.all { it in granted }
    }

    suspend fun readTodayData(): HealthData {
        val c = client ?: return HealthData()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(zone).toInstant()
        val now = Instant.now()
        val timeRange = TimeRangeFilter.between(startOfDay, now)

        var data = HealthData()

        try {
            // Steps
            val stepsResponse = c.readRecords(
                ReadRecordsRequest(StepsRecord::class, timeRange)
            )
            data = data.copy(steps = stepsResponse.records.sumOf { it.count })

            // Total calories
            val calResponse = c.readRecords(
                ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeRange)
            )
            data = data.copy(
                calories = calResponse.records.sumOf { it.energy.inKilocalories }
            )

            // Active calories
            val activeCalResponse = c.readRecords(
                ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, timeRange)
            )
            data = data.copy(
                activeCalories = activeCalResponse.records.sumOf { it.energy.inKilocalories }
            )

            // Heart rate
            val hrResponse = c.readRecords(
                ReadRecordsRequest(HeartRateRecord::class, timeRange)
            )
            val allSamples = hrResponse.records.flatMap { it.samples }
            if (allSamples.isNotEmpty()) {
                data = data.copy(
                    heartRateAvg = allSamples.map { it.beatsPerMinute }.average().toLong(),
                    heartRateMin = allSamples.minOf { it.beatsPerMinute },
                    heartRateMax = allSamples.maxOf { it.beatsPerMinute },
                )
            }

            // Resting heart rate
            val rhrResponse = c.readRecords(
                ReadRecordsRequest(RestingHeartRateRecord::class, timeRange)
            )
            if (rhrResponse.records.isNotEmpty()) {
                data = data.copy(
                    restingHeartRate = rhrResponse.records.last().beatsPerMinute
                )
            }

            // HRV
            val hrvResponse = c.readRecords(
                ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, timeRange)
            )
            if (hrvResponse.records.isNotEmpty()) {
                data = data.copy(
                    hrvRmssd = hrvResponse.records.map { it.heartRateVariabilityMillis }.average()
                )
            }

            // Sleep (check last night)
            val sleepStart = today.minusDays(1).atTime(18, 0).atZone(zone).toInstant()
            val sleepTimeRange = TimeRangeFilter.between(sleepStart, now)
            val sleepResponse = c.readRecords(
                ReadRecordsRequest(SleepSessionRecord::class, sleepTimeRange)
            )
            if (sleepResponse.records.isNotEmpty()) {
                val totalMinutes = sleepResponse.records.sumOf { session ->
                    java.time.Duration.between(session.startTime, session.endTime).toMinutes()
                }
                val stages = mutableMapOf<Int, Long>()
                sleepResponse.records.forEach { session ->
                    session.stages.forEach { stage ->
                        val duration = java.time.Duration.between(stage.startTime, stage.endTime).toMinutes()
                        stages[stage.stage] = (stages[stage.stage] ?: 0L) + duration
                    }
                }
                data = data.copy(
                    sleepDurationMinutes = totalMinutes,
                    sleepStages = stages,
                )
            }
        } catch (_: Exception) {
            // Graceful degradation if any read fails
        }

        return data
    }

    suspend fun writeExerciseSession(
        title: String,
        startTime: Instant,
        endTime: Instant,
    ) {
        val c = client ?: return
        try {
            c.insertRecords(
                listOf(
                    ExerciseSessionRecord(
                        startTime = startTime,
                        startZoneOffset = ZoneId.systemDefault().rules.getOffset(startTime),
                        endTime = endTime,
                        endZoneOffset = ZoneId.systemDefault().rules.getOffset(endTime),
                        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                        title = title,
                    )
                )
            )
        } catch (_: Exception) {
            // Fail silently
        }
    }
}
