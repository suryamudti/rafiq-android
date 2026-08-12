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
import kotlinx.coroutines.flow.update
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
                _uiState.update {
                    it.copy(
                        favoriteIds = favIds,
                        storyFontSize = size
                    )
                }
            }.collect { }
        }
    }

    fun loadProphets() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = prophetRepository.getProphets()
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(prophets = result.data, isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error) }
                }
            }
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setShowFavoritesOnly(show: Boolean) {
        _uiState.update { it.copy(showFavoritesOnly = show) }
    }

    fun toggleFavorite(id: Int) {
        val willAdd = id !in _uiState.value.favoriteIds
        _uiState.update { state ->
            val favs = if (willAdd) state.favoriteIds + id else state.favoriteIds - id
            state.copy(favoriteIds = favs)
        }
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.toggleFavoriteProphet(id)
        }
    }

    fun setStoryFontSize(size: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.setStoryFontSize(size)
        }
    }

    fun filterProphets(state: ProphetsUiState): List<ProphetStory> {
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