package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationProviderImplTest {

    private lateinit var context: Context
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationTask: Task<Location>

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)
        fusedClient = mockk(relaxed = true)
        locationTask = mockk(relaxed = true)

        mockkStatic(LocationServices::class)
        mockkStatic(Tasks::class)
        every { LocationServices.getFusedLocationProviderClient(context) } returns fusedClient
        every { fusedClient.lastLocation } returns locationTask
        every { preferencesManager.latitude } returns flowOf("-6.2088")
        every { preferencesManager.longitude } returns flowOf("106.8456")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun testDispatcherProvider(dispatcher: CoroutineDispatcher) = object : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val unconfined: CoroutineDispatcher = dispatcher
    }

    @Test
    fun `getLastLocation rethrows CancellationException instead of falling back`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        every { Tasks.await(locationTask) } throws CancellationException("cancelled")

        val provider = LocationProviderImpl(context, preferencesManager, testDispatcherProvider(dispatcher))

        var caughtCancellation = false
        try {
            provider.getLastLocation()
        } catch (_: CancellationException) {
            caughtCancellation = true
        }

        assertTrue("Expected CancellationException to be rethrown", caughtCancellation)
    }

    @Test
    fun `getLastLocation falls back to preferences on non-cancellation error`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        every { Tasks.await(locationTask) } throws SecurityException("no permission")

        val provider = LocationProviderImpl(context, preferencesManager, testDispatcherProvider(dispatcher))
        val result = provider.getLastLocation()

        assertTrue(result is Result.Success)
        val geo = (result as Result.Success).data
        assertEquals(-6.2088, geo.latitude, 0.0001)
        assertEquals(106.8456, geo.longitude, 0.0001)
    }

    @Test
    fun `getLastLocation saves and returns fetched location`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val location = mockk<Location> {
            every { latitude } returns 21.4225
            every { longitude } returns 39.8262
        }
        every { Tasks.await(locationTask) } returns location

        val provider = LocationProviderImpl(context, preferencesManager, testDispatcherProvider(dispatcher))
        val result = provider.getLastLocation()

        assertTrue(result is Result.Success)
        val geo = (result as Result.Success).data
        assertEquals(21.4225, geo.latitude, 0.0001)
        assertEquals(39.8262, geo.longitude, 0.0001)
        coVerify { preferencesManager.setLatitude("21.4225") }
        coVerify { preferencesManager.setLongitude("39.8262") }
    }
}
