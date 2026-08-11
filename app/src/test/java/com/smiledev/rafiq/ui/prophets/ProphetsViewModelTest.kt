package com.smiledev.rafiq.ui.prophets

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.ProphetStory
import com.smiledev.rafiq.domain.repository.ProphetRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProphetsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val prophetRepository: ProphetRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk()

    private fun prophet(id: Int, arabic: String, en: String, idName: String) = ProphetStory(
        id = id, nameArabic = arabic, nameEn = en, nameId = idName,
        summaryEn = "S", summaryId = "S", storyEn = "S", storyId = "S",
        miraclesEn = "M", miraclesId = "M"
    )

    private fun newVm(initialFavorites: Set<Int> = emptySet()): ProphetsViewModel {
        every { preferencesManager.favoriteProphetIds } returns flowOf(initialFavorites)
        every { preferencesManager.storyFontSize } returns flowOf(16)
        coEvery { preferencesManager.toggleFavoriteProphet(any()) } returns Unit
        coEvery { preferencesManager.setStoryFontSize(any()) } returns Unit
        return ProphetsViewModel(prophetRepository, preferencesManager, testDispatcherProvider)
    }

    @Test
    fun `load prophets success`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Success(listOf(prophet(1, "آدم", "Adam", "Adam")))

        val vm = newVm()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.prophets.size)
    }

    @Test
    fun `load prophets error`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Error(AppError.Database("fail", null))

        val vm = newVm()
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.prophets.size)
    }

    @Test
    fun `filteredProphets returns all when query empty`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Success(
            listOf(prophet(1, "آدم", "Adam", "Adam"), prophet(2, "نوح", "Noah", "Nuh"))
        )

        val vm = newVm()
        advanceUntilIdle()

        assertEquals(2, vm.filteredProphets().size)
    }

    @Test
    fun `filteredProphets filters by query`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Success(
            listOf(prophet(1, "آدم", "Adam", "Adam"), prophet(2, "نوح", "Noah", "Nuh"))
        )

        val vm = newVm()
        advanceUntilIdle()

        vm.search("Noah")
        assertEquals(1, vm.filteredProphets().size)
    }

    @Test
    fun `filteredProphets matches arabic name`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Success(
            listOf(prophet(1, "آدم", "Adam", "Adam"), prophet(2, "نوح", "Noah", "Nuh"))
        )

        val vm = newVm()
        advanceUntilIdle()

        vm.search("نوح")
        assertEquals(1, vm.filteredProphets().size)
        assertEquals(2, vm.filteredProphets()[0].id)
    }

    @Test
    fun `favorites only filters by favorite ids`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Success(
            listOf(prophet(1, "آدم", "Adam", "Adam"), prophet(2, "نوح", "Noah", "Nuh"))
        )

        val vm = newVm(initialFavorites = setOf(2))
        advanceUntilIdle()

        vm.setShowFavoritesOnly(true)
        assertEquals(1, vm.filteredProphets().size)
        assertEquals(2, vm.filteredProphets()[0].id)
    }

    @Test
    fun `toggleFavorite persists and updates state`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Success(emptyList())
        val vm = newVm(initialFavorites = setOf(1))
        advanceUntilIdle()

        vm.toggleFavorite(1)
        advanceUntilIdle()

        coVerify(exactly = 1) { preferencesManager.toggleFavoriteProphet(1) }
        assertTrue(1 !in vm.uiState.value.favoriteIds)
    }

    @Test
    fun `setStoryFontSize persists`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Success(emptyList())
        val vm = newVm()
        advanceUntilIdle()

        vm.setStoryFontSize(24)
        advanceUntilIdle()

        coVerify(exactly = 1) { preferencesManager.setStoryFontSize(24) }
    }
}