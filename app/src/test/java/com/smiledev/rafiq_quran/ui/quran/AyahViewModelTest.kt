package com.smiledev.rafiq_quran.ui.quran

import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.data.remote.EQuranApiService
import com.smiledev.rafiq_quran.data.remote.IslamicAppApiService
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.domain.model.Surah
import com.smiledev.rafiq_quran.domain.repository.BookmarkRepository
import com.smiledev.rafiq_quran.domain.repository.QuranRepository
import com.smiledev.rafiq_quran.service.AudioPlayerController
import com.smiledev.rafiq_quran.service.PlaybackState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AyahViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val quranRepository: QuranRepository = mockk()
    private val bookmarkRepository: BookmarkRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private val audioPlayer: AudioPlayerController = mockk()
    private val islamicAppApi: IslamicAppApiService = mockk()
    private val equranApi: EQuranApiService = mockk()

    private val surah = Surah(1, 1, "الفاتحة", "Al-Fatihah", "Al-Fatihah", 7, "Meccan")
    private val ayahs = listOf(
        Ayah(sura = 1, aya = 1, text = "بِسْمِ", bismillah = null, translation = "In the name"),
        Ayah(sura = 1, aya = 2, text = "الْحَمْدُ", bismillah = null, translation = "Praise")
    )

    private fun createVm(playbackStateFlow: MutableStateFlow<PlaybackState> = MutableStateFlow(PlaybackState())): AyahViewModel {
        every { preferencesManager.translationLanguage } returns MutableStateFlow("system")
        every { preferencesManager.ayahFontSize } returns MutableStateFlow(22)
        every { preferencesManager.translationFontSize } returns MutableStateFlow(15)
        every { preferencesManager.lastReadSura } returns MutableStateFlow(0)
        every { preferencesManager.lastReadAya } returns MutableStateFlow(0)
        every { quranRepository.getChapters(any()) } returns Result.Success(listOf(surah))
        every { bookmarkRepository.observeAll() } returns MutableStateFlow(emptyList())
        every { audioPlayer.playbackState } returns playbackStateFlow
        return AyahViewModel(
            quranRepository,
            bookmarkRepository,
            preferencesManager,
            audioPlayer,
            islamicAppApi,
            equranApi,
            testDispatcherProvider
        )
    }

    @Test
    fun `toggleAyahAudio passes url title artist and completion`() = runTest(testDispatcher) {
        every { quranRepository.getAyahsWithTranslation(1, "en") } returns Result.Success(ayahs)
        every { audioPlayer.playAyah(any(), any(), any(), any()) } returns Unit

        val vm = createVm()
        vm.loadAyahs(1)
        advanceUntilIdle()
        vm.toggleAyahAudio(1)
        advanceUntilIdle()

        verify {
            audioPlayer.playAyah(
                "https://everyayah.com/data/Alafasy_128kbps/001001.mp3",
                "Al-Fatihah · Ayah 1",
                "Alafasy",
                any()
            )
        }
        assertEquals(1, vm.uiState.value.currentPlayingAyah)
    }

    @Test
    fun `playAyah completion advances to next ayah then stops`() = runTest(testDispatcher) {
        every { quranRepository.getAyahsWithTranslation(1, "en") } returns Result.Success(ayahs)
        val completions = mutableListOf<() -> Unit>()
        every { audioPlayer.playAyah(any(), any(), any(), any()) } answers {
            completions.add(arg(3))
            Unit
        }

        val vm = createVm()
        vm.loadAyahs(1)
        advanceUntilIdle()
        vm.toggleAyahAudio(1)
        advanceUntilIdle()

        assertEquals(1, completions.size)
        assertEquals(1, vm.uiState.value.currentPlayingAyah)

        completions[0]()
        advanceUntilIdle()
        assertEquals(2, completions.size)
        assertEquals(2, vm.uiState.value.currentPlayingAyah)

        completions[1]()
        advanceUntilIdle()
        assertEquals(false, vm.uiState.value.isPlaying)
        assertEquals(null, vm.uiState.value.currentPlayingAyah)
    }

    @Test
    fun `seekTo delegates to audio player`() = runTest(testDispatcher) {
        every { audioPlayer.seekTo(any()) } returns Unit

        val vm = createVm()
        vm.seekTo(15000)
        advanceUntilIdle()

        verify { audioPlayer.seekTo(15000L) }
    }

    @Test
    fun `playback state from controller updates ui state`() = runTest(testDispatcher) {
        val playbackStateFlow = MutableStateFlow(PlaybackState())
        val vm = createVm(playbackStateFlow)

        playbackStateFlow.value = PlaybackState(positionMs = 5000, durationMs = 60000, isPlaying = true)
        advanceUntilIdle()

        assertEquals(5000L, vm.uiState.value.positionMs)
        assertEquals(60000L, vm.uiState.value.durationMs)
        assertEquals(true, vm.uiState.value.isPlaying)
    }
}
