package com.smiledev.rafiq_quran.ui.prayerlog

import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.repository.PrayerLogDay
import com.smiledev.rafiq_quran.domain.repository.PrayerLogRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class PrayerLogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val prayerLogRepository: PrayerLogRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { prayerLogRepository.observeAll() } returns MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load logs on init`() = runTest(testDispatcher) {
        val log = PrayerLogDay(date = "2024-01-01", fajr = true, dhuhr = true)
        val flow = MutableStateFlow(listOf(log))
        every { prayerLogRepository.observeAll() } returns flow

        val vm = PrayerLogViewModel(prayerLogRepository, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.logs.size)
    }

    @Test
    fun `toggle prayer fajr on calls upsert`() = runTest(testDispatcher) {
        val flow = MutableStateFlow(emptyList<PrayerLogDay>())
        every { prayerLogRepository.observeAll() } returns flow
        coEvery { prayerLogRepository.upsert(any()) } returns Result.Success(Unit)

        val vm = PrayerLogViewModel(prayerLogRepository, testDispatcherProvider)
        advanceUntilIdle()

        vm.togglePrayer("fajr", true)
        advanceUntilIdle()

        coVerify { prayerLogRepository.upsert(any()) }
    }

    @Test
    fun `toggle prayer sets correct value`() = runTest(testDispatcher) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val existingLog = PrayerLogDay(date = today, fajr = false)
        val flow = MutableStateFlow(listOf(existingLog))
        every { prayerLogRepository.observeAll() } returns flow

        val vm = PrayerLogViewModel(prayerLogRepository, testDispatcherProvider)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.todayLog?.fajr ?: true)
    }

    @Test
    fun `calculate streak includes yesterday when today in progress`() = runTest(testDispatcher) {
        val vm = PrayerLogViewModel(prayerLogRepository, testDispatcherProvider)
        val logsMap = mapOf(
            "2026-09-17" to PrayerLogDay(date = "2026-09-17", fajr = true), // partial today
            "2026-09-16" to PrayerLogDay(date = "2026-09-16", fajr = true, dhuhr = true, asr = true, maghrib = true, isha = true),
            "2026-09-15" to PrayerLogDay(date = "2026-09-15", fajr = true, dhuhr = true, asr = true, maghrib = true, isha = true),
            "2026-09-14" to PrayerLogDay(date = "2026-09-14", fajr = true) // broken
        )

        val streak = vm.calculateStreak("2026-09-17", logsMap)
        assertEquals(2, streak)
    }

    @Test
    fun `calculate streak includes today when today all completed`() = runTest(testDispatcher) {
        val vm = PrayerLogViewModel(prayerLogRepository, testDispatcherProvider)
        val logsMap = mapOf(
            "2026-09-17" to PrayerLogDay(date = "2026-09-17", fajr = true, dhuhr = true, asr = true, maghrib = true, isha = true),
            "2026-09-16" to PrayerLogDay(date = "2026-09-16", fajr = true, dhuhr = true, asr = true, maghrib = true, isha = true)
        )

        val streak = vm.calculateStreak("2026-09-17", logsMap)
        assertEquals(2, streak)
    }

    @Test
    fun `calculate weekly stats aggregates prayers over last 7 days`() = runTest(testDispatcher) {
        val vm = PrayerLogViewModel(prayerLogRepository, testDispatcherProvider)
        val logsMap = mapOf(
            "2026-09-17" to PrayerLogDay(date = "2026-09-17", fajr = true, dhuhr = true), // 2
            "2026-09-16" to PrayerLogDay(date = "2026-09-16", fajr = true, dhuhr = true, asr = true, maghrib = true, isha = true) // 5
        )

        val (completed, total) = vm.calculateWeeklyStats("2026-09-17", logsMap)
        assertEquals(7, completed)
        assertEquals(35, total)
    }

    @Test
    fun `date navigation previousDay, nextDay, and selectDate work properly`() = runTest(testDispatcher) {
        val flow = MutableStateFlow(emptyList<PrayerLogDay>())
        every { prayerLogRepository.observeAll() } returns flow

        val vm = PrayerLogViewModel(prayerLogRepository, testDispatcherProvider)
        advanceUntilIdle()

        val initialToday = vm.uiState.value.todayDate
        assertEquals(initialToday, vm.uiState.value.selectedDate)

        vm.previousDay()
        val prev = vm.uiState.value.selectedDate
        org.junit.Assert.assertNotEquals(initialToday, prev)

        vm.nextDay()
        assertEquals(initialToday, vm.uiState.value.selectedDate)

        // nextDay cannot exceed today
        vm.nextDay()
        assertEquals(initialToday, vm.uiState.value.selectedDate)

        vm.selectDate("2026-01-01")
        assertEquals("2026-01-01", vm.uiState.value.selectedDate)

        vm.jumpToToday()
        assertEquals(initialToday, vm.uiState.value.selectedDate)
    }

    @Test
    fun `markAllPrayers updates all 5 prayers`() = runTest(testDispatcher) {
        val flow = MutableStateFlow(emptyList<PrayerLogDay>())
        every { prayerLogRepository.observeAll() } returns flow
        coEvery { prayerLogRepository.upsert(any()) } returns Result.Success(Unit)

        val vm = PrayerLogViewModel(prayerLogRepository, testDispatcherProvider)
        advanceUntilIdle()

        vm.markAllPrayers(true)
        advanceUntilIdle()

        coVerify {
            prayerLogRepository.upsert(match {
                it.fajr && it.dhuhr && it.asr && it.maghrib && it.isha
            })
        }
    }
}
