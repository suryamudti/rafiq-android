package com.smiledev.rafiq_quran.ui.mosques

import android.content.Context
import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.Mosque
import com.smiledev.rafiq_quran.domain.repository.GeoLocation
import com.smiledev.rafiq_quran.domain.repository.LocationProvider
import com.smiledev.rafiq_quran.domain.repository.MosqueRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.every
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MosquesViewModelTest {

    private val context: Context = mockk(relaxed = true)
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private val mosqueRepository: MosqueRepository = mockk()
    private val locationProvider: LocationProvider = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider = TestDispatcherProvider(testDispatcher)

    private lateinit var viewModel: MosquesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { mosqueRepository.getNearbyMosques(any(), any(), any()) } returns Result.Success(emptyList())
        
        // Mock preferences flows
        every { preferencesManager.latitude } returns flowOf("-6.2088")
        every { preferencesManager.longitude } returns flowOf("106.8456")
        
        viewModel = MosquesViewModel(context, preferencesManager, mosqueRepository, locationProvider, dispatcherProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onPermissionResult false sets showPermissionDenied`() = runTest(testDispatcher) {
        viewModel.onPermissionResult(false)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showPermissionDenied)
    }

    @Test
    fun `onPermissionResult true sets locationGranted and fetches location`() = runTest(testDispatcher) {
        coEvery { locationProvider.getLastLocation() } returns Result.Success(GeoLocation(-6.2, 106.8))

        viewModel.onPermissionResult(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.locationGranted)
        assertFalse(viewModel.uiState.value.showPermissionDenied)
        assertEquals(-6.2, viewModel.uiState.value.userLocation?.latitude ?: 0.0, 0.001)
        assertEquals(106.8, viewModel.uiState.value.userLocation?.longitude ?: 0.0, 0.001)
    }

    @Test
    fun `loadMosques success updates mosques list`() = runTest(testDispatcher) {
        val mosques = listOf(
            Mosque(1, "Masjid Istiqlal", -6.2, 106.8),
            Mosque(2, "Masjid Raya", -6.21, 106.81)
        )
        coEvery { locationProvider.getLastLocation() } returns Result.Success(GeoLocation(-6.2088, 106.8456))
        coEvery { mosqueRepository.getNearbyMosques(-6.2088, 106.8456, 5000) } returns Result.Success(mosques)

        viewModel.onPermissionResult(true)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.mosques.size)
        assertEquals("Masjid Istiqlal", viewModel.uiState.value.mosques[0].name)
    }

    @Test
    fun `loadMosques error sets error state`() = runTest(testDispatcher) {
        coEvery { locationProvider.getLastLocation() } returns Result.Success(GeoLocation(-6.2088, 106.8456))
        coEvery { mosqueRepository.getNearbyMosques(-6.2088, 106.8456, 5000) } returns Result.Error(com.smiledev.rafiq_quran.core.AppError.Network("Network error", Exception()))

        viewModel.onPermissionResult(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.error != null)
        assertTrue(viewModel.uiState.value.error is com.smiledev.rafiq_quran.core.AppError.Network)
    }
    
    @Test
    fun `locationProvider error falls back to default location and loads mosques`() = runTest(testDispatcher) {
        coEvery { locationProvider.getLastLocation() } returns Result.Error(com.smiledev.rafiq_quran.core.AppError.Network("GPS error", Exception()))
        coEvery { mosqueRepository.getNearbyMosques(-6.2088, 106.8456, 5000) } returns Result.Success(emptyList())

        viewModel.onPermissionResult(true)
        advanceUntilIdle()

        // Should load mosques at default location (error cleared after successful load)
        assertFalse(viewModel.uiState.value.showPermissionDenied)
    }
}