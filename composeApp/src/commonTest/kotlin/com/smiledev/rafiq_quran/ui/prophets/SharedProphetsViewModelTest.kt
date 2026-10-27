package com.smiledev.rafiq_quran.ui.prophets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedProphetsViewModelTest {

    @Test
    fun initialState_loadsDefaultProphets() {
        val viewModel = SharedProphetsViewModel()
        val state = viewModel.uiState.value

        assertTrue(state.prophets.isNotEmpty())
        assertEquals("", state.searchQuery)
        assertFalse(state.showFavoritesOnly)
        assertTrue(state.favoriteIds.isEmpty())
        assertNull(state.selectedProphet)
    }

    @Test
    fun selectProphet_updatesSelectedAndClears() {
        val viewModel = SharedProphetsViewModel()
        val firstProphet = viewModel.uiState.value.prophets.first()

        viewModel.selectProphet(firstProphet)
        assertEquals(firstProphet, viewModel.uiState.value.selectedProphet)

        viewModel.selectProphet(null)
        assertNull(viewModel.uiState.value.selectedProphet)
    }

    @Test
    fun toggleFavorite_addsAndRemovesFavoriteId() {
        val viewModel = SharedProphetsViewModel()
        viewModel.toggleFavorite(1)
        assertTrue(1 in viewModel.uiState.value.favoriteIds)

        viewModel.toggleFavorite(2)
        assertEquals(setOf(1, 2), viewModel.uiState.value.favoriteIds)

        viewModel.toggleFavorite(1)
        assertEquals(setOf(2), viewModel.uiState.value.favoriteIds)
    }

    @Test
    fun setStoryFontSize_clampsToBounds() {
        val viewModel = SharedProphetsViewModel()
        viewModel.setStoryFontSize(20)
        assertEquals(20, viewModel.uiState.value.storyFontSize)

        viewModel.setStoryFontSize(8) // below min 12
        assertEquals(12, viewModel.uiState.value.storyFontSize)

        viewModel.setStoryFontSize(35) // above max 28
        assertEquals(28, viewModel.uiState.value.storyFontSize)
    }

    @Test
    fun searchFiltering_matchesNameAndId() {
        val viewModel = SharedProphetsViewModel()
        viewModel.onSearchQueryChange("Musa")

        val filtered = viewModel.getFilteredProphets()
        assertEquals(1, filtered.size)
        assertEquals("Musa (Moses)", filtered.first().nameEn)

        viewModel.onSearchQueryChange("1")
        val byId = viewModel.getFilteredProphets()
        assertTrue(byId.any { it.id == 1 })
    }

    @Test
    fun toggleFavoritesOnly_filtersOnlyFavorites() {
        val viewModel = SharedProphetsViewModel()
        viewModel.toggleFavorite(3) // Ibrahim
        viewModel.toggleFavoritesOnly()

        val filtered = viewModel.getFilteredProphets()
        assertEquals(1, filtered.size)
        assertEquals(3, filtered.first().id)
    }
}
