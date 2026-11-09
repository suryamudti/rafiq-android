package com.smiledev.rafiq_quran.ui.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals

class SharedDashboardViewModelTest {

    @Test
    fun initialState_hasExpectedDefaultValues() {
        val viewModel = SharedDashboardViewModel()
        val state = viewModel.uiState.value

        assertEquals("Assalamu'alaikum", state.greeting)
        assertEquals("Jakarta, Indonesia", state.cityName)
        assertEquals("Maghrib", state.nextPrayerName)
        assertEquals(2, state.lastReadSura)
        assertEquals(255, state.lastReadAya)
        assertEquals("Al-Baqarah", state.lastReadSuraName)
    }

    @Test
    fun updateCity_updatesCityName() {
        val viewModel = SharedDashboardViewModel()

        viewModel.updateCity("Surabaya, Indonesia")
        assertEquals("Surabaya, Indonesia", viewModel.uiState.value.cityName)
    }

    @Test
    fun updateLastRead_updatesPosition() {
        val viewModel = SharedDashboardViewModel()

        viewModel.updateLastRead(18, "Al-Kahf", 10)
        val state = viewModel.uiState.value

        assertEquals(18, state.lastReadSura)
        assertEquals("Al-Kahf", state.lastReadSuraName)
        assertEquals(10, state.lastReadAya)
    }
}
