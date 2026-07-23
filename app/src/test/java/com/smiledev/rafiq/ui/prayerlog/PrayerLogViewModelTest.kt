package com.smiledev.rafiq.ui.prayerlog

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.repository.PrayerLogDay
import com.smiledev.rafiq.domain.repository.PrayerLogRepository
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
}
