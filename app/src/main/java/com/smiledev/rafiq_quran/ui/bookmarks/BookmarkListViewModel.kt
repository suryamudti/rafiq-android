package com.smiledev.rafiq.ui.bookmarks

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.domain.repository.BookmarkItem
import com.smiledev.rafiq.domain.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class BookmarkListUiState(
    val bookmarks: List<BookmarkItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class BookmarkListViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarkListUiState())
    val uiState: StateFlow<BookmarkListUiState> = _uiState

    init {
        observeBookmarks()
    }

    private fun observeBookmarks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            bookmarkRepository.observeAll().collect { bookmarks ->
                _uiState.value = _uiState.value.copy(bookmarks = bookmarks, isLoading = false)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = false)
            bookmarkRepository.observeAll().collect { bookmarks ->
                _uiState.value = _uiState.value.copy(bookmarks = bookmarks, isLoading = false)
            }
        }
    }

    fun delete(sura: Int, aya: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            bookmarkRepository.delete(sura, aya)
        }
    }
}
