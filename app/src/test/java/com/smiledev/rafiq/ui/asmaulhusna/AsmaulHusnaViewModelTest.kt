package com.smiledev.rafiq.ui.asmaulhusna

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.AsmaulHusna
import com.smiledev.rafiq.domain.repository.AsmaulHusnaRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AsmaulHusnaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val repository: AsmaulHusnaRepository = mockk()

    @Test
    fun `load names success`() = runTest(testDispatcher) {
        val names = listOf(
            AsmaulHusna(1, "الرحمن", "Ar-Rahman", "The Most Gracious", "Maha Pengasih", "Benefit", "Manfaat")
        )
        every { repository.getNames() } returns Result.Success(names)

        val vm = AsmaulHusnaViewModel(repository, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.names.size)
        assertEquals("Ar-Rahman", vm.uiState.value.names[0].transliteration)
    }

    @Test
    fun `load names error`() = runTest(testDispatcher) {
        every { repository.getNames() } returns Result.Error(AppError.Unknown("fail"))

        val vm = AsmaulHusnaViewModel(repository, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.names.size)
    }

    @Test
    fun `filteredNames returns all when query empty`() = runTest(testDispatcher) {
        val names = listOf(
            AsmaulHusna(1, "الرحمن", "Ar-Rahman", "The Most Gracious", "Maha Pengasih", "B", "M")
        )
        every { repository.getNames() } returns Result.Success(names)

        val vm = AsmaulHusnaViewModel(repository, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(1, vm.filteredNames().size)
    }

    @Test
    fun `filteredNames filters by query`() = runTest(testDispatcher) {
        val names = listOf(
            AsmaulHusna(1, "الرحمن", "Ar-Rahman", "The Most Gracious", "Maha Pengasih", "B", "M"),
            AsmaulHusna(2, "الرحيم", "Ar-Rahim", "The Most Merciful", "Maha Penyayang", "B", "M")
        )
        every { repository.getNames() } returns Result.Success(names)

        val vm = AsmaulHusnaViewModel(repository, testDispatcherProvider)
        advanceUntilIdle()

        vm.search("Rahim")
        assertEquals(1, vm.filteredNames().size)
        assertEquals("Ar-Rahim", vm.filteredNames()[0].transliteration)
    }
}
