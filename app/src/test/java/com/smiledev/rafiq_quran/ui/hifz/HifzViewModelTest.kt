package com.smiledev.rafiq_quran.ui.hifz

import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.repository.HifzGoal
import com.smiledev.rafiq_quran.domain.repository.HifzRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HifzViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val hifzRepository: HifzRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { hifzRepository.observeAll() } returns MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads goals on init`() = runTest(testDispatcher) {
        val goal = HifzGoal(id = 1, suraNumber = 112, suraName = "Al-Ikhlas", ayaStart = 1, ayaEnd = 4)
        every { hifzRepository.observeAll() } returns MutableStateFlow(listOf(goal))

        val vm = HifzViewModel(hifzRepository, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.goals.size)
    }

    @Test
    fun `toggleAyah adds ayah and upserts`() = runTest(testDispatcher) {
        val goal = HifzGoal(id = 1, suraNumber = 112, suraName = "Al-Ikhlas", ayaStart = 1, ayaEnd = 4)
        every { hifzRepository.observeAll() } returns MutableStateFlow(listOf(goal))
        coEvery { hifzRepository.upsert(any()) } returns Result.Success(Unit)

        val vm = HifzViewModel(hifzRepository, testDispatcherProvider)
        advanceUntilIdle()

        vm.toggleAyah(goalId = 1, aya = 2)
        advanceUntilIdle()

        coVerify { hifzRepository.upsert(goal.copy(memorizedAyahs = setOf(2))) }
    }

    @Test
    fun `toggleAyah removes already memorized ayah`() = runTest(testDispatcher) {
        val goal = HifzGoal(
            id = 1, suraNumber = 112, suraName = "Al-Ikhlas", ayaStart = 1, ayaEnd = 4,
            memorizedAyahs = setOf(2)
        )
        every { hifzRepository.observeAll() } returns MutableStateFlow(listOf(goal))
        coEvery { hifzRepository.upsert(any()) } returns Result.Success(Unit)

        val vm = HifzViewModel(hifzRepository, testDispatcherProvider)
        advanceUntilIdle()

        vm.toggleAyah(goalId = 1, aya = 2)
        advanceUntilIdle()

        coVerify { hifzRepository.upsert(goal.copy(memorizedAyahs = emptySet())) }
    }

    @Test
    fun `addGoal with inverted range does not upsert`() = runTest(testDispatcher) {
        coEvery { hifzRepository.upsert(any()) } returns Result.Success(Unit)

        val vm = HifzViewModel(hifzRepository, testDispatcherProvider)
        advanceUntilIdle()

        vm.addGoal(suraNumber = 2, suraName = "Al-Baqarah", ayaStart = 10, ayaEnd = 5)
        advanceUntilIdle()

        coVerify(exactly = 0) { hifzRepository.upsert(any()) }
    }

    @Test
    fun `deleteGoal delegates to repository`() = runTest(testDispatcher) {
        coEvery { hifzRepository.delete(any()) } returns Result.Success(Unit)

        val vm = HifzViewModel(hifzRepository, testDispatcherProvider)
        advanceUntilIdle()

        vm.deleteGoal(7)
        advanceUntilIdle()

        coVerify { hifzRepository.delete(7) }
    }
}
