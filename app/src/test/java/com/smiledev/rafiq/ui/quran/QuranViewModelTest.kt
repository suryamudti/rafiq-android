package com.smiledev.rafiq.ui.quran

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.Surah
import com.smiledev.rafiq.domain.repository.QuranRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuranViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val quranRepository: QuranRepository = mockk()

    private fun createVm() = QuranViewModel(quranRepository, testDispatcherProvider)

    @Test
    fun `load surahs success`() = runTest(testDispatcher) {
        val surahs = listOf(
            Surah(1, 1, "الفاتحة", "Al-Fatiha", "Al-Fatiha", 7, "meccan")
        )
        every { quranRepository.getChapters(any()) } returns Result.Success(surahs)

        val vm = createVm()
        vm.loadSurahs()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.surahs.size)
        assertEquals("Al-Fatiha", vm.uiState.value.surahs[0].translatedName)
    }

    @Test
    fun `load surahs error sets error state`() = runTest(testDispatcher) {
        every { quranRepository.getChapters(any()) } returns Result.Error(AppError.Database("Failed", null))

        val vm = createVm()
        vm.loadSurahs()
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.surahs.size)
    }
}
