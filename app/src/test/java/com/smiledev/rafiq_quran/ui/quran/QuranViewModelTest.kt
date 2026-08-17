package com.smiledev.rafiq_quran.ui.quran

import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.domain.model.Surah
import com.smiledev.rafiq_quran.domain.repository.QuranRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuranViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val quranRepository: QuranRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)

    private val surah = Surah(1, 1, "الفاتحة", "Al-Fatiha", "Al-Fatiha", 7, "meccan")
    private val ayah = Ayah(sura = 1, aya = 1, text = "بِسْمِ ٱللَّهِ", bismillah = null,
        translation = "In the name of Allah")

    private fun createVm(): QuranViewModel {
        every { preferencesManager.translationLanguage } returns MutableStateFlow("system")
        every { quranRepository.getChapters(any()) } returns Result.Success(emptyList())
        return QuranViewModel(quranRepository, preferencesManager, testDispatcherProvider)
    }

    @Test
    fun `load surahs success`() = runTest(testDispatcher) {
        val vm = createVm()
        every { quranRepository.getChapters(any()) } returns Result.Success(listOf(surah))
        vm.loadSurahs()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.surahs.size)
        assertEquals("Al-Fatiha", vm.uiState.value.surahs[0].translatedName)
    }

    @Test
    fun `load surahs error sets error state`() = runTest(testDispatcher) {
        val vm = createVm()
        every { quranRepository.getChapters(any()) } returns Result.Error(AppError.Database("Failed", null))
        vm.loadSurahs()
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.surahs.size)
    }

    @Test
    fun `search populates results after debounce`() = runTest(testDispatcher) {
        every { quranRepository.searchAyahs("In the name", "en", 100) } returns Result.Success(listOf(ayah))

        val vm = createVm()
        vm.search("In the name")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertEquals(listOf(ayah), vm.uiState.value.searchResults)
        assertEquals(false, vm.uiState.value.searchLoading)
    }

    @Test
    fun `debounce cancels the earlier keystroke`() = runTest(testDispatcher) {
        every { quranRepository.searchAyahs("In", "en", 100) } returns Result.Success(listOf(ayah))
        every { quranRepository.searchAyahs("In the name", "en", 100) } returns Result.Success(listOf(ayah))

        val vm = createVm()
        vm.search("In")
        advanceTimeBy(100)
        vm.search("In the name")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertEquals("In the name", vm.uiState.value.searchQuery)
        assertEquals(listOf(ayah), vm.uiState.value.searchResults)
    }

    @Test
    fun `blank query clears results without hitting repo`() = runTest(testDispatcher) {
        every { quranRepository.searchAyahs("In the name", "en", 100) } returns Result.Success(listOf(ayah))

        val vm = createVm()
        vm.search("In the name")
        advanceTimeBy(250)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.searchResults.size)

        vm.search("   ")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.searchResults.isEmpty())
    }

    @Test
    fun `search error surfaces in state`() = runTest(testDispatcher) {
        every { quranRepository.searchAyahs("boom", "en", 100) } returns Result.Error(AppError.Database("fail", null))

        val vm = createVm()
        vm.search("boom")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.searchError != null)
        assertEquals(false, vm.uiState.value.searchLoading)
    }

    @Test
    fun `resolvedLanguage maps system to locale code`() = runTest(testDispatcher) {
        val vm = createVm()
        assertEquals("en", vm.resolvedLanguage())
    }
}
