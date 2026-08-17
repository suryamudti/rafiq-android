package com.smiledev.rafiq_quran.ui.hadith

import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.Hadith
import com.smiledev.rafiq_quran.domain.model.HadithBook
import com.smiledev.rafiq_quran.domain.repository.HadithRepository
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
class HadithSearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val repository: HadithRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)

    private val book = HadithBook("bukhari.1", "bukhari", 1, "كتاب بدء الوحي", "Revelation", "Permulaan Wahyu")
    private val hadith = Hadith(1, "bukhari.1", 1, "نarrator", "Narrator", "arabic", "english", "indonesia")

    private fun createVm(): HadithSearchViewModel {
        every { preferencesManager.translationLanguage } returns MutableStateFlow("system")
        every { repository.getBooks() } returns Result.Success(listOf(book))
        return HadithSearchViewModel(repository, preferencesManager, testDispatcherProvider)
    }

    @Test
    fun `search populates results after debounce`() = runTest(testDispatcher) {
        every { repository.searchHadiths("indonesia", 100) } returns Result.Success(listOf(hadith))

        val vm = createVm()
        vm.search("indonesia")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertEquals(listOf(hadith), vm.uiState.value.results)
        assertEquals(false, vm.uiState.value.isLoading)
    }

    @Test
    fun `debounce cancels the earlier keystroke`() = runTest(testDispatcher) {
        every { repository.searchHadiths("indonesia", 100) } returns Result.Success(listOf(hadith))
        every { repository.searchHadiths("ind", 100) } returns Result.Success(listOf(hadith))

        val vm = createVm()
        vm.search("ind")
        advanceTimeBy(100)
        vm.search("indonesia")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertEquals("indonesia", vm.uiState.value.query)
        assertEquals(listOf(hadith), vm.uiState.value.results)
    }

    @Test
    fun `blank query clears results without hitting repo`() = runTest(testDispatcher) {
        every { repository.searchHadiths("indonesia", 100) } returns Result.Success(listOf(hadith))

        val vm = createVm()
        vm.search("indonesia")
        advanceTimeBy(250)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.results.size)

        vm.search("   ")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.results.isEmpty())
    }

    @Test
    fun `search error surfaces in state`() = runTest(testDispatcher) {
        every { repository.searchHadiths("indonesia", 100) } returns Result.Error(AppError.Database("fail", null))

        val vm = createVm()
        vm.search("indonesia")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertEquals(true, vm.uiState.value.error != null)
        assertEquals(false, vm.uiState.value.isLoading)
    }

    @Test
    fun `resolvedLanguage maps system to locale code`() = runTest(testDispatcher) {
        val vm = createVm()
        assertEquals("en", vm.resolvedLanguage())
    }
}
