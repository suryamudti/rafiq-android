package com.smiledev.rafiq_quran.ui.sources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedSourcesViewModelTest {

    @Test
    fun initialState_hasAllSources() {
        val viewModel = SharedSourcesViewModel()
        val state = viewModel.uiState.value

        assertEquals(SourceCategory.ALL, state.selectedCategory)
        assertEquals(DEFAULT_SOURCES_CATALOG.size, state.filteredSources.size)
    }

    @Test
    fun selectCategory_filtersSources() {
        val viewModel = SharedSourcesViewModel()

        viewModel.selectCategory(SourceCategory.QURAN)
        val state = viewModel.uiState.value

        assertEquals(SourceCategory.QURAN, state.selectedCategory)
        assertTrue(state.filteredSources.all { it.category == SourceCategory.QURAN })
        assertTrue(state.filteredSources.isNotEmpty())

        viewModel.selectCategory(SourceCategory.LIBRARIES)
        val libState = viewModel.uiState.value
        assertTrue(libState.filteredSources.all { it.category == SourceCategory.LIBRARIES })
    }
}
