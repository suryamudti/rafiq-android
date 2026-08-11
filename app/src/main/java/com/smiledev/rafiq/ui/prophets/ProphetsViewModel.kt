package com.smiledev.rafiq.ui.prophets

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.currentLocaleCode
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.ProphetStory
import com.smiledev.rafiq.domain.repository.ProphetRepository
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ProphetsUiState(
    val prophets: List<ProphetStory> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val favoriteIds: Set<Int> = emptySet(),
    val showFavoritesOnly: Boolean = false,
    val storyFontSize: Int = 16
)

@HiltViewModel
class ProphetsViewModel @Inject constructor(
    private val prophetRepository: ProphetRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProphetsUiState())
    val uiState: StateFlow<ProphetsUiState> = _uiState

    val localeCode = currentLocaleCode()

    init {
        loadProphets()
        viewModelScope.launch(dispatcherProvider.io) {
            combine(
                preferencesManager.favoriteProphetIds,
                preferencesManager.storyFontSize
            ) { favIds, size ->
                _uiState.value = _uiState.value.copy(
                    favoriteIds = favIds,
                    storyFontSize = size
                )
            }.collect { }
        }
    }

    fun loadProphets() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = prophetRepository.getProphets()
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(prophets = result.data, isLoading = false)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setShowFavoritesOnly(show: Boolean) {
        _uiState.value = _uiState.value.copy(showFavoritesOnly = show)
    }

    fun toggleFavorite(id: Int) {
        val current = _uiState.value.favoriteIds
        _uiState.value = _uiState.value.copy(
            favoriteIds = if (id in current) current - id else current + id
        )
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.toggleFavoriteProphet(id)
        }
    }

    fun setStoryFontSize(size: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.setStoryFontSize(size)
        }
    }

    fun filteredProphets(): List<ProphetStory> {
        val state = _uiState.value
        val q = state.searchQuery.lowercase()
        return state.prophets.filter { p ->
            val favoriteOk = !state.showFavoritesOnly || p.id in state.favoriteIds
            val searchOk = q.isEmpty() ||
                p.nameEn.lowercase().contains(q) ||
                p.nameId.lowercase().contains(q) ||
                p.nameArabic.lowercase().contains(q)
            favoriteOk && searchOk
        }
    }
}