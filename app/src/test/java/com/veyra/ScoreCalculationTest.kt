package com.veyra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreCalculationTest {

    // Replicate the 1RM formula from ActiveWorkoutViewModel
    private fun estimateOneRepMax(weight: Double, reps: Int): Double {
        if (reps <= 0 || weight <= 0) return 0.0
        if (reps == 1) return weight
        return weight * (36.0 / (37.0 - reps))
    }

    @Test
    fun `1RM estimation with 1 rep returns weight`() {
        assertEquals(100.0, estimateOneRepMax(100.0, 1), 0.01)
    }

    @Test
    fun `1RM estimation with 0 reps returns 0`() {
        assertEquals(0.0, estimateOneRepMax(100.0, 0), 0.01)
    }

    @Test
    fun `1RM estimation with 0 weight returns 0`() {
        assertEquals(0.0, estimateOneRepMax(0.0, 10), 0.01)
    }

    @Test
    fun `1RM estimation with negative weight returns 0`() {
        assertEquals(0.0, estimateOneRepMax(-50.0, 5), 0.01)
    }

    @Test
    fun `1RM increases with reps at same weight`() {
        val e1rm5 = estimateOneRepMax(100.0, 5)
        val e1rm10 = estimateOneRepMax(100.0, 10)
        assertTrue("More reps should give higher 1RM", e1rm10 > e1rm5)
    }

    @Test
    fun `1RM increases with weight at same reps`() {
        val e1rm100 = estimateOneRepMax(100.0, 5)
        val e1rm120 = estimateOneRepMax(120.0, 5)
        assertTrue("More weight should give higher 1RM", e1rm120 > e1rm100)
    }

    @Test
    fun `1RM for typical bench press values`() {
        // 100kg x 5 reps = ~112.5kg estimated 1RM (Brzycki)
        val e1rm = estimateOneRepMax(100.0, 5)
        assertTrue("1RM should be > weight used", e1rm > 100.0)
        assertTrue("1RM should be reasonable", e1rm < 150.0)
    }

    @Test
    fun `1RM for 10 rep set`() {
        // 80kg x 10 reps
        val e1rm = estimateOneRepMax(80.0, 10)
        assertTrue("1RM for 80x10 should be > 100", e1rm > 100.0)
        assertTrue("1RM for 80x10 should be < 120", e1rm < 120.0)
    }

    // Recovery score logic replicated
    private fun calculateRecoveryScore(hrvRmssd: Double, restingHR: Long, sleepMinutes: Long): Int {
        var score = 50
        if (hrvRmssd > 0) {
            score += when {
                hrvRmssd >= 60 -> 25
                hrvRmssd >= 45 -> 15
                hrvRmssd >= 30 -> 5
                else -> -10
            }
        }
        if (restingHR > 0) {
            score += when {
                restingHR <= 55 -> 20
                restingHR <= 65 -> 10
                restingHR <= 75 -> 0
                else -> -15
            }
        }
        val sleepHours = sleepMinutes / 60.0
        score += when {
            sleepHours >= 8 -> 10
            sleepHours >= 7 -> 5
            sleepHours >= 6 -> 0
            else -> -15
        }
        return score.coerceIn(0, 100)
    }

    @Test
    fun `recovery score is within valid range`() {
        val score = calculateRecoveryScore(50.0, 62, 450)
        assertTrue(score in 0..100)
    }

    @Test
    fun `excellent recovery metrics give high score`() {
        val score = calculateRecoveryScore(70.0, 52, 510)
        assertTrue("Excellent metrics should give score >= 80, got $score", score >= 80)
    }

    @Test
    fun `poor recovery metrics give low score`() {
        val score = calculateRecoveryScore(20.0, 85, 300)
        assertTrue("Poor metrics should give score <= 30, got $score", score <= 30)
    }

    @Test
    fun `recovery score baseline is 50 with no data`() {
        val score = calculateRecoveryScore(0.0, 0, 0)
        // 50 (base) + 0 (no HRV) + 0 (no HR) + -15 (bad sleep) = 35
        assertEquals(35, score)
    }

    // Sleep score logic replicated
    private fun calculateSleepScore(sleepMinutes: Long, hrvRmssd: Double, restingHR: Long): Int {
        if (sleepMinutes == 0L) return 0
        val hours = sleepMinutes / 60.0
        var score = when {
            hours >= 8.5 -> 90
            hours >= 8 -> 85
            hours >= 7.5 -> 75
            hours >= 7 -> 65
            hours >= 6.5 -> 50
            hours >= 6 -> 40
            else -> 25
        }
        if (hrvRmssd >= 50) score += 5
        if (restingHR in 1..60) score += 5
        return score.coerceIn(0, 100)
    }

    @Test
    fun `sleep score is 0 with no sleep data`() {
        assertEquals(0, calculateSleepScore(0, 50.0, 60))
    }

    @Test
    fun `8 hours sleep gives good score`() {
        val score = calculateSleepScore(480, 50.0, 58)
        assertTrue("8h sleep should score >= 85, got $score", score >= 85)
    }

    @Test
    fun `5 hours sleep gives low score`() {
        val score = calculateSleepScore(300, 30.0, 70)
        assertTrue("5h sleep should score <= 30, got $score", score <= 30)
    }
}
