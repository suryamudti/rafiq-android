package com.smiledev.rafiq.ui.hadith

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.Hadith
import com.smiledev.rafiq.domain.model.HadithBook
import com.smiledev.rafiq.domain.repository.HadithRepository
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
class HadithListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val repository: HadithRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)

    private val book = HadithBook("bukhari.1", "bukhari", 1, "كتاب بدء الوحي", "Revelation", "Permulaan Wahyu")
    private val hadith = Hadith(1, "bukhari.1", 1, "نarrator", "Narrator", "arabic", "english", "indonesia")

    private fun createVm(): HadithListViewModel {
        every { preferencesManager.translationLanguage } returns MutableStateFlow("system")
        return HadithListViewModel(repository, preferencesManager, testDispatcherProvider)
    }

    @Test
    fun `load populates book and hadiths`() = runTest(testDispatcher) {
        every { repository.getBooks() } returns Result.Success(listOf(book))
        every { repository.getHadithsByBook("bukhari.1") } returns Result.Success(listOf(hadith))

        val vm = createVm()
        vm.load("bukhari.1")
        advanceUntilIdle()

        assertEquals("bukhari.1", vm.uiState.value.book?.id)
        assertEquals(1, vm.uiState.value.hadiths.size)
        assertEquals(false, vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `load error surfaces in state`() = runTest(testDispatcher) {
        every { repository.getBooks() } returns Result.Success(listOf(book))
        every { repository.getHadithsByBook("bukhari.1") } returns Result.Error(AppError.Database("fail", null))

        val vm = createVm()
        vm.load("bukhari.1")
        advanceUntilIdle()

        assertEquals(true, vm.uiState.value.error != null)
        assertEquals(false, vm.uiState.value.isLoading)
    }

    @Test
    fun `resolvedLanguage maps system to locale code`() = runTest(testDispatcher) {
        every { repository.getBooks() } returns Result.Success(emptyList())
        every { repository.getHadithsByBook(any()) } returns Result.Success(emptyList())

        val vm = createVm()
        vm.load("bukhari.1")
        advanceUntilIdle()

        assertEquals("en", vm.resolvedLanguage()) // JVM default locale; adjust if locale is id
    }
}