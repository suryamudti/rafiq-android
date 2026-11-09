package com.smiledev.rafiq_quran.ui.recitation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedRecitationViewModelTest {

    @Test
    fun initialState_hasRecitersAndSurahs() {
        val viewModel = SharedRecitationViewModel()
        val state = viewModel.uiState.value

        assertTrue(state.reciters.isNotEmpty())
        assertTrue(state.surahs.isNotEmpty())
        assertEquals("Mishary Rashid Alafasy", state.selectedReciter.nameEn)
        assertEquals("Al-Fatihah", state.currentSurah?.nameSimple)
    }

    @Test
    fun selectReciter_updatesState() {
        val viewModel = SharedRecitationViewModel()
        val secondReciter = viewModel.uiState.value.reciters[1]

        viewModel.selectReciter(secondReciter)
        assertEquals(secondReciter.id, viewModel.uiState.value.selectedReciter.id)
    }

    @Test
    fun playSurah_startsPlaybackAtBeginning() {
        val viewModel = SharedRecitationViewModel()
        val yasin = viewModel.uiState.value.surahs.first { it.nameSimple == "Yasin" }

        viewModel.playSurah(yasin)
        val state = viewModel.uiState.value

        assertEquals("Yasin", state.currentSurah?.nameSimple)
        assertTrue(state.isPlaying)
        assertEquals(0L, state.positionMs)
        assertTrue(state.durationMs > 0L)
    }

    @Test
    fun togglePlayPause_invertsPlayback() {
        val viewModel = SharedRecitationViewModel()
        val wasPlaying = viewModel.uiState.value.isPlaying

        viewModel.togglePlayPause()
        assertEquals(!wasPlaying, viewModel.uiState.value.isPlaying)

        viewModel.togglePlayPause()
        assertEquals(wasPlaying, viewModel.uiState.value.isPlaying)
    }

    @Test
    fun seekTo_clampsWithinDuration() {
        val viewModel = SharedRecitationViewModel()
        val duration = viewModel.uiState.value.durationMs

        viewModel.seekTo(duration + 50000L)
        assertEquals(duration, viewModel.uiState.value.positionMs)

        viewModel.seekTo(-5000L)
        assertEquals(0L, viewModel.uiState.value.positionMs)

        viewModel.seekTo(30000L)
        assertEquals(30000L, viewModel.uiState.value.positionMs)
    }

    @Test
    fun playNextAndPrevious_traverseSurahs() {
        val viewModel = SharedRecitationViewModel()

        viewModel.playNext()
        assertEquals("Al-Baqarah", viewModel.uiState.value.currentSurah?.nameSimple)

        viewModel.playPrevious()
        assertEquals("Al-Fatihah", viewModel.uiState.value.currentSurah?.nameSimple)
    }

    @Test
    fun updateSearchQuery_filtersSurahs() {
        val viewModel = SharedRecitationViewModel()

        viewModel.updateSearchQuery("Mulk")
        assertEquals(1, viewModel.uiState.value.filteredSurahs.size)
        assertEquals("Al-Mulk", viewModel.uiState.value.filteredSurahs.first().nameSimple)
    }
}
