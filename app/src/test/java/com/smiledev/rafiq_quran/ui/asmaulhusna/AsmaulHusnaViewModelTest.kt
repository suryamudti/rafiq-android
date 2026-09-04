package com.smiledev.rafiq_quran.ui.asmaulhusna

import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.AsmaulHusna
import com.smiledev.rafiq_quran.domain.repository.AsmaulHusnaRepository
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

    @Test
    fun `initial state has isLoading true`() {
        val vm = AsmaulHusnaViewModel(repository, testDispatcherProvider)
        assertEquals(true, vm.uiState.value.isLoading)
        assertEquals(0, vm.uiState.value.names.size)
    }

    @Test
    fun `filterNames matches meaning, transliteration, and arabic`() {
        val names = listOf(
            AsmaulHusna(1, "الرحمن", "Ar-Rahman", "The Most Gracious", "Maha Pengasih", "B1", "M1"),
            AsmaulHusna(2, "الرحيم", "Ar-Rahim", "The Most Merciful", "Maha Penyayang", "B2", "M2"),
            AsmaulHusna(3, "الملك", "Al-Malik", "The King", "Maha Raja", "B3", "M3")
        )
        val vm = AsmaulHusnaViewModel(repository, testDispatcherProvider)

        // Matches English meaning
        val byMeaningEn = vm.filterNames(names, "gracious")
        assertEquals(1, byMeaningEn.size)
        assertEquals("Ar-Rahman", byMeaningEn[0].transliteration)

        // Matches Indonesian meaning
        val byMeaningId = vm.filterNames(names, "penyayang")
        assertEquals(1, byMeaningId.size)
        assertEquals("Ar-Rahim", byMeaningId[0].transliteration)

        // Matches Arabic
        val byArabic = vm.filterNames(names, "الملك")
        assertEquals(1, byArabic.size)
        assertEquals("Al-Malik", byArabic[0].transliteration)

        // Trims whitespace
        val trimmed = vm.filterNames(names, "  king  ")
        assertEquals(1, trimmed.size)
        assertEquals("Al-Malik", trimmed[0].transliteration)

        // Unmatched query returns empty
        val unmatched = vm.filterNames(names, "unknown query")
        assertEquals(0, unmatched.size)
    }
}
