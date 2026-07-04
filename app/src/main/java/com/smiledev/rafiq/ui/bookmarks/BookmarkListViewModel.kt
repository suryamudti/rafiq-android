package com.smiledev.rafiq.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.data.local.BookmarkDao
import com.smiledev.rafiq.data.local.BookmarkEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookmarkListUiState(
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class BookmarkListViewModel @Inject constructor(
    private val bookmarkDao: BookmarkDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarkListUiState())
    val uiState: StateFlow<BookmarkListUiState> = _uiState

    init { load() }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val bookmarks = bookmarkDao.getAllBookmarks()
            _uiState.value = _uiState.value.copy(bookmarks = bookmarks, isLoading = false)
        }
    }

    fun delete(sura: Int, aya: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkDao.delete(sura, aya)
            load()
        }
    }
}
