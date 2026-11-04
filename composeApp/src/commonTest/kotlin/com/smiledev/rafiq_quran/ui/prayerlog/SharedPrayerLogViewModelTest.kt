package com.smiledev.rafiq_quran.ui.prayerlog

import com.smiledev.rafiq_quran.domain.repository.PrayerLogDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedPrayerLogViewModelTest {

    private val sampleLogs = listOf(
        PrayerLogDay(date = "2026-10-28", fajr = true, dhuhr = true, asr = true, maghrib = false, isha = false),
        PrayerLogDay(date = "2026-10-27", fajr = true, dhuhr = true, asr = true, maghrib = true, isha = true),
        PrayerLogDay(date = "2026-10-26", fajr = true, dhuhr = true, asr = true, maghrib = true, isha = true),
        PrayerLogDay(date = "2026-10-25", fajr = true, dhuhr = true, asr = true, maghrib = true, isha = true)
    )

    @Test
    fun testInitialStateAndStreakCalculation() {
        val viewModel = SharedPrayerLogViewModel(
            initialTodayDate = "2026-10-28",
            initialLogs = sampleLogs
        )
        val state = viewModel.uiState.value

        assertEquals("2026-10-28", state.todayDate)
        assertEquals("2026-10-28", state.selectedDate)
        // Today is not all completed (3/5), but yesterday back 3 days are all completed -> streak = 3
        assertEquals(3, state.currentStreak)
        assertEquals(7, state.recentDays.size)
        assertEquals("2026-10-28", state.recentDays.last().date)
        assertTrue(state.recentDays.last().isToday)
    }

    @Test
    fun testCompletingTodayIncreasesStreak() {
        val viewModel = SharedPrayerLogViewModel(
            initialTodayDate = "2026-10-28",
            initialLogs = sampleLogs
        )
        // Mark remaining prayers
        viewModel.togglePrayer("maghrib", true)
        viewModel.togglePrayer("isha", true)

        val state = viewModel.uiState.value
        assertTrue(state.selectedLog.isAllCompleted)
        assertEquals(5, state.selectedLog.completedCount)
        // Now today is complete, so streak is 1 (today) + 3 (previous days) = 4
        assertEquals(4, state.currentStreak)
    }

    @Test
    fun testMarkAllObligatoryAndReset() {
        val viewModel = SharedPrayerLogViewModel(
            initialTodayDate = "2026-10-28",
            initialLogs = sampleLogs
        )
        viewModel.markAllObligatory(true)
        assertTrue(viewModel.uiState.value.selectedLog.isAllCompleted)

        viewModel.markAllObligatory(false)
        assertEquals(0, viewModel.uiState.value.selectedLog.completedCount)
        assertFalse(viewModel.uiState.value.selectedLog.isAllCompleted)
    }

    @Test
    fun testSunnahToggling() {
        val viewModel = SharedPrayerLogViewModel(
            initialTodayDate = "2026-10-28",
            initialLogs = sampleLogs
        )
        viewModel.toggleSunnah("tahajjud", true)
        assertTrue(viewModel.uiState.value.selectedSunnah["tahajjud"] == true)

        viewModel.toggleSunnah("tahajjud", false)
        assertFalse(viewModel.uiState.value.selectedSunnah["tahajjud"] == true)
    }

    @Test
    fun testDateNavigation() {
        val viewModel = SharedPrayerLogViewModel(
            initialTodayDate = "2026-10-28",
            initialLogs = sampleLogs
        )
        viewModel.previousDay()
        assertEquals("2026-10-27", viewModel.uiState.value.selectedDate)

        // nextDay moves back to today
        viewModel.nextDay()
        assertEquals("2026-10-28", viewModel.uiState.value.selectedDate)

        // nextDay beyond today is not allowed
        viewModel.nextDay()
        assertEquals("2026-10-28", viewModel.uiState.value.selectedDate)

        viewModel.selectDate("2026-10-25")
        assertEquals("2026-10-25", viewModel.uiState.value.selectedDate)

        viewModel.jumpToToday()
        assertEquals("2026-10-28", viewModel.uiState.value.selectedDate)
    }

    @Test
    fun testDateMathUtilities() {
        assertEquals("2026-10-29", SharedPrayerLogViewModel.offsetDateString("2026-10-28", 1))
        assertEquals("2026-10-27", SharedPrayerLogViewModel.offsetDateString("2026-10-28", -1))
        assertEquals("2026-11-01", SharedPrayerLogViewModel.offsetDateString("2026-10-31", 1))
        assertEquals("2026-10-31", SharedPrayerLogViewModel.offsetDateString("2026-11-01", -1))
        assertEquals("2027-01-01", SharedPrayerLogViewModel.offsetDateString("2026-12-31", 1))
        assertEquals("2026-12-31", SharedPrayerLogViewModel.offsetDateString("2027-01-01", -1))
    }
}
