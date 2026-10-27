package com.smiledev.rafiq_quran.ui.quran

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedQuranViewModelTest {

    @Test
    fun initialState_hasDefaultSurahsAndNoSelection() {
        val viewModel = SharedQuranViewModel()
        val state = viewModel.uiState.value

        assertTrue(state.surahs.isNotEmpty())
        assertNull(state.selectedSurah)
        assertTrue(state.ayahs.isEmpty())
        assertTrue(state.showTranslation)
        assertEquals(26, state.arabicFontSizeSp)
    }

    @Test
    fun filterSurahs_matchesSearchQuery() {
        val viewModel = SharedQuranViewModel()
        viewModel.onSearchQueryChange("Fatihah")

        val filtered = viewModel.getFilteredSurahs()
        assertEquals(1, filtered.size)
        assertEquals("Al-Fatihah", filtered.first().nameSimple)
    }

    @Test
    fun filterSurahs_byRevelationPlace() {
        val viewModel = SharedQuranViewModel()
        viewModel.selectRevelationFilter("madinah")

        val filtered = viewModel.getFilteredSurahs()
        assertTrue(filtered.all { it.revelationPlace.equals("madinah", ignoreCase = true) })
    }

    @Test
    fun selectSurah_loadsAyahsForSelectedSurah() {
        val viewModel = SharedQuranViewModel()
        val surah = viewModel.uiState.value.surahs.first { it.chapterNumber == 1 }

        viewModel.selectSurah(surah)
        val state = viewModel.uiState.value
        assertEquals(1, state.selectedSurah?.chapterNumber)
        assertEquals(7, state.ayahs.size)
        assertEquals("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", state.ayahs.first().text)
    }

    @Test
    fun clearSurahSelection_resetsAyahs() {
        val viewModel = SharedQuranViewModel()
        val surah = viewModel.uiState.value.surahs.first()
        viewModel.selectSurah(surah)
        assertNotNull(viewModel.uiState.value.selectedSurah)

        viewModel.selectSurah(null)
        assertNull(viewModel.uiState.value.selectedSurah)
        assertTrue(viewModel.uiState.value.ayahs.isEmpty())
    }

    @Test
    fun toggleTranslation_updatesState() {
        val viewModel = SharedQuranViewModel()
        assertTrue(viewModel.uiState.value.showTranslation)

        viewModel.toggleTranslation()
        assertFalse(viewModel.uiState.value.showTranslation)
    }

    @Test
    fun fontScaling_adjustsWithinClampedRange() {
        val viewModel = SharedQuranViewModel()
        assertEquals(26, viewModel.uiState.value.arabicFontSizeSp)

        viewModel.increaseArabicFontSize()
        assertEquals(28, viewModel.uiState.value.arabicFontSizeSp)

        viewModel.decreaseArabicFontSize()
        viewModel.decreaseArabicFontSize()
        assertEquals(24, viewModel.uiState.value.arabicFontSizeSp)
    }
}
