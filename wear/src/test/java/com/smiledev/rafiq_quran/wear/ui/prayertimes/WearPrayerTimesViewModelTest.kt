package com.smiledev.rafiq_quran.wear.ui.prayertimes

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.PrayerTimesData
import com.smiledev.rafiq_quran.domain.model.PrayerTimings
import com.smiledev.rafiq_quran.domain.repository.PrayerTimesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class TestDispatcherProvider(
    private val testDispatcher: CoroutineDispatcher
) : DispatcherProvider {
    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
    override val unconfined: CoroutineDispatcher = testDispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class WearPrayerTimesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)

    private val prayerTimesRepository: PrayerTimesRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk()

    private val fakePrayerData = PrayerTimesData(
        timings = PrayerTimings(
            imsak = "04:15",
            fajr = "04:30",
            sunrise = "05:45",
            dhuhr = "11:55",
            asr = "15:10",
            maghrib = "17:55",
            isha = "19:05"
        ),
        hijriDate = "14 Shawwal 1445"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { preferencesManager.latitude } returns flowOf("-6.2088")
        every { preferencesManager.longitude } returns flowOf("106.8456")
        every { preferencesManager.prayerCalculationMethod } returns flowOf(20)
        every { preferencesManager.cityName } returns flowOf("Jakarta")

        coEvery {
            prayerTimesRepository.getCachedPrayerTimes(any(), any(), any(), any())
        } returns null

        coEvery {
            prayerTimesRepository.fetchPrayerTimes(any(), any(), any(), any(), any())
        } returns Result.Success(fakePrayerData)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel(): WearPrayerTimesViewModel {
        return WearPrayerTimesViewModel(
            prayerTimesRepository = prayerTimesRepository,
            preferencesManager = preferencesManager,
            dispatcherProvider = testDispatcherProvider
        ).apply {
            enablePeriodicCountdown = false
        }
    }

    @Test
    fun `loading cached prayer times populates 8 prayer entries and hijri date`() = runTest(testDispatcher) {
        coEvery {
            prayerTimesRepository.getCachedPrayerTimes(any(), any(), any(), any())
        } returns fakePrayerData

        val vm = createViewModel()
        testScheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals("Jakarta", state.cityName)
        assertEquals("KEMENAG", state.calculationMethodName)
        assertEquals("14 Shawwal 1445", state.hijriDate)
        assertEquals(8, state.prayerTimes.size)

        val expectedKeys = listOf("imsak", "fajr", "sunrise", "dhuha", "dhuhr", "asr", "maghrib", "isha")
        assertEquals(expectedKeys, state.prayerTimes.map { it.prayerKey })

        val dhuhaEntry = state.prayerTimes.first { it.prayerKey == "dhuha" }
        assertEquals("06:05", dhuhaEntry.time)

        assertFalse(state.isLoading)
        assertNull(state.error)

        coVerify(exactly = 0) {
            prayerTimesRepository.fetchPrayerTimes(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `network fallback when cache is empty calls fetchPrayerTimes`() = runTest(testDispatcher) {
        coEvery {
            prayerTimesRepository.getCachedPrayerTimes(any(), any(), any(), any())
        } returns null
        coEvery {
            prayerTimesRepository.fetchPrayerTimes(any(), any(), any(), any(), any())
        } returns Result.Success(fakePrayerData)

        val vm = createViewModel()
        testScheduler.runCurrent()

        coVerify(atLeast = 1) {
            prayerTimesRepository.fetchPrayerTimes(
                lat = -6.2088,
                lon = 106.8456,
                date = any(),
                method = 20,
                forceRefresh = false
            )
        }

        val state = vm.uiState.value
        assertEquals(8, state.prayerTimes.size)
        assertEquals("14 Shawwal 1445", state.hijriDate)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `error handling sets uiState error`() = runTest(testDispatcher) {
        coEvery {
            prayerTimesRepository.getCachedPrayerTimes(any(), any(), any(), any())
        } returns null
        val networkError = AppError.Network("Network failure", null)
        coEvery {
            prayerTimesRepository.fetchPrayerTimes(any(), any(), any(), any(), any())
        } returns Result.Error(networkError)

        val vm = createViewModel()
        testScheduler.runCurrent()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(networkError, state.error)
        assertTrue(state.prayerTimes.isEmpty())
    }

    @Test
    fun `day navigation updates currentDate and reloads`() = runTest(testDispatcher) {
        coEvery {
            prayerTimesRepository.getCachedPrayerTimes(any(), any(), any(), any())
        } returns fakePrayerData

        val vm = createViewModel()
        testScheduler.runCurrent()

        assertTrue(vm.uiState.value.isToday)
        val initialDate = vm.uiState.value.currentDate

        vm.goToNextDay()
        testScheduler.runCurrent()
        assertFalse(vm.uiState.value.isToday)
        assertTrue(vm.uiState.value.currentDate.after(initialDate))

        vm.goToPreviousDay()
        testScheduler.runCurrent()
        assertTrue(vm.uiState.value.isToday)

        vm.goToPreviousDay()
        testScheduler.runCurrent()
        assertFalse(vm.uiState.value.isToday)
        assertTrue(vm.uiState.value.currentDate.before(initialDate))

        vm.goToToday()
        testScheduler.runCurrent()
        assertTrue(vm.uiState.value.isToday)

        coVerify(atLeast = 4) {
            prayerTimesRepository.getCachedPrayerTimes(any(), any(), any(), any())
        }
    }

    @Test
    fun `refresh calls fetchPrayerTimes with forceRefresh true`() = runTest(testDispatcher) {
        coEvery {
            prayerTimesRepository.getCachedPrayerTimes(any(), any(), any(), any())
        } returns fakePrayerData
        coEvery {
            prayerTimesRepository.fetchPrayerTimes(any(), any(), any(), any(), forceRefresh = true)
        } returns Result.Success(fakePrayerData)

        val vm = createViewModel()
        testScheduler.runCurrent()

        vm.refresh()
        testScheduler.runCurrent()

        coVerify(exactly = 1) {
            prayerTimesRepository.fetchPrayerTimes(any(), any(), any(), any(), forceRefresh = true)
        }
    }
}
