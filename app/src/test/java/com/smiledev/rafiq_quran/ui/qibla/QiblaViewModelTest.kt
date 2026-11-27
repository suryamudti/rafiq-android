package com.smiledev.rafiq_quran.ui.qibla

import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.usecase.CalculateQiblaUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QiblaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val preferencesManager: PreferencesManager = mockk()
    private val calculateQiblaUseCase = CalculateQiblaUseCase()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads saved coordinates from preferences and calculates qibla`() = runTest(testDispatcher) {
        val lat = 3.1390
        val lon = 101.6869
        every { preferencesManager.latitude } returns flowOf(lat.toString())
        every { preferencesManager.longitude } returns flowOf(lon.toString())

        val vm = QiblaViewModel(preferencesManager, calculateQiblaUseCase, testDispatcherProvider)
        advanceUntilIdle()

        val expected = calculateQiblaUseCase(lat, lon)
        assertEquals(expected.bearing, vm.uiState.value.bearing)
        assertEquals(expected.distanceKm, vm.uiState.value.distanceKm)
        assertEquals(lat, vm.uiState.value.userLat, 0.0001)
        assertEquals(lon, vm.uiState.value.userLon, 0.0001)
        assertEquals(false, vm.uiState.value.isLoading)
    }

    @Test
    fun `init falls back to Jakarta default coordinates when preferences are invalid`() = runTest(testDispatcher) {
        every { preferencesManager.latitude } returns flowOf("not-a-number")
        every { preferencesManager.longitude } returns flowOf("")

        val vm = QiblaViewModel(preferencesManager, calculateQiblaUseCase, testDispatcherProvider)
        advanceUntilIdle()

        val expected = calculateQiblaUseCase(-6.2088, 106.8456)
        assertEquals(expected.bearing, vm.uiState.value.bearing)
        assertEquals(expected.distanceKm, vm.uiState.value.distanceKm)
        assertEquals(-6.2088, vm.uiState.value.userLat, 0.0001)
        assertEquals(106.8456, vm.uiState.value.userLon, 0.0001)
    }

    @Test
    fun `recalculate updates bearing distance and user coordinates`() = runTest(testDispatcher) {
        every { preferencesManager.latitude } returns flowOf("-6.2088")
        every { preferencesManager.longitude } returns flowOf("106.8456")

        val vm = QiblaViewModel(preferencesManager, calculateQiblaUseCase, testDispatcherProvider)
        advanceUntilIdle()

        val londonLat = 51.5074
        val londonLon = -0.1278
        vm.recalculate(londonLat, londonLon)

        val expected = calculateQiblaUseCase(londonLat, londonLon)
        assertEquals(expected.bearing, vm.uiState.value.bearing)
        assertEquals(expected.distanceKm, vm.uiState.value.distanceKm)
        assertEquals(londonLat, vm.uiState.value.userLat, 0.0001)
        assertEquals(londonLon, vm.uiState.value.userLon, 0.0001)
    }

    @Test
    fun `compassDirection returns English and Indonesian cardinal points including wrapped bearings`() {
        assertEquals("N", compassDirection(0, "en"))
        assertEquals("N", compassDirection(360, "en"))
        assertEquals("E", compassDirection(90, "en"))
        assertEquals("S", compassDirection(180, "en"))
        assertEquals("W", compassDirection(270, "en"))
        assertEquals("W", compassDirection(-90, "en"))
        assertEquals("WNW", compassDirection(295, "en"))

        assertEquals("U", compassDirection(0, "id"))
        assertEquals("T", compassDirection(90, "id"))
        assertEquals("S", compassDirection(180, "id"))
        assertEquals("B", compassDirection(270, "id"))
        assertEquals("BBL", compassDirection(295, "id"))
    }

    @Test
    fun `normalizeAngle180 wraps angles into minus 180 to 180 range`() {
        assertEquals(0.0, normalizeAngle180(0.0), 0.0001)
        assertEquals(90.0, normalizeAngle180(90.0), 0.0001)
        assertEquals(-90.0, normalizeAngle180(270.0), 0.0001)
        assertEquals(90.0, normalizeAngle180(-270.0), 0.0001)
        assertEquals(-180.0, normalizeAngle180(180.0), 0.0001)
        assertEquals(-180.0, normalizeAngle180(-180.0), 0.0001)
        assertEquals(-180.0, normalizeAngle180(540.0), 0.0001)
        assertEquals(45.0, normalizeAngle180(405.0), 0.0001)
    }
}
