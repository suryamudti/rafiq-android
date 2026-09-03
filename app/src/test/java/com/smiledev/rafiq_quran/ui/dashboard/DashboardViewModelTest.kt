package com.smiledev.rafiq_quran.ui.dashboard

import android.content.Context
import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.PrayerTimesData
import com.smiledev.rafiq_quran.domain.model.PrayerTimings
import com.smiledev.rafiq_quran.domain.model.Surah
import com.smiledev.rafiq_quran.domain.repository.PrayerLogDay
import com.smiledev.rafiq_quran.domain.repository.PrayerLogRepository
import com.smiledev.rafiq_quran.domain.repository.PrayerTimesRepository
import com.smiledev.rafiq_quran.domain.repository.QuranRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)

    private val prayerTimesRepository: PrayerTimesRepository = mockk(relaxed = true)
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val quranRepository: QuranRepository = mockk(relaxed = true)
    private val prayerLogRepository: PrayerLogRepository = mockk(relaxed = true)

    private val latFlow = MutableStateFlow("-6.2088")
    private val lonFlow = MutableStateFlow("106.8456")
    private val methodFlow = MutableStateFlow(20)
    private val cityFlow = MutableStateFlow("Jakarta")
    private val lastReadSuraFlow = MutableStateFlow(2)
    private val lastReadAyaFlow = MutableStateFlow(255)
    private val prayerLogsFlow = MutableStateFlow<List<PrayerLogDay>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { preferencesManager.latitude } returns latFlow
        every { preferencesManager.longitude } returns lonFlow
        every { preferencesManager.prayerCalculationMethod } returns methodFlow
        every { preferencesManager.cityName } returns cityFlow
        every { preferencesManager.lastReadSura } returns lastReadSuraFlow
        every { preferencesManager.lastReadAya } returns lastReadAyaFlow
        every { prayerLogRepository.observeAll() } returns prayerLogsFlow

        coEvery {
            prayerTimesRepository.fetchPrayerTimes(any(), any(), any(), any())
        } returns Result.Success(
            PrayerTimesData(
                timings = PrayerTimings(
                    imsak = "04:30",
                    fajr = "04:42",
                    sunrise = "05:58",
                    dhuhr = "11:58",
                    asr = "15:18",
                    maghrib = "18:02",
                    isha = "19:12"
                ),
                hijriDate = "14 Ramadan 1447"
            )
        )

        val surahs = listOf(
            Surah(
                id = 2,
                chapterNumber = 2,
                nameArabic = "البقرة",
                nameSimple = "Al-Baqarah",
                translatedName = "The Cow",
                versesCount = 286,
                revelationPlace = "madinah"
            )
        )
        every { quranRepository.getChapters(any()) } returns Result.Success(surahs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state initializes greeting, gregorian date, and daily inspiration`() = runTest(testDispatcher) {
        val vm = DashboardViewModel(
            prayerTimesRepository = prayerTimesRepository,
            preferencesManager = preferencesManager,
            context = context,
            dispatcherProvider = testDispatcherProvider,
            quranRepository = quranRepository,
            prayerLogRepository = prayerLogRepository,
            enablePeriodicCountdown = false
        )
        testScheduler.runCurrent()

        val state = vm.uiState.value
        assertTrue(state.greeting.isNotBlank())
        assertTrue(state.gregorianDate.isNotBlank())
        assertTrue(state.dailyAyahArabic.isNotBlank())
        assertTrue(state.dailyAyahTranslation.isNotBlank())
        assertTrue(state.dailyAyahSurahRef.isNotBlank())
    }

    @Test
    fun `loadPrayerTimes populates hijriDate and prayerTimeline`() = runTest(testDispatcher) {
        val vm = DashboardViewModel(
            prayerTimesRepository = prayerTimesRepository,
            preferencesManager = preferencesManager,
            context = context,
            dispatcherProvider = testDispatcherProvider,
            quranRepository = quranRepository,
            prayerLogRepository = prayerLogRepository,
            enablePeriodicCountdown = false
        )
        testScheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals("14 Ramadan 1447", state.hijriDate)
        assertEquals(5, state.prayerTimeline.size)
        assertTrue(state.nextPrayerName.isNotBlank())
        assertTrue(state.nextPrayerTime.isNotBlank())
        assertTrue(state.countdown.isNotBlank())
    }

    @Test
    fun `last read Surah and Ayah are observed and name is resolved`() = runTest(testDispatcher) {
        val vm = DashboardViewModel(
            prayerTimesRepository = prayerTimesRepository,
            preferencesManager = preferencesManager,
            context = context,
            dispatcherProvider = testDispatcherProvider,
            quranRepository = quranRepository,
            prayerLogRepository = prayerLogRepository,
            enablePeriodicCountdown = false
        )
        testScheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals(2, state.lastReadSura)
        assertEquals(255, state.lastReadAya)
        assertEquals("Al-Baqarah", state.lastReadSuraName)
    }

    @Test
    fun `todayCompletedPrayersCount counts true prayers for today`() = runTest(testDispatcher) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val todayLog = PrayerLogDay(
            date = todayStr,
            fajr = true,
            dhuhr = true,
            asr = true,
            maghrib = false,
            isha = false
        )
        prayerLogsFlow.value = listOf(todayLog)

        val vm = DashboardViewModel(
            prayerTimesRepository = prayerTimesRepository,
            preferencesManager = preferencesManager,
            context = context,
            dispatcherProvider = testDispatcherProvider,
            quranRepository = quranRepository,
            prayerLogRepository = prayerLogRepository,
            enablePeriodicCountdown = false
        )
        testScheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals(3, state.todayCompletedPrayersCount)
    }

    @Test
    fun `periodic countdown can be started and stopped cleanly`() = runTest(testDispatcher) {
        val vm = DashboardViewModel(
            prayerTimesRepository = prayerTimesRepository,
            preferencesManager = preferencesManager,
            context = context,
            dispatcherProvider = testDispatcherProvider,
            quranRepository = quranRepository,
            prayerLogRepository = prayerLogRepository,
            enablePeriodicCountdown = true
        )
        testScheduler.runCurrent()
        vm.stopCountdown()
    }
}
