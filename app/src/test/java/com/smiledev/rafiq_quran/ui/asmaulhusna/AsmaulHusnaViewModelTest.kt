package com.smiledev.rafiq_quran.ui.asmaulhusna

import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.AsmaulHusna
import com.smiledev.rafiq_quran.domain.repository.AsmaulHusnaRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AsmaulHusnaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val repository: AsmaulHusnaRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk()

    private fun createViewModel(initialFavorites: Set<Int> = emptySet()): AsmaulHusnaViewModel {
        every { preferencesManager.favoriteAsmaulHusnaIds } returns flowOf(initialFavorites)
        coEvery { preferencesManager.toggleFavoriteAsmaulHusna(any()) } returns Unit
        return AsmaulHusnaViewModel(repository, preferencesManager, testDispatcherProvider)
    }

    @Test
    fun `load names success`() = runTest(testDispatcher) {
        val names = listOf(
            AsmaulHusna(1, "الرحمن", "Ar-Rahman", "The Most Gracious", "Maha Pengasih", "Benefit", "Manfaat")
        )
        every { repository.getNames() } returns Result.Success(names)

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.names.size)
        assertEquals("Ar-Rahman", vm.uiState.value.names[0].transliteration)
    }

    @Test
    fun `load names error`() = runTest(testDispatcher) {
        every { repository.getNames() } returns Result.Error(AppError.Unknown("fail"))

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.names.size)
    }

    @Test
    fun `filteredNames returns all when query empty`() = runTest(testDispatcher) {
        val names = listOf(
            AsmaulHusna(1, "الرحمن", "Ar-Rahman", "The Most Gracious", "Maha Pengasih", "B", "M")
        )
        every { repository.getNames() } returns Result.Success(names)

        val vm = createViewModel()
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

        val vm = createViewModel()
        advanceUntilIdle()

        vm.search("Rahim")
        assertEquals(1, vm.filteredNames().size)
        assertEquals("Ar-Rahim", vm.filteredNames()[0].transliteration)
    }

    @Test
    fun `initial state has isLoading true`() {
        val vm = createViewModel()
        assertEquals(true, vm.uiState.value.isLoading)
        assertEquals(0, vm.uiState.value.names.size)
    }

    @Test
    fun `filterNames matches meaning, transliteration, arabic, and number`() {
        val names = listOf(
            AsmaulHusna(1, "الرحمن", "Ar-Rahman", "The Most Gracious", "Maha Pengasih", "B1", "M1"),
            AsmaulHusna(2, "الرحيم", "Ar-Rahim", "The Most Merciful", "Maha Penyayang", "B2", "M2"),
            AsmaulHusna(3, "الملك", "Al-Malik", "The King", "Maha Raja", "B3", "M3")
        )
        val vm = createViewModel()

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

        // Matches Number
        val byNumber = vm.filterNames(names, "2")
        assertEquals(1, byNumber.size)
        assertEquals("Ar-Rahim", byNumber[0].transliteration)

        // Trims whitespace
        val trimmed = vm.filterNames(names, "  king  ")
        assertEquals(1, trimmed.size)
        assertEquals("Al-Malik", trimmed[0].transliteration)

        // Unmatched query returns empty
        val unmatched = vm.filterNames(names, "unknown query")
        assertEquals(0, unmatched.size)
    }

    @Test
    fun `toggleFavorite adds and removes favorite`() = runTest(testDispatcher) {
        every { repository.getNames() } returns Result.Success(emptyList())
        val vm = createViewModel(initialFavorites = setOf(1))
        advanceUntilIdle()

        assertTrue(1 in vm.uiState.value.favoriteIds)

        // Toggle existing ID removes it
        vm.toggleFavorite(1)
        advanceUntilIdle()
        assertTrue(1 !in vm.uiState.value.favoriteIds)
        coVerify { preferencesManager.toggleFavoriteAsmaulHusna(1) }

        // Toggle new ID adds it
        vm.toggleFavorite(2)
        advanceUntilIdle()
        assertTrue(2 in vm.uiState.value.favoriteIds)
        coVerify { preferencesManager.toggleFavoriteAsmaulHusna(2) }
    }

    @Test
    fun `filterNames with showFavoritesOnly filters properly`() {
        val names = listOf(
            AsmaulHusna(1, "الرحمن", "Ar-Rahman", "The Most Gracious", "Maha Pengasih", "B1", "M1"),
            AsmaulHusna(2, "الرحيم", "Ar-Rahim", "The Most Merciful", "Maha Penyayang", "B2", "M2")
        )
        val vm = createViewModel()

        val favsOnly = vm.filterNames(
            names = names,
            query = "",
            showFavoritesOnly = true,
            favoriteIds = setOf(2)
        )
        assertEquals(1, favsOnly.size)
        assertEquals("Ar-Rahim", favsOnly[0].transliteration)
    }

    @Test
    fun `selectName and dhikr counter state changes`() {
        val item = AsmaulHusna(1, "الرحمن", "Ar-Rahman", "The Most Gracious", "Maha Pengasih", "B1", "M1")
        val vm = createViewModel()

        assertNull(vm.uiState.value.selectedName)
        assertEquals(0, vm.uiState.value.dhikrCount)

        vm.selectName(item)
        assertEquals(item, vm.uiState.value.selectedName)

        vm.incrementDhikr()
        vm.incrementDhikr()
        assertEquals(2, vm.uiState.value.dhikrCount)

        vm.resetDhikr()
        assertEquals(0, vm.uiState.value.dhikrCount)

        vm.incrementDhikr()
        vm.selectName(null)
        assertNull(vm.uiState.value.selectedName)
        assertEquals(0, vm.uiState.value.dhikrCount)
    }

    @Test
    fun `setGridView toggles grid view state`() {
        val vm = createViewModel()
        assertEquals(false, vm.uiState.value.isGridView)

        vm.setGridView(true)
        assertEquals(true, vm.uiState.value.isGridView)
    }

    @Test
    fun `dailyName and randomName return valid items`() {
        val names = listOf(
            AsmaulHusna(1, "الرحمن", "Ar-Rahman", "The Most Gracious", "Maha Pengasih", "B1", "M1"),
            AsmaulHusna(2, "الرحيم", "Ar-Rahim", "The Most Merciful", "Maha Penyayang", "B2", "M2")
        )
        every { repository.getNames() } returns Result.Success(names)
        val vm = createViewModel()

        val daily = vm.getDailyName(names)
        assertNotNull(daily)
        assertTrue(daily!!.id in 1..2)

        val random = vm.getRandomName()
        // before advanceUntilIdle, names is empty in state
        assertNull(random)
    }
}
