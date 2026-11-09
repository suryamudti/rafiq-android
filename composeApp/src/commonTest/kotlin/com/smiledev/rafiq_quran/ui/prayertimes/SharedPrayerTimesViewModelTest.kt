package com.smiledev.rafiq_quran.ui.prayertimes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedPrayerTimesViewModelTest {

    @Test
    fun initialState_hasDefaultPrayersAndToday() {
        val viewModel = SharedPrayerTimesViewModel()
        val state = viewModel.uiState.value

        assertEquals("Jakarta, Indonesia", state.cityName)
        assertEquals("2026-10-28", state.selectedDate)
        assertTrue(state.isToday)
        assertEquals(7, state.prayers.size)
        assertTrue(state.prayerNotificationsEnabled)
    }

    @Test
    fun toggleNotifications_flipsEnabledState() {
        val viewModel = SharedPrayerTimesViewModel()

        assertTrue(viewModel.uiState.value.prayerNotificationsEnabled)
        viewModel.toggleNotifications()
        assertFalse(viewModel.uiState.value.prayerNotificationsEnabled)
        viewModel.toggleNotifications()
        assertTrue(viewModel.uiState.value.prayerNotificationsEnabled)
    }

    @Test
    fun dateNavigation_updatesSelectedDateAndTodayFlag() {
        val viewModel = SharedPrayerTimesViewModel(initialDate = "2026-10-28")

        viewModel.goToNextDay()
        val nextState = viewModel.uiState.value
        assertEquals("2026-11-01", nextState.selectedDate)
        assertFalse(nextState.isToday)

        viewModel.goToToday()
        val todayState = viewModel.uiState.value
        assertEquals("2026-10-28", todayState.selectedDate)
        assertTrue(todayState.isToday)

        viewModel.goToPreviousDay()
        assertFalse(viewModel.uiState.value.isToday)
    }

    @Test
    fun updateLocation_updatesCoordinatesAndCity() {
        val viewModel = SharedPrayerTimesViewModel()

        viewModel.updateLocation("Bandung, Indonesia", -6.9175, 107.6191)
        val state = viewModel.uiState.value

        assertEquals("Bandung, Indonesia", state.cityName)
        assertEquals(-6.9175, state.latitude)
        assertEquals(107.6191, state.longitude)
    }

    @Test
    fun setCalculationMethod_updatesMethodName() {
        val viewModel = SharedPrayerTimesViewModel()

        viewModel.setCalculationMethod("Muslim World League")
        assertEquals("Muslim World League", viewModel.uiState.value.calculationMethodName)
    }
}
