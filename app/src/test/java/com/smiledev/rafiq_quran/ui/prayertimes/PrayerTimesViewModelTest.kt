package com.smiledev.rafiq_quran.ui.prayertimes

import android.content.Context
import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.PrayerTimesData
import com.smiledev.rafiq_quran.domain.model.PrayerTimings
import com.smiledev.rafiq_quran.domain.repository.PrayerTimesRepository
import com.smiledev.rafiq_quran.service.PrayerNotificationWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTimesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)

    private val prayerTimesRepository: PrayerTimesRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk()
    private val context: Context = mockk(relaxed = true)

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
        mockkObject(PrayerNotificationWorker.Companion)
        every { PrayerNotificationWorker.scheduleNow(any()) } just runs

        every { preferencesManager.latitude } returns flowOf("-6.2088")
        every { preferencesManager.longitude } returns flowOf("106.8456")
        every { preferencesManager.prayerCalculationMethod } returns flowOf(20)
        every { preferencesManager.cityName } returns flowOf("Jakarta")
        every { preferencesManager.prayerNotificationsEnabled } returns flowOf(true)
        every { preferencesManager.translationLanguage } returns flowOf("en")

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

    @Test
    fun `initial load sets prayer times and metadata`() = runTest(testDispatcher) {
        val vm = PrayerTimesViewModel(
            prayerTimesRepository = prayerTimesRepository,
            preferencesManager = preferencesManager,
            appContext = context,
            dispatcherProvider = testDispatcherProvider
        ).apply {
            enablePeriodicCountdown = false
        }

        testScheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals("Jakarta", state.cityName)
        assertEquals("KEMENAG", state.calculationMethodName)
        assertEquals("14 Shawwal 1445", state.hijriDate)
        assertEquals(8, state.prayerTimes.size)
        assertTrue(state.isToday)
        assertFalse(state.isLoading)
    }

    @Test
    fun `navigation to previous day and next day updates isToday flag`() = runTest(testDispatcher) {
        val vm = PrayerTimesViewModel(
            prayerTimesRepository = prayerTimesRepository,
            preferencesManager = preferencesManager,
            appContext = context,
            dispatcherProvider = testDispatcherProvider
        ).apply {
            enablePeriodicCountdown = false
        }

        testScheduler.runCurrent()
        assertTrue(vm.uiState.value.isToday)

        vm.goToPreviousDay()
        testScheduler.runCurrent()
        assertFalse(vm.uiState.value.isToday)

        vm.goToToday()
        testScheduler.runCurrent()
        assertTrue(vm.uiState.value.isToday)

        vm.goToNextDay()
        testScheduler.runCurrent()
        assertFalse(vm.uiState.value.isToday)
    }

    @Test
    fun `toggle notifications calls preferencesManager`() = runTest(testDispatcher) {
        coEvery { preferencesManager.setPrayerNotificationsEnabled(any()) } just runs

        val vm = PrayerTimesViewModel(
            prayerTimesRepository = prayerTimesRepository,
            preferencesManager = preferencesManager,
            appContext = context,
            dispatcherProvider = testDispatcherProvider
        ).apply {
            enablePeriodicCountdown = false
        }

        testScheduler.runCurrent()

        vm.toggleNotifications()
        testScheduler.runCurrent()

        coVerify { preferencesManager.setPrayerNotificationsEnabled(false) }
    }
}
