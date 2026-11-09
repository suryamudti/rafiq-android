package com.smiledev.rafiq_quran.ui.bookmarks

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.smiledev.rafiq_quran.domain.repository.BookmarkItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

val DEFAULT_INITIAL_BOOKMARKS = listOf(
    BookmarkItem(1, 1, "Al-Fatihah", 1, "2026-10-28 04:30"),
    BookmarkItem(2, 2, "Al-Baqarah", 255, "2026-10-27 20:15"),
    BookmarkItem(3, 18, "Al-Kahf", 10, "2026-10-24 06:10"),
    BookmarkItem(4, 36, "Yasin", 58, "2026-10-22 19:40"),
    BookmarkItem(5, 67, "Al-Mulk", 1, "2026-10-20 21:00")
)

@Immutable
data class SharedBookmarksUiState(
    val bookmarks: List<BookmarkItem> = DEFAULT_INITIAL_BOOKMARKS,
    val searchQuery: String = "",
    val isLoading: Boolean = false
) {
    val filteredBookmarks: List<BookmarkItem>
        get() {
            if (searchQuery.isBlank()) return bookmarks
            val q = searchQuery.trim().lowercase()
            return bookmarks.filter {
                it.suraName.lowercase().contains(q) ||
                "${it.sura}:${it.aya}".contains(q) ||
                "surah ${it.sura}".contains(q) ||
                "ayah ${it.aya}".contains(q)
            }
        }
}

class SharedBookmarksViewModel(
    initialBookmarks: List<BookmarkItem> = DEFAULT_INITIAL_BOOKMARKS
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SharedBookmarksUiState(bookmarks = initialBookmarks)
    )
    val uiState: StateFlow<SharedBookmarksUiState> = _uiState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteBookmark(sura: Int, aya: Int) {
        _uiState.update { state ->
            val updated = state.bookmarks.filterNot { it.sura == sura && it.aya == aya }
            state.copy(bookmarks = updated)
        }
    }

    fun clearAllBookmarks() {
        _uiState.update { it.copy(bookmarks = emptyList()) }
    }
}
