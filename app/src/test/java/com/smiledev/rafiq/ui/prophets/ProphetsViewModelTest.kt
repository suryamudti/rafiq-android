package com.smiledev.rafiq.ui.prophets

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.ProphetStory
import com.smiledev.rafiq.domain.repository.ProphetRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProphetsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val prophetRepository: ProphetRepository = mockk()

    @Test
    fun `load prophets success`() = runTest(testDispatcher) {
        val prophets = listOf(
            ProphetStory(1, "آدم", "Adam", "Adam", "First prophet", "Nabi pertama", "Story", "Kisah", "Miracles", "Mukjizat")
        )
        every { prophetRepository.getProphets() } returns Result.Success(prophets)

        val vm = ProphetsViewModel(prophetRepository, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.prophets.size)
    }

    @Test
    fun `load prophets error`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Error(AppError.Database("fail", null))

        val vm = ProphetsViewModel(prophetRepository, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.prophets.size)
    }

    @Test
    fun `filteredProphets returns all when query empty`() = runTest(testDispatcher) {
        val prophets = listOf(
            ProphetStory(1, "آدم", "Adam", "Adam", "First", "Pertama", "S", "K", "M", "Muk"),
            ProphetStory(2, "نوح", "Noah", "Nuh", "Second", "Kedua", "S", "K", "M", "Muk")
        )
        every { prophetRepository.getProphets() } returns Result.Success(prophets)

        val vm = ProphetsViewModel(prophetRepository, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(2, vm.filteredProphets().size)
    }

    @Test
    fun `filteredProphets filters by query`() = runTest(testDispatcher) {
        val prophets = listOf(
            ProphetStory(1, "آدم", "Adam", "Adam", "First", "Pertama", "S", "K", "M", "Muk"),
            ProphetStory(2, "نوح", "Noah", "Nuh", "Second", "Kedua", "S", "K", "M", "Muk")
        )
        every { prophetRepository.getProphets() } returns Result.Success(prophets)

        val vm = ProphetsViewModel(prophetRepository, testDispatcherProvider)
        advanceUntilIdle()

        vm.search("Noah")
        assertEquals(1, vm.filteredProphets().size)
    }
}
