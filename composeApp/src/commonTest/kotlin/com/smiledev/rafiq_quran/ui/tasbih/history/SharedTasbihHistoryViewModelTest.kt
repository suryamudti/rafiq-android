package com.smiledev.rafiq_quran.ui.tasbih.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedTasbihHistoryViewModelTest {

    @Test
    fun initialState_computesStatsAccurately() {
        val viewModel = SharedTasbihHistoryViewModel()
        val state = viewModel.uiState.value

        assertEquals(4, state.dailyHistories.size)
        assertEquals(532, state.totalAllTimeCount)
        assertEquals(4, state.activeDaysCount)
        assertEquals(100, state.todayCount)
        assertEquals(4, state.streakDays)
    }

    @Test
    fun deleteDay_removesTargetDayAndRecomputesStats() {
        val viewModel = SharedTasbihHistoryViewModel()

        viewModel.deleteDay("2026-10-25")
        val state = viewModel.uiState.value

        assertEquals(3, state.dailyHistories.size)
        assertEquals(532 - 99, state.totalAllTimeCount)
        assertEquals(3, state.activeDaysCount)
        assertTrue(state.dailyHistories.none { it.date == "2026-10-25" })
    }

    @Test
    fun clearAll_emptiesAllHistory() {
        val viewModel = SharedTasbihHistoryViewModel()

        viewModel.clearAll()
        val state = viewModel.uiState.value

        assertTrue(state.dailyHistories.isEmpty())
        assertEquals(0, state.totalAllTimeCount)
        assertEquals(0, state.activeDaysCount)
        assertEquals(0, state.todayCount)
        assertEquals(0, state.streakDays)
    }
}
