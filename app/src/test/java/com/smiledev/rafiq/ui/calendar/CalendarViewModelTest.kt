package com.smiledev.rafiq.ui.calendar

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.GregorianDate
import com.smiledev.rafiq.domain.model.HijriDate
import com.smiledev.rafiq.domain.model.IslamicEvent
import com.smiledev.rafiq.domain.repository.IslamicCalendarRepository
import com.smiledev.rafiq.domain.util.TodayProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val repository: IslamicCalendarRepository = mockk()
    private val todayProvider = TodayProvider { GregorianDate(2025, 7, 5) }

    private val events = listOf(
        IslamicEvent(1, 10, "Day of Ashura", "Hari Asyura", "D", "D", "observance"),
        IslamicEvent(10, 1, "Eid al-Fitr", "Idul Fitri", "D", "D", "holiday")
    )

    private val monthNames = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhul-Qi'dah", "Dhul-Hijjah"
    )

    private fun stubRepository(todayEvents: List<IslamicEvent> = emptyList()) {
        every { repository.getEvents() } returns Result.Success(events)
        every { repository.getTodayEvents() } returns Result.Success(todayEvents)
        every { repository.islamicMonthNames } returns monthNames
        every { repository.islamicMonthNamesId } returns monthNames
    }

    @Test
    fun `grid for July 2025 has 42 cells and places Ashura on July 5`() = runTest(testDispatcher) {
        stubRepository()
        val vm = CalendarViewModel(repository, todayProvider, testDispatcherProvider)
        advanceUntilIdle()

        val grid = vm.uiState.value.grid
        assertEquals(42, grid.size)
        assertNull(grid[0])
        assertNull(grid[1])
        // July 1 2025 is Tuesday -> index 2 -> 1 Muharram 1447 day 6
        assertEquals(1, grid[2]?.gregorianDay)
        assertEquals(HijriDate(1447, 1, 6), grid[2]?.hijriDate)
        // July 5 2025 = 10 Muharram 1447 (Ashura) -> index 6
        assertEquals(5, grid[6]?.gregorianDay)
        assertEquals(HijriDate(1447, 1, 10), grid[6]?.hijriDate)
        assertEquals(1, grid[6]?.events?.size)
        assertEquals("Day of Ashura", grid[6]?.events?.single()?.titleEn)
        assertTrue(grid[6]?.isToday == true)
    }

    @Test
    fun `todayEvents loaded into state`() = runTest(testDispatcher) {
        stubRepository(todayEvents = events)
        val vm = CalendarViewModel(repository, todayProvider, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.todayEvents.size)
        assertEquals(2025, vm.uiState.value.displayedYear)
        assertEquals(7, vm.uiState.value.displayedMonth)
    }

    @Test
    fun `nextMonth and previousMonth navigate with year rollover`() = runTest(testDispatcher) {
        stubRepository()
        val vm = CalendarViewModel(repository, todayProvider, testDispatcherProvider)
        advanceUntilIdle()

        vm.nextMonth()
        advanceUntilIdle()
        assertEquals(8, vm.uiState.value.displayedMonth)
        assertEquals(2025, vm.uiState.value.displayedYear)

        vm.previousMonth()
        advanceUntilIdle()
        assertEquals(7, vm.uiState.value.displayedMonth)

        // roll to Dec 2025 then Jan 2026
        repeat(5) { vm.nextMonth(); advanceUntilIdle() }
        assertEquals(12, vm.uiState.value.displayedMonth)
        vm.nextMonth()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.displayedMonth)
        assertEquals(2026, vm.uiState.value.displayedYear)
    }

    @Test
    fun `goToToday returns to today month`() = runTest(testDispatcher) {
        stubRepository()
        val vm = CalendarViewModel(repository, todayProvider, testDispatcherProvider)
        advanceUntilIdle()

        vm.nextMonth()
        advanceUntilIdle()
        assertEquals(8, vm.uiState.value.displayedMonth)

        vm.goToToday()
        advanceUntilIdle()
        assertEquals(2025, vm.uiState.value.displayedYear)
        assertEquals(7, vm.uiState.value.displayedMonth)
    }

    @Test
    fun `onDayClick selects and dismissDaySheet clears`() = runTest(testDispatcher) {
        stubRepository()
        val vm = CalendarViewModel(repository, todayProvider, testDispatcherProvider)
        advanceUntilIdle()

        vm.onDayClick(6)
        assertEquals(6, vm.uiState.value.selectedIndex)

        vm.dismissDaySheet()
        assertNull(vm.uiState.value.selectedIndex)
    }

    @Test
    fun `events appear on the correct day across months`() = runTest(testDispatcher) {
        stubRepository()
        val vm = CalendarViewModel(repository, todayProvider, testDispatcherProvider)
        advanceUntilIdle()

        // Eid al-Fitr (10,1) = 1 Shawwal 1447 = 19 Mar 2026; navigate to Mar 2026
        repeat(8) { vm.nextMonth(); advanceUntilIdle() }
        assertEquals(3, vm.uiState.value.displayedMonth)
        assertEquals(2026, vm.uiState.value.displayedYear)

        val grid = vm.uiState.value.grid
        val cell = grid.firstOrNull { it?.gregorianDay == 19 }
        assertEquals("Eid al-Fitr", cell?.events?.single()?.titleEn)
    }
}
