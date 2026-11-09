package com.smiledev.rafiq_quran.ui.qibla

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedQiblaViewModelTest {

    @Test
    fun initialState_calculatesCorrectJakartaBearingAndDistance() {
        val viewModel = SharedQiblaViewModel()
        val state = viewModel.uiState.value

        assertEquals("Jakarta, Indonesia", state.cityName)
        assertEquals(295, state.bearing)
        assertTrue(state.distanceKm in 7900..8000)
        assertEquals("WNW", state.compassDirection)
    }

    @Test
    fun isAligned_returnsTrue_whenAzimuthIsWithinThreshold() {
        val viewModel = SharedQiblaViewModel()
        val bearing = viewModel.uiState.value.bearing.toFloat() // 295f

        assertTrue(viewModel.isAligned(bearing))
        assertTrue(viewModel.isAligned(bearing + 3f))
        assertTrue(viewModel.isAligned(bearing - 4f))
    }

    @Test
    fun isAligned_returnsFalse_whenAzimuthIsOutsideThreshold() {
        val viewModel = SharedQiblaViewModel()
        val bearing = viewModel.uiState.value.bearing.toFloat() // 295f

        assertFalse(viewModel.isAligned(bearing + 10f))
        assertFalse(viewModel.isAligned(bearing - 15f))
        assertFalse(viewModel.isAligned(0f))
        assertFalse(viewModel.isAligned(180f))
    }

    @Test
    fun updateCoordinates_recalculatesBearingForNewCity() {
        val viewModel = SharedQiblaViewModel()

        // London coordinates (approx 119° bearing to Makkah)
        viewModel.updateCoordinates("London, UK", 51.5074, -0.1278)
        val state = viewModel.uiState.value

        assertEquals("London, UK", state.cityName)
        assertEquals(119, state.bearing)
        assertTrue(state.distanceKm in 4700..4900)
        assertEquals("ESE", state.compassDirection)
    }

    @Test
    fun compassDirection_calculatesCorrectCardinalPoints() {
        assertEquals("N", calculateCompassDirection(0))
        assertEquals("E", calculateCompassDirection(90))
        assertEquals("S", calculateCompassDirection(180))
        assertEquals("W", calculateCompassDirection(270))
        assertEquals("NW", calculateCompassDirection(315))
    }

    @Test
    fun normalizeAngleDifference_handlesWrapAround() {
        val diff1 = normalizeAngleDifference(5f, 355f)
        assertEquals(10f, diff1)

        val diff2 = normalizeAngleDifference(355f, 5f)
        assertEquals(-10f, diff2)
    }
}
