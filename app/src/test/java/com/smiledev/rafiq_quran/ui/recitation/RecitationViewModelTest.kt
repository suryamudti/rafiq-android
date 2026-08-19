package com.smiledev.rafiq_quran.ui.recitation

import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.Reciter
import com.smiledev.rafiq_quran.domain.model.Surah
import com.smiledev.rafiq_quran.domain.repository.QuranRepository
import com.smiledev.rafiq_quran.domain.repository.ReciterRepository
import com.smiledev.rafiq_quran.service.AudioPlayerController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
            Reciter(1, "Abdul Basit", "عبد الباسط", "Mujawwad", "Egypt", "https://download.quranicaudio.com/quran/abdul_basit_murattal")
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

    @Test
    fun `playSurah builds url from audio base`() = runTest(testDispatcher) {
        val reciter = Reciter(1, "Abdul Basit", "عبد الباسط", "Mujawwad", "Egypt", "https://download.quranicaudio.com/quran/abdul_basit_murattal")
        val surah = Surah(1, 1, "Al-Fatihah", "Al-Fatihah", "الفاتحة", 7, "Meccan")
        every { reciterRepository.getReciters() } returns Result.Success(listOf(reciter))
        every { quranRepository.getChapters() } returns Result.Success(listOf(surah))
        every { audioPlayer.play(any()) } returns Unit

        val vm = RecitationViewModel(reciterRepository, quranRepository, audioPlayer, testDispatcherProvider)
        advanceUntilIdle()
        vm.selectReciter(reciter)
        advanceUntilIdle()
        vm.playSurah(surah)
        advanceUntilIdle()

        verify { audioPlayer.play("https://download.quranicaudio.com/quran/abdul_basit_murattal/001.mp3") }
    }

    @Test
    fun `playSurah pads surah number to three digits`() = runTest(testDispatcher) {
        val reciter = Reciter(2, "Khalid Al-Jalil", "خالد الجليل", "Murattal", "Saudi Arabia", "https://server10.mp3quran.net/jleel")
        val surah = Surah(114, 114, "An-Nas", "An-Nas", "الناس", 6, "Meccan")
        every { reciterRepository.getReciters() } returns Result.Success(listOf(reciter))
        every { quranRepository.getChapters() } returns Result.Success(listOf(surah))
        every { audioPlayer.play(any()) } returns Unit

        val vm = RecitationViewModel(reciterRepository, quranRepository, audioPlayer, testDispatcherProvider)
        advanceUntilIdle()
        vm.selectReciter(reciter)
        advanceUntilIdle()
        vm.playSurah(surah)
        advanceUntilIdle()

        verify { audioPlayer.play("https://server10.mp3quran.net/jleel/114.mp3") }
    }
}
