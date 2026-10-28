package com.smiledev.rafiq_quran.ui.hadith

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedHadithViewModelTest {

    @Test
    fun initialState_loadsBooksTopicsAndHadiths() {
        val viewModel = SharedHadithViewModel()
        val state = viewModel.uiState.value

        assertTrue(state.books.isNotEmpty())
        assertTrue(state.topics.isNotEmpty())
        assertTrue(state.hadiths.isNotEmpty())
        assertNull(state.selectedBook)
        assertNull(state.selectedTopic)
        assertNull(state.selectedHadith)
        assertNotNull(state.hadithOfTheDay)
        assertTrue(state.favoriteIds.isEmpty())
        assertFalse(state.showFavoritesOnly)
    }

    @Test
    fun selectBook_togglesSelection() {
        val viewModel = SharedHadithViewModel()
        val book = viewModel.uiState.value.books.first()

        viewModel.selectBook(book)
        assertEquals(book, viewModel.uiState.value.selectedBook)

        // Clicking same book again clears selection
        viewModel.selectBook(book)
        assertNull(viewModel.uiState.value.selectedBook)
    }

    @Test
    fun selectTopic_clearsSelectedBook() {
        val viewModel = SharedHadithViewModel()
        val book = viewModel.uiState.value.books.first()
        val topic = viewModel.uiState.value.topics.first()

        viewModel.selectBook(book)
        assertEquals(book, viewModel.uiState.value.selectedBook)

        viewModel.selectTopic(topic)
        assertEquals(topic, viewModel.uiState.value.selectedTopic)
        assertNull(viewModel.uiState.value.selectedBook)
    }

    @Test
    fun toggleFavorite_addsAndRemovesId() {
        val viewModel = SharedHadithViewModel()
        viewModel.toggleFavorite(1)
        assertTrue(1 in viewModel.uiState.value.favoriteIds)

        viewModel.toggleFavorite(2)
        assertEquals(setOf(1, 2), viewModel.uiState.value.favoriteIds)

        viewModel.toggleFavorite(1)
        assertEquals(setOf(2), viewModel.uiState.value.favoriteIds)
    }

    @Test
    fun setFontSize_clampsWithinSafeRange() {
        val viewModel = SharedHadithViewModel()
        viewModel.setFontSize(22)
        assertEquals(22, viewModel.uiState.value.fontSize)

        viewModel.setFontSize(8) // below min 14
        assertEquals(14, viewModel.uiState.value.fontSize)

        viewModel.setFontSize(36) // above max 28
        assertEquals(28, viewModel.uiState.value.fontSize)
    }

    @Test
    fun getFilteredHadiths_matchesSearchQuery() {
        val viewModel = SharedHadithViewModel()
        viewModel.setSearchQuery("intentions")

        val filtered = viewModel.getFilteredHadiths(viewModel.uiState.value)
        assertTrue(filtered.isNotEmpty())
        assertTrue(filtered.any { it.textEn.contains("intentions", ignoreCase = true) })
    }

    @Test
    fun getFilteredHadiths_filtersByFavorites() {
        val viewModel = SharedHadithViewModel()
        viewModel.toggleFavorite(2)
        viewModel.toggleFavoritesOnly()

        val filtered = viewModel.getFilteredHadiths(viewModel.uiState.value)
        assertEquals(1, filtered.size)
        assertEquals(2, filtered.first().id)
    }
}
