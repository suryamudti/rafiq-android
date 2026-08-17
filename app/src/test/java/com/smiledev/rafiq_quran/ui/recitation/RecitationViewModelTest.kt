package com.smiledev.rafiq.ui.recitation

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.Reciter
import com.smiledev.rafiq.domain.repository.QuranRepository
import com.smiledev.rafiq.domain.repository.ReciterRepository
import com.smiledev.rafiq.service.AudioPlayerController
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecitationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val reciterRepository: ReciterRepository = mockk()
    private val quranRepository: QuranRepository = mockk()
    private val audioPlayer: AudioPlayerController = mockk()

    @Test
    fun `load reciters success`() = runTest(testDispatcher) {
        val reciters = listOf(
            Reciter(1, "Abdul Basit", "عبد الباسط", "Mujawwad", "Egypt", "abdul_basit")
        )
        every { reciterRepository.getReciters() } returns Result.Success(reciters)

        val vm = RecitationViewModel(reciterRepository, quranRepository, audioPlayer, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.reciters.size)
    }

    @Test
    fun `load reciters error`() = runTest(testDispatcher) {
        every { reciterRepository.getReciters() } returns Result.Error(AppError.Database("fail", null))

        val vm = RecitationViewModel(reciterRepository, quranRepository, audioPlayer, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.reciters.size)
    }
}
