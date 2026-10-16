package com.smiledev.rafiq_quran.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HifzGoalTest {

    @Test
    fun `fresh goal has zero progress`() {
        val goal = HifzGoal(suraNumber = 67, suraName = "Al-Mulk", ayaStart = 1, ayaEnd = 12)

        assertEquals(12, goal.totalAyahs)
        assertEquals(0, goal.memorizedCount)
        assertEquals(0, goal.progressPercent)
        assertFalse(goal.isComplete)
    }

    @Test
    fun `partial progress rounds down`() {
        val goal = HifzGoal(
            suraNumber = 67, suraName = "Al-Mulk", ayaStart = 1, ayaEnd = 12,
            memorizedAyahs = setOf(1, 2, 3)
        )

        assertEquals(3, goal.memorizedCount)
        assertEquals(25, goal.progressPercent)
        assertFalse(goal.isComplete)
    }

    @Test
    fun `full range is complete`() {
        val goal = HifzGoal(
            suraNumber = 112, suraName = "Al-Ikhlas", ayaStart = 1, ayaEnd = 4,
            memorizedAyahs = setOf(1, 2, 3, 4)
        )

        assertEquals(100, goal.progressPercent)
        assertTrue(goal.isComplete)
    }

    @Test
    fun `out-of-range memorized ayahs are ignored`() {
        val goal = HifzGoal(
            suraNumber = 112, suraName = "Al-Ikhlas", ayaStart = 1, ayaEnd = 4,
            memorizedAyahs = setOf(1, 2, 99)
        )

        assertEquals(2, goal.memorizedCount)
        assertEquals(50, goal.progressPercent)
    }

    @Test
    fun `inverted range yields zero totals`() {
        val goal = HifzGoal(suraNumber = 2, suraName = "Al-Baqarah", ayaStart = 10, ayaEnd = 5)

        assertEquals(0, goal.totalAyahs)
        assertEquals(0, goal.progressPercent)
        assertFalse(goal.isComplete)
    }
}
