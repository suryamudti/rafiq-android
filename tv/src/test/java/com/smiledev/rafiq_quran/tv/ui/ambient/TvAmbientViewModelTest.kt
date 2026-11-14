package com.smiledev.rafiq_quran.tv.ui.ambient

import android.content.Context
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.PrayerTimesData
import com.smiledev.rafiq_quran.domain.model.PrayerTimings
import com.smiledev.rafiq_quran.domain.repository.PrayerTimesRepository
import com.smiledev.rafiq_quran.tv.service.TvPrayerNotificationWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
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
import org.junit.Assert.assertNotNull
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
class TvAmbientViewModelTest {

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
        TvPrayerNotificationWorker.dismissInAppAdhanAlert()
        mockkObject(TvPrayerNotificationWorker.Companion)
        every { TvPrayerNotificationWorker.scheduleNow(any()) } just runs
        every { TvPrayerNotificationWorker.cancelScheduledAlarms(any()) } just runs

        every { preferencesManager.latitude } returns flowOf("-6.2088")
        every { preferencesManager.longitude } returns flowOf("106.8456")
        every { preferencesManager.prayerCalculationMethod } returns flowOf(20)
        every { preferencesManager.cityName } returns flowOf("Jakarta")
        every { preferencesManager.prayerNotificationsEnabled } returns flowOf(true)

        coEvery { preferencesManager.setCityName(any()) } just runs
        coEvery { preferencesManager.setLatitude(any()) } just runs
        coEvery { preferencesManager.setLongitude(any()) } just runs
        coEvery { preferencesManager.setPrayerCalculationMethod(any()) } just runs
        coEvery { preferencesManager.setPrayerNotificationsEnabled(any()) } just runs

        coEvery {
            prayerTimesRepository.getCachedPrayerTimes(any(), any(), any(), any())
        } returns null

        coEvery {
            prayerTimesRepository.fetchPrayerTimes(any(), any(), any(), any(), any())
        } returns Result.Success(fakePrayerData)
    }

    @After
    fun tearDown() {
        TvPrayerNotificationWorker.dismissInAppAdhanAlert()
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel(): TvAmbientViewModel {
        return TvAmbientViewModel(
            prayerTimesRepository = prayerTimesRepository,
            preferencesManager = preferencesManager,
            appContext = context,
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
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Jakarta", state.cityName)
        assertEquals("KEMENAG", state.calculationMethodName)
        assertEquals("14 Shawwal 1445", state.hijriDate)
        assertEquals(8, state.prayerTimes.size)
        assertEquals("04:15", state.prayerTimes[0].time)
        assertEquals("04:30", state.prayerTimes[1].time)
        assertEquals("06:05", state.prayerTimes[3].time) // Dhuha = sunrise (05:45) + 20m
        assertEquals("19:05", state.prayerTimes[7].time)
        assertTrue(state.prayerNotificationsEnabled)
        verify(atLeast = 1) { TvPrayerNotificationWorker.scheduleNow(context) }

        val expectedKeys = listOf("imsak", "fajr", "sunrise", "dhuha", "dhuhr", "asr", "maghrib", "isha")
        assertEquals(expectedKeys, state.prayerTimes.map { it.prayerKey })

        coVerify(exactly = 0) {
            prayerTimesRepository.fetchPrayerTimes(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `when cache is empty fetches from network and updates state`() = runTest(testDispatcher) {
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
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(8, state.prayerTimes.size)
        assertEquals("14 Shawwal 1445", state.hijriDate)
    }

    @Test
    fun `network failure when cache is empty surfaces error in state`() = runTest(testDispatcher) {
        coEvery {
            prayerTimesRepository.getCachedPrayerTimes(any(), any(), any(), any())
        } returns null
        val networkError = AppError.Network("Offline", null)
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
    fun `selectNextCity switches from Jakarta to Makkah and updates preferences`() = runTest(testDispatcher) {
        coEvery {
            prayerTimesRepository.getCachedPrayerTimes(any(), any(), any(), any())
        } returns fakePrayerData

        val vm = createViewModel()
        testScheduler.runCurrent()
        assertEquals("Jakarta", vm.uiState.value.cityName)

        vm.selectNextCity()
        testScheduler.runCurrent()

        assertEquals("Makkah", vm.uiState.value.cityName)
        assertEquals(4, vm.uiState.value.calculationMethod)
        assertEquals("Umm Al-Qura", vm.uiState.value.calculationMethodName)
        coVerify { preferencesManager.setCityName("Makkah") }
    }

    @Test
    fun `cycleCalculationMethod cycles to next calculation method`() = runTest(testDispatcher) {
        coEvery {
            prayerTimesRepository.getCachedPrayerTimes(any(), any(), any(), any())
        } returns fakePrayerData

        val vm = createViewModel()
        testScheduler.runCurrent()
        assertEquals(20, vm.uiState.value.calculationMethod)

        vm.cycleCalculationMethod()
        testScheduler.runCurrent()

        assertEquals(4, vm.uiState.value.calculationMethod)
        assertEquals("Umm Al-Qura", vm.uiState.value.calculationMethodName)
        coVerify { preferencesManager.setPrayerCalculationMethod(4) }
    }

    @Test
    fun `triggerAdhanPreview and dismissAdhanAlert update activeAdhanAlert`() = runTest(testDispatcher) {
        coEvery {
            prayerTimesRepository.getCachedPrayerTimes(any(), any(), any(), any())
        } returns fakePrayerData

        val vm = createViewModel()
        testScheduler.runCurrent()
        assertNull(vm.uiState.value.activeAdhanAlert)

        vm.triggerAdhanPreview()
        testScheduler.runCurrent()

        assertNotNull(vm.uiState.value.activeAdhanAlert)

        vm.dismissAdhanAlert()
        testScheduler.runCurrent()

        assertNull(vm.uiState.value.activeAdhanAlert)
    }

    @Test
    fun `nextSpiritualReminder cycles through Quranic reminders`() = runTest(testDispatcher) {
        coEvery {
            prayerTimesRepository.getCachedPrayerTimes(any(), any(), any(), any())
        } returns fakePrayerData

        val vm = createViewModel()
        testScheduler.runCurrent()

        assertEquals(0, vm.uiState.value.reminderIndex)
        assertEquals("QS. An-Nisa: 103", vm.uiState.value.currentReminder.reference)

        vm.nextSpiritualReminder()
        assertEquals(1, vm.uiState.value.reminderIndex)
        assertEquals("QS. Al-Baqarah: 238", vm.uiState.value.currentReminder.reference)
    }

    @Test
    fun `toggleNotifications updates state and alarms`() = runTest(testDispatcher) {
        coEvery {
            prayerTimesRepository.getCachedPrayerTimes(any(), any(), any(), any())
        } returns fakePrayerData

        val vm = createViewModel()
        testScheduler.runCurrent()

        assertTrue(vm.uiState.value.prayerNotificationsEnabled)

        vm.toggleNotifications()
        testScheduler.runCurrent()

        assertFalse(vm.uiState.value.prayerNotificationsEnabled)
        coVerify { preferencesManager.setPrayerNotificationsEnabled(false) }
        verify { TvPrayerNotificationWorker.cancelScheduledAlarms(context) }

        vm.toggleNotifications()
        testScheduler.runCurrent()

        assertTrue(vm.uiState.value.prayerNotificationsEnabled)
        coVerify { preferencesManager.setPrayerNotificationsEnabled(true) }
        verify(atLeast = 2) { TvPrayerNotificationWorker.scheduleNow(context) }
    }
}
