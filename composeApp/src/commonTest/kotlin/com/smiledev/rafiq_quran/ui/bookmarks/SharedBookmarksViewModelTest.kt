package com.smiledev.rafiq_quran.ui.bookmarks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedBookmarksViewModelTest {

    @Test
    fun initialState_hasDefaultBookmarks() {
        val viewModel = SharedBookmarksViewModel()
        val state = viewModel.uiState.value

        assertEquals(5, state.bookmarks.size)
        assertEquals(5, state.filteredBookmarks.size)
    }

    @Test
    fun updateSearchQuery_filtersBookmarksCorrectly() {
        val viewModel = SharedBookmarksViewModel()

        viewModel.updateSearchQuery("Baqarah")
        assertEquals(1, viewModel.uiState.value.filteredBookmarks.size)
        assertEquals("Al-Baqarah", viewModel.uiState.value.filteredBookmarks.first().suraName)

        viewModel.updateSearchQuery("36")
        assertEquals(1, viewModel.uiState.value.filteredBookmarks.size)
        assertEquals("Yasin", viewModel.uiState.value.filteredBookmarks.first().suraName)

        viewModel.updateSearchQuery("nonexistent")
        assertEquals(0, viewModel.uiState.value.filteredBookmarks.size)
    }

    @Test
    fun deleteBookmark_removesSpecifiedAyah() {
        val viewModel = SharedBookmarksViewModel()

        viewModel.deleteBookmark(1, 1)
        val state = viewModel.uiState.value

        assertEquals(4, state.bookmarks.size)
        assertTrue(state.bookmarks.none { it.sura == 1 && it.aya == 1 })
    }

    @Test
    fun clearAllBookmarks_emptiesList() {
        val viewModel = SharedBookmarksViewModel()

        viewModel.clearAllBookmarks()
        assertTrue(viewModel.uiState.value.bookmarks.isEmpty())
        assertTrue(viewModel.uiState.value.filteredBookmarks.isEmpty())
    }
}
