package com.smiledev.rafiq.ui.quran

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.Ayah
import com.smiledev.rafiq.domain.model.Surah
import com.smiledev.rafiq.domain.repository.BookmarkRepository
import com.smiledev.rafiq.domain.repository.QuranRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuranViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val quranRepository: QuranRepository = mockk()
    private val bookmarkRepository: BookmarkRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk()

    private fun createVm() = QuranViewModel(quranRepository, bookmarkRepository, preferencesManager, testDispatcherProvider)

    @Test
    fun `load surahs success`() = runTest(testDispatcher) {
        val surahs = listOf(
            Surah(1, 1, "الفاتحة", "Al-Fatiha", "Al-Fatiha", 7, "meccan")
        )
        every { quranRepository.getChapters(any()) } returns Result.Success(surahs)
        every { preferencesManager.translationLanguage } returns MutableStateFlow("system")
        every { preferencesManager.ayahFontSize } returns MutableStateFlow(22)
        every { preferencesManager.translationFontSize } returns MutableStateFlow(15)

        val vm = createVm()
        vm.loadSurahs()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.surahs.size)
        assertEquals("Al-Fatiha", vm.uiState.value.surahs[0].translatedName)
    }

    @Test
    fun `load surahs error sets error state`() = runTest(testDispatcher) {
        every { quranRepository.getChapters(any()) } returns Result.Error(AppError.Database("Failed", null))
        every { preferencesManager.translationLanguage } returns MutableStateFlow("system")
        every { preferencesManager.ayahFontSize } returns MutableStateFlow(22)
        every { preferencesManager.translationFontSize } returns MutableStateFlow(15)

        val vm = createVm()
        vm.loadSurahs()
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.surahs.size)
    }

    @Test
    fun `load ayahs success`() = runTest(testDispatcher) {
        val surahs = listOf(
            Surah(1, 1, "الفاتحة", "Al-Fatiha", "Al-Fatiha", 7, "meccan")
        )
        val ayahs = listOf(
            Ayah(1, 1, "text", null, null, "en trans", "id trans", 1, 1, false, null, false, false)
        )
        every { quranRepository.getChapters(any()) } returns Result.Success(surahs)
        every { quranRepository.getAyahsWithTranslation(1, any()) } returns Result.Success(ayahs)
        every { preferencesManager.translationLanguage } returns MutableStateFlow("system")
        every { preferencesManager.ayahFontSize } returns MutableStateFlow(22)
        every { preferencesManager.translationFontSize } returns MutableStateFlow(15)

        val vm = createVm()
        vm.loadSurahs()
        advanceUntilIdle()

        vm.loadAyahs(1)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.ayahs.size)
    }

    @Test
    fun `toggle bookmark calls repository`() = runTest(testDispatcher) {
        every { quranRepository.getChapters(any()) } returns Result.Success(emptyList())
        every { preferencesManager.translationLanguage } returns MutableStateFlow("system")
        every { preferencesManager.ayahFontSize } returns MutableStateFlow(22)
        every { preferencesManager.translationFontSize } returns MutableStateFlow(15)
        coEvery { bookmarkRepository.toggle(1, 1, "Al-Fatiha") } returns Result.Success(Unit)

        val vm = createVm()
        vm.toggleBookmark(1, 1, "Al-Fatiha")
        advanceUntilIdle()

        coVerify { bookmarkRepository.toggle(1, 1, "Al-Fatiha") }
    }

    @Test
    fun `clearAyahs resets ayah list`() = runTest(testDispatcher) {
        every { quranRepository.getChapters(any()) } returns Result.Success(emptyList())
        every { preferencesManager.translationLanguage } returns MutableStateFlow("system")
        every { preferencesManager.ayahFontSize } returns MutableStateFlow(22)
        every { preferencesManager.translationFontSize } returns MutableStateFlow(15)

        val vm = createVm()
        vm.loadSurahs()
        advanceUntilIdle()

        vm.clearAyahs()
        assertEquals(0, vm.uiState.value.ayahs.size)
        assertNull(vm.uiState.value.currentSurah)
    }
}
