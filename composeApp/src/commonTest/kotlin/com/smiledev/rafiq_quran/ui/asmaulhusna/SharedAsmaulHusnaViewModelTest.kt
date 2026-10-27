package com.smiledev.rafiq_quran.ui.asmaulhusna

import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.AsmaulHusna
import com.smiledev.rafiq_quran.domain.repository.AsmaulHusnaRepository
import com.smiledev.rafiq_quran.domain.usecase.GetAsmaulHusnaUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SharedAsmaulHusnaViewModelTest {

    @Test
    fun initialState_loadsDefaultNames() {
        val viewModel = SharedAsmaulHusnaViewModel()
        val state = viewModel.uiState.value

        assertTrue(state.names.isNotEmpty())
        assertEquals("", state.searchQuery)
        assertTrue(state.isGridView)
        assertFalse(state.showFavoritesOnly)
        assertTrue(state.favoriteIds.isEmpty())
    }

    @Test
    fun toggleFavoritesOnly_togglesState() {
        val viewModel = SharedAsmaulHusnaViewModel()
        assertFalse(viewModel.uiState.value.showFavoritesOnly)

        viewModel.toggleFavoritesOnly()
        assertTrue(viewModel.uiState.value.showFavoritesOnly)

        viewModel.toggleFavoritesOnly()
        assertFalse(viewModel.uiState.value.showFavoritesOnly)
    }

    @Test
    fun toggleGridView_togglesGridAndList() {
        val viewModel = SharedAsmaulHusnaViewModel()
        assertTrue(viewModel.uiState.value.isGridView)

        viewModel.toggleGridView()
        assertFalse(viewModel.uiState.value.isGridView)
    }

    @Test
    fun toggleFavorite_addsAndRemovesId() {
        val viewModel = SharedAsmaulHusnaViewModel()
        viewModel.toggleFavorite(1)
        assertTrue(1 in viewModel.uiState.value.favoriteIds)

        viewModel.toggleFavorite(2)
        assertEquals(setOf(1, 2), viewModel.uiState.value.favoriteIds)

        viewModel.toggleFavorite(1)
        assertEquals(setOf(2), viewModel.uiState.value.favoriteIds)
    }

    @Test
    fun dhikrCount_incrementsAndResets() {
        val viewModel = SharedAsmaulHusnaViewModel()
        assertEquals(0, viewModel.uiState.value.dhikrCount)

        viewModel.incrementDhikr()
        viewModel.incrementDhikr()
        assertEquals(2, viewModel.uiState.value.dhikrCount)

        viewModel.resetDhikr()
        assertEquals(0, viewModel.uiState.value.dhikrCount)
    }

    @Test
    fun searchFiltering_matchesTransliterationAndId() {
        val viewModel = SharedAsmaulHusnaViewModel()
        viewModel.onSearchQueryChange("Rahman")

        val filtered = viewModel.getFilteredNames()
        assertEquals(1, filtered.size)
        assertEquals("Ar-Rahman", filtered.first().transliteration)

        viewModel.onSearchQueryChange("1")
        val byId = viewModel.getFilteredNames()
        assertTrue(byId.any { it.id == 1 })
    }

    @Test
    fun selectRandomName_picksValidName() {
        val viewModel = SharedAsmaulHusnaViewModel()
        viewModel.selectRandomName()

        assertNotNull(viewModel.uiState.value.selectedName)
    }
}
