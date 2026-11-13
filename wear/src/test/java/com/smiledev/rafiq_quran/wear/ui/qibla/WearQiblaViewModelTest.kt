package com.smiledev.rafiq_quran.wear.ui.qibla

import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.usecase.CalculateQiblaUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class TestDispatcherProvider(
    private val testDispatcher: CoroutineDispatcher
) : DispatcherProvider {
    override val main: CoroutineDispatcher get() = testDispatcher
    override val io: CoroutineDispatcher get() = testDispatcher
    override val default: CoroutineDispatcher get() = testDispatcher
    override val unconfined: CoroutineDispatcher get() = testDispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class WearQiblaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)

    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private val calculateQiblaUseCase = CalculateQiblaUseCase()

    private val latFlow = MutableStateFlow("-6.2088")
    private val lonFlow = MutableStateFlow("106.8456")
    private val cityFlow = MutableStateFlow("Jakarta")

    @Before
    fun setUp() {
        every { preferencesManager.latitude } returns latFlow
        every { preferencesManager.longitude } returns lonFlow
        every { preferencesManager.cityName } returns cityFlow
    }

    @Test
    fun `initial state calculates accurate Qibla bearing and distance for Jakarta`() = runTest(testDispatcher) {
        val viewModel = WearQiblaViewModel(
            preferencesManager = preferencesManager,
            calculateQiblaUseCase = calculateQiblaUseCase,
            dispatcherProvider = testDispatcherProvider
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Jakarta", state.cityName)
        assertEquals(295, state.bearing)
        assertTrue("Distance should be ~7900+ km, was ${state.distanceKm}", state.distanceKm in 7900..8000)
        assertEquals("WNW", state.directionCardinal)
    }

    @Test
    fun `recalculate updates state when coordinates change`() = runTest(testDispatcher) {
        val viewModel = WearQiblaViewModel(
            preferencesManager = preferencesManager,
            calculateQiblaUseCase = calculateQiblaUseCase,
            dispatcherProvider = testDispatcherProvider
        )
        advanceUntilIdle()

        // London coordinates: 51.5074 N, -0.1278 W -> Bearing to Kaaba ~119° ESE, distance ~4798 km
        viewModel.recalculate(51.5074, -0.1278, "London")

        val state = viewModel.uiState.value
        assertEquals("London", state.cityName)
        assertEquals(119, state.bearing)
        assertTrue(state.distanceKm in 4700..4900)
        assertEquals("ESE", state.directionCardinal)
    }

    @Test
    fun `preferences flow emission updates Qibla state`() = runTest(testDispatcher) {
        val viewModel = WearQiblaViewModel(
            preferencesManager = preferencesManager,
            calculateQiblaUseCase = calculateQiblaUseCase,
            dispatcherProvider = testDispatcherProvider
        )
        advanceUntilIdle()

        // Update preferences with Makkah coordinates (Kaaba location)
        latFlow.value = "21.4225"
        lonFlow.value = "39.8262"
        cityFlow.value = "Makkah"
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Makkah", state.cityName)
        assertEquals(0, state.bearing)
        assertEquals(0, state.distanceKm)
        assertEquals("N", state.directionCardinal)
    }

    @Test
    fun `wearCompassDirection returns correct cardinal points for 16-wind compass rose`() {
        assertEquals("N", wearCompassDirection(0))
        assertEquals("N", wearCompassDirection(360))
        assertEquals("N", wearCompassDirection(-5))
        assertEquals("NE", wearCompassDirection(45))
        assertEquals("E", wearCompassDirection(90))
        assertEquals("SE", wearCompassDirection(135))
        assertEquals("S", wearCompassDirection(180))
        assertEquals("SW", wearCompassDirection(225))
        assertEquals("W", wearCompassDirection(270))
        assertEquals("WNW", wearCompassDirection(295))
        assertEquals("NNW", wearCompassDirection(337))

        // Indonesian locale
        assertEquals("U", wearCompassDirection(0, "id"))
        assertEquals("TL", wearCompassDirection(45, "id"))
        assertEquals("T", wearCompassDirection(90, "id"))
        assertEquals("TG", wearCompassDirection(135, "id"))
        assertEquals("S", wearCompassDirection(180, "id"))
        assertEquals("BD", wearCompassDirection(225, "id"))
        assertEquals("B", wearCompassDirection(270, "id"))
        assertEquals("BBL", wearCompassDirection(295, "id"))
    }

    @Test
    fun `wearNormalizeAngle180 normalizes angles between -180 and 180 degrees`() {
        assertEquals(0.0, wearNormalizeAngle180(0.0), 0.001)
        assertEquals(90.0, wearNormalizeAngle180(90.0), 0.001)
        assertEquals(-90.0, wearNormalizeAngle180(-90.0), 0.001)
        assertEquals(-180.0, wearNormalizeAngle180(180.0), 0.001)
        assertEquals(-170.0, wearNormalizeAngle180(190.0), 0.001)
        assertEquals(-10.0, wearNormalizeAngle180(350.0), 0.001)
        assertEquals(170.0, wearNormalizeAngle180(-190.0), 0.001)
        assertEquals(10.0, wearNormalizeAngle180(-350.0), 0.001)
        assertEquals(45.0, wearNormalizeAngle180(405.0), 0.001)
    }

    @Test
    fun `isQiblaAligned returns true when azimuth is within threshold`() {
        val bearing = 295

        assertTrue(isQiblaAligned(bearing, 295f))
        assertTrue(isQiblaAligned(bearing, 298f))
        assertTrue(isQiblaAligned(bearing, 292f))
        assertTrue(isQiblaAligned(bearing, 300f)) // exactly 5°
        assertTrue(isQiblaAligned(bearing, 290f)) // exactly 5°
    }

    @Test
    fun `isQiblaAligned returns false when azimuth is outside threshold`() {
        val bearing = 295

        assertFalse(isQiblaAligned(bearing, 301f)) // 6° difference
        assertFalse(isQiblaAligned(bearing, 289f)) // 6° difference
        assertFalse(isQiblaAligned(bearing, 0f))
        assertFalse(isQiblaAligned(bearing, 180f))
    }

    @Test
    fun `isQiblaAligned handles wrap-around angles across 0 and 360 degrees boundary`() {
        // Bearing 358° vs Azimuth 2° -> diff is 4° (aligned within 5°)
        assertTrue(isQiblaAligned(358, 2f))
        // Bearing 2° vs Azimuth 358° -> diff is 4° (aligned within 5°)
        assertTrue(isQiblaAligned(2, 358f))

        // Bearing 0° vs Azimuth 356° -> diff is 4° (aligned)
        assertTrue(isQiblaAligned(0, 356f))
        // Bearing 0° vs Azimuth 354° -> diff is 6° (unaligned)
        assertFalse(isQiblaAligned(0, 354f))

        // Bearing 355° vs Azimuth 3° -> diff is 8° (unaligned)
        assertFalse(isQiblaAligned(355, 3f))
    }

    @Test
    fun `refreshLocation invokes WearLocationSync`() = runTest(testDispatcher) {
        val locationSync: com.smiledev.rafiq_quran.wear.location.WearLocationSync = mockk()
        io.mockk.coEvery { locationSync.syncIfPermitted(any()) } returns true

        val viewModel = WearQiblaViewModel(
            preferencesManager = preferencesManager,
            calculateQiblaUseCase = calculateQiblaUseCase,
            dispatcherProvider = testDispatcherProvider,
            wearLocationSync = locationSync
        )
        advanceUntilIdle()

        viewModel.refreshLocation(forceGeocode = true)
        advanceUntilIdle()

        io.mockk.coVerify(exactly = 1) { locationSync.syncIfPermitted(true) }
        assertFalse(viewModel.uiState.value.isSyncingLocation)
    }
}
