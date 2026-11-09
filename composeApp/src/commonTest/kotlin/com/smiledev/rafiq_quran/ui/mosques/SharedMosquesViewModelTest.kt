package com.smiledev.rafiq_quran.ui.mosques

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedMosquesViewModelTest {

    @Test
    fun initialState_sortsMosquesByDistance() {
        val viewModel = SharedMosquesViewModel()
        val state = viewModel.uiState.value

        assertEquals("Jakarta, Indonesia", state.cityName)
        assertTrue(state.isMapView)
        assertTrue(state.mosques.isNotEmpty())

        // Verify sorted order
        for (i in 0 until state.mosques.size - 1) {
            assertTrue(state.mosques[i].distanceMeters <= state.mosques[i + 1].distanceMeters)
        }
    }

    @Test
    fun selectMosque_updatesSelectedMosque() {
        val viewModel = SharedMosquesViewModel()
        assertNull(viewModel.uiState.value.selectedMosque)

        val target = viewModel.uiState.value.mosques.first().mosque
        viewModel.selectMosque(target)
        assertEquals(target, viewModel.uiState.value.selectedMosque)

        viewModel.selectMosque(null)
        assertNull(viewModel.uiState.value.selectedMosque)
    }

    @Test
    fun toggleViewMode_flipsMapAndList() {
        val viewModel = SharedMosquesViewModel()
        assertTrue(viewModel.uiState.value.isMapView)

        viewModel.toggleViewMode()
        assertFalse(viewModel.uiState.value.isMapView)

        viewModel.toggleViewMode()
        assertTrue(viewModel.uiState.value.isMapView)
    }

    @Test
    fun setRadius_updatesRadius() {
        val viewModel = SharedMosquesViewModel()
        assertEquals(2000, viewModel.uiState.value.radiusMeters)

        viewModel.setRadius(5000)
        assertEquals(5000, viewModel.uiState.value.radiusMeters)
    }

    @Test
    fun updateUserLocation_recalculatesDistances() {
        val viewModel = SharedMosquesViewModel()
        // Move to Bandung
        viewModel.updateUserLocation(-6.9175, 107.6191, "Bandung, Indonesia")
        val state = viewModel.uiState.value

        assertEquals("Bandung, Indonesia", state.cityName)
        assertEquals(-6.9175, state.userLat)
        assertEquals(107.6191, state.userLon)

        // Distances from Bandung to Jakarta mosques should be > 100 km (~110-140 km)
        assertTrue(state.mosques.first().distanceMeters > 100_000)
    }

    @Test
    fun formatDistanceMeters_formatsCorrectly() {
        assertEquals("450 m", formatDistanceMeters(450))
        assertEquals("1.2 km", formatDistanceMeters(1200))
        assertEquals("15 km", formatDistanceMeters(15000))
    }
}
