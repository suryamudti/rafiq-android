package com.smiledev.rafiq.ui.hadith

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HadithBooksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val repository: HadithRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)

    private fun books() = listOf(
        HadithBook("bukhari.1", "bukhari", 1, "كتاب بدء الوحي", "Revelation", "Permulaan Wahyu")
    )

    private fun createVm(): HadithBooksViewModel {
        every { preferencesManager.translationLanguage } returns MutableStateFlow("system")
        return HadithBooksViewModel(repository, preferencesManager, testDispatcherProvider)
    }

    @Test
    fun `load books success`() = runTest(testDispatcher) {
        every { repository.getBooks() } returns Result.Success(books())

        val vm = createVm()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.books.size)
        assertEquals(false, vm.uiState.value.isLoading)
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun `load books error`() = runTest(testDispatcher) {
        every { repository.getBooks() } returns Result.Error(AppError.Database("fail", null))

        val vm = createVm()
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.books.size)
        assertEquals(false, vm.uiState.value.isLoading)
        assertEquals(true, vm.uiState.value.error != null)
    }

    @Test
    fun `resolvedLanguage uses pref when set`() = runTest(testDispatcher) {
        every { repository.getBooks() } returns Result.Success(books())
        val vm = createVm()
        every { preferencesManager.translationLanguage } returns MutableStateFlow("id")
        advanceUntilIdle()

        assertEquals("id", vm.resolvedLanguage())
    }
}
