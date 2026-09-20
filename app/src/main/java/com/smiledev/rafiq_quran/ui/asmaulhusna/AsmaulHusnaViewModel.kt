package com.smiledev.rafiq_quran.ui.asmaulhusna

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.AsmaulHusna
import com.smiledev.rafiq_quran.domain.repository.AsmaulHusnaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class AsmaulHusnaUiState(
    val names: List<AsmaulHusna> = emptyList(),
    val searchQuery: String = "",
    val favoriteIds: Set<Int> = emptySet(),
    val showFavoritesOnly: Boolean = false,
    val isGridView: Boolean = false,
    val selectedName: AsmaulHusna? = null,
    val dhikrCount: Int = 0,
    val isLoading: Boolean = true,
    val error: AppError? = null
)

@HiltViewModel
class AsmaulHusnaViewModel @Inject constructor(
    private val repository: AsmaulHusnaRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AsmaulHusnaUiState())
    val uiState: StateFlow<AsmaulHusnaUiState> = _uiState

    val localeCode: String
        get() = currentLocaleCode()

    init {
        load()
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.favoriteAsmaulHusnaIds.collect { favs ->
                _uiState.update { it.copy(favoriteIds = favs) }
            }
        }
    }

    private fun load() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getNames()
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(names = result.data, isLoading = false) }
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

    fun setGridView(isGrid: Boolean) {
        _uiState.update { it.copy(isGridView = isGrid) }
    }

    fun selectName(name: AsmaulHusna?) {
        _uiState.update { it.copy(selectedName = name, dhikrCount = 0) }
    }

    fun incrementDhikr() {
        _uiState.update { it.copy(dhikrCount = it.dhikrCount + 1) }
    }

    fun resetDhikr() {
        _uiState.update { it.copy(dhikrCount = 0) }
    }

    fun toggleFavorite(id: Int) {
        val willAdd = id !in _uiState.value.favoriteIds
        _uiState.update { state ->
            val favs = if (willAdd) state.favoriteIds + id else state.favoriteIds - id
            state.copy(favoriteIds = favs)
        }
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.toggleFavoriteAsmaulHusna(id)
        }
    }

    fun getDailyName(names: List<AsmaulHusna>): AsmaulHusna? {
        if (names.isEmpty()) return null
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val index = (dayOfYear - 1) % names.size
        return names.getOrNull(index) ?: names.firstOrNull()
    }

    fun getRandomName(): AsmaulHusna? {
        val names = _uiState.value.names
        return if (names.isNotEmpty()) names.random() else null
    }

    fun filterNames(
        names: List<AsmaulHusna>,
        query: String,
        showFavoritesOnly: Boolean = false,
        favoriteIds: Set<Int> = emptySet()
    ): List<AsmaulHusna> {
        val q = query.trim().lowercase()
        return names.filter { item ->
            val matchesFavorite = !showFavoritesOnly || item.id in favoriteIds
            val matchesQuery = if (q.isEmpty()) {
                true
            } else {
                item.transliteration.lowercase().contains(q) ||
                item.meaningEn.lowercase().contains(q) ||
                item.meaningId.lowercase().contains(q) ||
                item.arabic.contains(q) ||
                item.id.toString() == q
            }
            matchesFavorite && matchesQuery
        }
    }

    fun filterNames(state: AsmaulHusnaUiState): List<AsmaulHusna> {
        return filterNames(
            names = state.names,
            query = state.searchQuery,
            showFavoritesOnly = state.showFavoritesOnly,
            favoriteIds = state.favoriteIds
        )
    }

    fun filteredNames(): List<AsmaulHusna> {
        return filterNames(_uiState.value)
    }
}
