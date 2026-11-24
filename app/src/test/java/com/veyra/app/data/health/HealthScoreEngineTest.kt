package com.veyra.app.data.health

import androidx.health.connect.client.records.HeartRateRecord
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthScoreEngineTest {
    private val now = Instant.parse("2026-11-13T08:00:00Z")

    @Test
    fun recoveryRemainsHiddenUntilFourteenValidDays() {
        val result = HealthScoreEngine.calculate(
            sleeps = sleeps(13),
            hrvValues = List(13) { 50.0 },
            restingHeartRates = List(13) { 55L },
            todayHeartRate = emptyList(),
            minutesSinceDayStart = 480,
        )

        assertNull(result.recovery)
        assertEquals(13, result.baselineDays)
        assertTrue(result.recoveryNote.contains("13/14"))
    }

    @Test
    fun ordinaryBaselineDayProducesNeutralRecovery() {
        val result = HealthScoreEngine.calculate(
            sleeps = sleeps(15),
            hrvValues = List(15) { 50.0 },
            restingHeartRates = List(15) { 55L },
            todayHeartRate = emptyList(),
            minutesSinceDayStart = 480,
        )

        assertNotNull(result.recovery)
        assertTrue(result.recovery!! in 58..70)
    }

    @Test
    fun completeEightHourSleepScoresHighly() {
        val result = HealthScoreEngine.calculate(
            sleeps = sleeps(1),
            hrvValues = emptyList(),
            restingHeartRates = emptyList(),
            todayHeartRate = emptyList(),
            minutesSinceDayStart = 480,
        )

        assertTrue((result.sleep ?: 0) >= 90)
    }

    @Test
    fun sustainedHighHeartRateCreatesHighButBoundedStrain() {
        val start = now.minus(Duration.ofMinutes(60))
        val samples = (0..12).map { index ->
            HeartRateRecord.Sample(start.plus(Duration.ofMinutes(index * 5L)), 155)
        }
        val result = HealthScoreEngine.calculate(
            sleeps = emptyList(),
            hrvValues = emptyList(),
            restingHeartRates = emptyList(),
            todayHeartRate = samples,
            minutesSinceDayStart = 480,
        )

        assertTrue(result.strain in 14..21)
        assertTrue(result.coverage in 0f..1f)
    }

    private fun sleeps(count: Int): List<SleepSummary> = List(count) { index ->
        val end = now.minus(Duration.ofDays(index.toLong()))
        SleepSummary(
            endTime = end,
            asleepMinutes = 480,
            sessionMinutes = 500,
            deepMinutes = 90,
            remMinutes = 105,
            bedtimeMinuteOfDay = 23 * 60,
        )
    }
}
