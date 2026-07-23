package com.smiledev.rafiq.ui.prophets

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.ProphetStory
import com.smiledev.rafiq.domain.repository.ProphetRepository
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.smiledev.rafiq.core.currentLocaleCode
import javax.inject.Inject

@Immutable
data class ProphetsUiState(
    val prophets: List<ProphetStory> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null
)

@HiltViewModel
class ProphetsViewModel @Inject constructor(
    private val prophetRepository: ProphetRepository,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProphetsUiState())
    val uiState: StateFlow<ProphetsUiState> = _uiState

    val localeCode = currentLocaleCode()

    init { loadProphets() }

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

    fun filteredProphets(): List<ProphetStory> {
        val q = _uiState.value.searchQuery.lowercase()
        return if (q.isEmpty()) _uiState.value.prophets
        else _uiState.value.prophets.filter {
            it.nameEn.lowercase().contains(q) || it.nameId.lowercase().contains(q)
        }
    }
}
