package com.smiledev.rafiq_quran.ui.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedCalendarViewModelTest {

    @Test
    fun initialState_buildsValidCalendarGrid() {
        val viewModel = SharedCalendarViewModel(initialYear = 2026, initialMonth = 10, initialDay = 28)
        val state = viewModel.uiState.value

        assertEquals(2026, state.displayedYear)
        assertEquals(10, state.displayedMonth)
        assertTrue(state.grid.isNotEmpty())
        assertTrue(state.events.isNotEmpty())
        assertNull(state.selectedDay)

        // October has 31 days, so at least 31 non-null day cells must exist
        val nonNullDays = state.grid.filterNotNull()
        assertEquals(31, nonNullDays.size)

        // Today must be marked correctly
        val todayCell = nonNullDays.find { it.isToday }
        assertNotNull(todayCell)
        assertEquals(28, todayCell.gregorianDay)
        assertEquals(10, todayCell.gregorianMonth)
    }

    @Test
    fun nextMonth_advancesMonthAndYearAcrossDecember() {
        val viewModel = SharedCalendarViewModel(initialYear = 2026, initialMonth = 12, initialDay = 1)
        viewModel.nextMonth()

        val state = viewModel.uiState.value
        assertEquals(2027, state.displayedYear)
        assertEquals(1, state.displayedMonth)
    }

    @Test
    fun previousMonth_decrementsMonthAndYearAcrossJanuary() {
        val viewModel = SharedCalendarViewModel(initialYear = 2026, initialMonth = 1, initialDay = 1)
        viewModel.previousMonth()

        val state = viewModel.uiState.value
        assertEquals(2025, state.displayedYear)
        assertEquals(12, state.displayedMonth)
    }

    @Test
    fun goToToday_resetsDisplayedMonthAndYear() {
        val viewModel = SharedCalendarViewModel(initialYear = 2026, initialMonth = 10, initialDay = 28)
        viewModel.nextMonth()
        viewModel.nextMonth()
        assertEquals(12, viewModel.uiState.value.displayedMonth)

        viewModel.goToToday()
        assertEquals(10, viewModel.uiState.value.displayedMonth)
        assertEquals(2026, viewModel.uiState.value.displayedYear)
    }

    @Test
    fun selectDay_updatesSelectedState() {
        val viewModel = SharedCalendarViewModel(initialYear = 2026, initialMonth = 10, initialDay = 28)
        val day = viewModel.uiState.value.grid.filterNotNull().first()

        viewModel.selectDay(day)
        assertEquals(day, viewModel.uiState.value.selectedDay)

        viewModel.selectDay(null)
        assertNull(viewModel.uiState.value.selectedDay)
    }

    @Test
    fun filterEvents_matchesQuery() {
        val viewModel = SharedCalendarViewModel()
        viewModel.setSearchQuery("Ramadan")

        val filtered = viewModel.getFilteredEvents(viewModel.uiState.value)
        assertTrue(filtered.isNotEmpty())
        assertTrue(filtered.any { it.titleEn.contains("Ramadan", ignoreCase = true) })
    }
}
