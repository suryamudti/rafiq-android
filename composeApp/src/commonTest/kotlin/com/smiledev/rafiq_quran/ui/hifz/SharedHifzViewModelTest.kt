package com.smiledev.rafiq_quran.ui.hifz

import com.smiledev.rafiq_quran.domain.repository.HifzGoal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SharedHifzViewModelTest {

    private val sampleGoals = listOf(
        HifzGoal(
            id = 1L,
            suraNumber = 67,
            suraName = "Al-Mulk",
            ayaStart = 1,
            ayaEnd = 10,
            memorizedAyahs = (1..5).toSet() // 5/10
        ),
        HifzGoal(
            id = 2L,
            suraNumber = 112,
            suraName = "Al-Ikhlas",
            ayaStart = 1,
            ayaEnd = 4,
            memorizedAyahs = (1..4).toSet() // 4/4 complete
        )
    )

    @Test
    fun testInitialMetrics() {
        val viewModel = SharedHifzViewModel(initialGoals = sampleGoals)
        val state = viewModel.uiState.value

        assertEquals(2, state.goals.size)
        assertEquals(14, state.totalAyahsTarget) // 10 + 4
        assertEquals(9, state.totalAyahsMemorized) // 5 + 4
        assertEquals(64, state.overallPercentage) // (9 * 100) / 14 = 64%
        assertEquals(1, state.completedGoalsCount)
    }

    @Test
    fun testToggleAyahProgress() {
        val viewModel = SharedHifzViewModel(initialGoals = sampleGoals)
        // Toggle ayah 6 on Al-Mulk (was not memorized)
        viewModel.toggleAyah(1L, 6)
        var goal1 = viewModel.uiState.value.goals.first { it.id == 1L }
        assertTrue(6 in goal1.memorizedAyahs)
        assertEquals(6, goal1.memorizedCount)
        assertEquals(60, goal1.progressPercent)

        // Toggle ayah 6 again (should remove it)
        viewModel.toggleAyah(1L, 6)
        goal1 = viewModel.uiState.value.goals.first { it.id == 1L }
        assertFalse(6 in goal1.memorizedAyahs)
        assertEquals(5, goal1.memorizedCount)
    }

    @Test
    fun testMarkAllAyahsAndReset() {
        val viewModel = SharedHifzViewModel(initialGoals = sampleGoals)
        // Mark all on Al-Mulk
        viewModel.markAllAyahs(1L, true)
        val goal1 = viewModel.uiState.value.goals.first { it.id == 1L }
        assertTrue(goal1.isComplete)
        assertEquals(10, goal1.memorizedCount)
        assertEquals(100, goal1.progressPercent)

        // Reset
        viewModel.markAllAyahs(1L, false)
        val goal1Reset = viewModel.uiState.value.goals.first { it.id == 1L }
        assertFalse(goal1Reset.isComplete)
        assertEquals(0, goal1Reset.memorizedCount)
    }

    @Test
    fun testFilteringGoals() {
        val viewModel = SharedHifzViewModel(initialGoals = sampleGoals)

        viewModel.setFilter(HifzFilter.ALL)
        assertEquals(2, viewModel.uiState.value.filteredGoals.size)

        viewModel.setFilter(HifzFilter.IN_PROGRESS)
        assertEquals(1, viewModel.uiState.value.filteredGoals.size)
        assertEquals(1L, viewModel.uiState.value.filteredGoals.first().id)

        viewModel.setFilter(HifzFilter.COMPLETED)
        assertEquals(1, viewModel.uiState.value.filteredGoals.size)
        assertEquals(2L, viewModel.uiState.value.filteredGoals.first().id)
    }

    @Test
    fun testAddGoalValidation() {
        val viewModel = SharedHifzViewModel(initialGoals = sampleGoals)

        // Invalid surah
        val fail1 = viewModel.addGoal(0, "Invalid", 1, 10)
        assertFalse(fail1)
        assertNotNull(viewModel.uiState.value.formError)

        // Invalid range
        val fail2 = viewModel.addGoal(1, "Al-Fatihah", 7, 2)
        assertFalse(fail2)
        assertNotNull(viewModel.uiState.value.formError)

        // Valid goal
        val ok = viewModel.addGoal(1, "Al-Fatihah", 1, 7)
        assertTrue(ok)
        assertEquals(3, viewModel.uiState.value.goals.size)
    }

    @Test
    fun testDeleteGoal() {
        val viewModel = SharedHifzViewModel(initialGoals = sampleGoals)
        viewModel.deleteGoal(2L)
        assertEquals(1, viewModel.uiState.value.goals.size)
        assertEquals(1L, viewModel.uiState.value.goals.first().id)
    }
}
