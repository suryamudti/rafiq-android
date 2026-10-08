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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuranViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val quranRepository: QuranRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)

    private val lastReadSuraFlow = MutableStateFlow(0)
    private val lastReadAyaFlow = MutableStateFlow(0)

    private val surah1 = Surah(1, 1, "الفاتحة", "Al-Fatihah", "The Opening", 7, "makkah")
    private val surah2 = Surah(2, 2, "البقرة", "Al-Baqarah", "The Cow", 286, "madinah")
    private val surah36 = Surah(36, 36, "يس", "Ya-Sin", "Ya Sin", 83, "meccan")

    private val ayah = Ayah(
        sura = 1,
        aya = 1,
        text = "بِسْمِ ٱللَّهِ",
        bismillah = null,
        translation = "In the name of Allah"
    )

    private fun createVm(): QuranViewModel {
        every { preferencesManager.translationLanguage } returns MutableStateFlow("system")
        every { preferencesManager.lastReadSura } returns lastReadSuraFlow
        every { preferencesManager.lastReadAya } returns lastReadAyaFlow
        every { quranRepository.getChapters(any()) } returns Result.Success(emptyList())
        return QuranViewModel(quranRepository, preferencesManager, testDispatcherProvider)
    }

    @Test
    fun `load surahs success`() = runTest(testDispatcher) {
        val vm = createVm()
        every { quranRepository.getChapters(any()) } returns Result.Success(listOf(surah1))
        vm.loadSurahs()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.surahs.size)
        assertEquals("The Opening", vm.uiState.value.surahs[0].translatedName)
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
    fun `revelation filter correctly filters surahs and counts`() = runTest(testDispatcher) {
        val vm = createVm()
        every { quranRepository.getChapters(any()) } returns Result.Success(listOf(surah1, surah2, surah36))
        vm.loadSurahs()
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.surahs.size)
        assertEquals(2, vm.uiState.value.meccanCount) // surah1 (makkah), surah36 (meccan)
        assertEquals(1, vm.uiState.value.medinanCount) // surah2 (madinah)

        // ALL
        assertEquals(3, vm.uiState.value.filteredSurahs.size)

        // MECCAN
        vm.setRevelationFilter(RevelationFilter.MECCAN)
        assertEquals(2, vm.uiState.value.filteredSurahs.size)
        assertTrue(vm.uiState.value.filteredSurahs.contains(surah1))
        assertTrue(vm.uiState.value.filteredSurahs.contains(surah36))

        // MEDINAN
        vm.setRevelationFilter(RevelationFilter.MEDINAN)
        assertEquals(1, vm.uiState.value.filteredSurahs.size)
        assertEquals(surah2, vm.uiState.value.filteredSurahs[0])
    }

    @Test
    fun `filteredSurahs matches by chapter number, name, arabic, or translated name`() = runTest(testDispatcher) {
        every { quranRepository.searchAyahs(any(), any(), any()) } returns Result.Success(emptyList())
        val vm = createVm()
        every { quranRepository.getChapters(any()) } returns Result.Success(listOf(surah1, surah2, surah36))
        vm.loadSurahs()
        advanceUntilIdle()

        // Match by number
        vm.search("36")
        assertEquals(1, vm.uiState.value.filteredSurahs.size)
        assertEquals(36, vm.uiState.value.filteredSurahs[0].chapterNumber)

        // Match by simple name
        vm.search("baqarah")
        assertEquals(1, vm.uiState.value.filteredSurahs.size)
        assertEquals("Al-Baqarah", vm.uiState.value.filteredSurahs[0].nameSimple)

        // Match by arabic
        vm.search("الفاتحة")
        assertEquals(1, vm.uiState.value.filteredSurahs.size)
        assertEquals(surah1, vm.uiState.value.filteredSurahs[0])

        // Match by translated name
        vm.search("opening")
        assertEquals(1, vm.uiState.value.filteredSurahs.size)
        assertEquals(surah1, vm.uiState.value.filteredSurahs[0])
    }

    @Test
    fun `lastRead updates correctly and computes lastReadSurah`() = runTest(testDispatcher) {
        val vm = createVm()
        every { quranRepository.getChapters(any()) } returns Result.Success(listOf(surah1, surah2))
        vm.loadSurahs()
        advanceUntilIdle()

        assertNull(vm.uiState.value.lastReadSurah)

        lastReadSuraFlow.value = 2
        lastReadAyaFlow.value = 255
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.lastReadSura)
        assertEquals(255, vm.uiState.value.lastReadAya)
        assertNotNull(vm.uiState.value.lastReadSurah)
        assertEquals(surah2, vm.uiState.value.lastReadSurah)
    }

    @Test
    fun `searchTab switching and clearSearch`() = runTest(testDispatcher) {
        every { quranRepository.searchAyahs(any(), any(), any()) } returns Result.Success(emptyList())
        val vm = createVm()
        vm.setSearchTab(QuranSearchTab.AYAHS)
        assertEquals(QuranSearchTab.AYAHS, vm.uiState.value.searchTab)

        vm.search("Allah")
        assertEquals("Allah", vm.uiState.value.searchQuery)

        vm.clearSearch()
        assertEquals("", vm.uiState.value.searchQuery)
        assertTrue(vm.uiState.value.searchResults.isEmpty())
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
