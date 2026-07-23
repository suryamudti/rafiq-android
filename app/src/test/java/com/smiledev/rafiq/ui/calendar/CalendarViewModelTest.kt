package com.smiledev.rafiq.ui.calendar

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.IslamicEvent
import com.smiledev.rafiq.domain.repository.IslamicCalendarRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val repository: IslamicCalendarRepository = mockk()

    @Test
    fun `load events success`() = runTest(testDispatcher) {
        val events = listOf(
            IslamicEvent(1, 1, "Islamic New Year", "Tahun Baru Islam", "Desc", "Desc", "holiday")
        )
        every { repository.getEvents() } returns Result.Success(events)
        every { repository.getTodayEvents() } returns Result.Success(emptyList())
        every { repository.getEventsForMonth(any()) } returns Result.Success(events)
        every { repository.islamicMonthNames } returns listOf("Muharram")
        every { repository.islamicMonthNamesId } returns listOf("Muharram")

        val vm = CalendarViewModel(repository, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.events.size)
    }

    @Test
    fun `selectMonth updates selected month`() = runTest(testDispatcher) {
        val events = listOf(
            IslamicEvent(2, 10, "Day of Ashura", "Hari Asyura", "D", "D", "religious")
        )
        every { repository.getEvents() } returns Result.Success(emptyList())
        every { repository.getTodayEvents() } returns Result.Success(emptyList())
        every { repository.getEventsForMonth(any()) } returns Result.Success(emptyList())
        every { repository.getEventsForMonth(2) } returns Result.Success(events)
        every { repository.islamicMonthNames } returns listOf("Muharram", "Safar")
        every { repository.islamicMonthNamesId } returns listOf("Muharram", "Safar")

        val vm = CalendarViewModel(repository, testDispatcherProvider)
        advanceUntilIdle()

        vm.selectMonth(2)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.selectedMonth)
    }
}
