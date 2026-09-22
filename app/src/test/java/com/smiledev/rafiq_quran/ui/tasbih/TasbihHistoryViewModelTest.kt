package com.smiledev.rafiq_quran.ui.tasbih

import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.GregorianDate
import com.smiledev.rafiq_quran.domain.model.TasbihHistoryItem
import com.smiledev.rafiq_quran.domain.repository.TasbihHistoryRepository
import com.smiledev.rafiq_quran.domain.util.TodayProvider
import com.smiledev.rafiq_quran.ui.tasbih.history.TasbihHistoryViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasbihHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)

    private val repository: TasbihHistoryRepository = mockk()
    private val todayProvider: TodayProvider = mockk()

    private val sampleRecords = listOf(
        TasbihHistoryItem(1, "2026-09-22", 1, "Subhanallah", "سُبْحَانَ اللّهُ", 33),
        TasbihHistoryItem(2, "2026-09-22", 2, "Alhamdulillah", "ٱلْحَمْدُ لِلَّهِ", 33),
        TasbihHistoryItem(3, "2026-09-21", 1, "Subhanallah", "سُبْحَانَ اللّهُ", 100)
    )

    @Before
    fun setup() {
        every { todayProvider.today() } returns GregorianDate(2026, 9, 22)
        every { repository.observeAllRecords() } returns flowOf(sampleRecords)
        coEvery { repository.deleteDay(any()) } returns Result.Success(Unit)
        coEvery { repository.clearAll() } returns Result.Success(Unit)
    }

    private fun createViewModel(): TasbihHistoryViewModel {
        return TasbihHistoryViewModel(
            tasbihHistoryRepository = repository,
            todayProvider = todayProvider,
            dispatcherProvider = testDispatcherProvider
        )
    }

    @Test
    fun `initial state aggregates history by day and computes stats`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.dailyHistories.size)

        // Day 1: 2026-09-22
        val day1 = state.dailyHistories[0]
        assertEquals("2026-09-22", day1.date)
        assertEquals(66, day1.totalCount)
        assertEquals(2, day1.items.size)

        // Day 2: 2026-09-21
        val day2 = state.dailyHistories[1]
        assertEquals("2026-09-21", day2.date)
        assertEquals(100, day2.totalCount)
        assertEquals(1, day2.items.size)

        // Overall stats
        assertEquals(166, state.totalAllTimeCount)
        assertEquals(2, state.activeDaysCount)
        assertEquals(66, state.todayCount)
    }

    @Test
    fun `confirmDeleteDay invokes repository deleteDay and resets selection`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.setDayToDelete("2026-09-21")
        assertEquals("2026-09-21", vm.uiState.value.dayToDelete)

        vm.confirmDeleteDay()
        advanceUntilIdle()

        coVerify { repository.deleteDay("2026-09-21") }
        assertNull(vm.uiState.value.dayToDelete)
    }

    @Test
    fun `confirmClearAll invokes repository clearAll and dismisses dialog`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.setShowClearAllDialog(true)
        assertTrue(vm.uiState.value.showClearAllDialog)

        vm.confirmClearAll()
        advanceUntilIdle()

        coVerify { repository.clearAll() }
        assertFalse(vm.uiState.value.showClearAllDialog)
    }
}
