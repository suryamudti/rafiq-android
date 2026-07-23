package com.smiledev.rafiq.ui.asmaulhusna

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.currentLocaleCode
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.AsmaulHusna
import com.smiledev.rafiq.domain.repository.AsmaulHusnaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


@Immutable
data class AsmaulHusnaUiState(
    val names: List<AsmaulHusna> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null
)

@HiltViewModel
class AsmaulHusnaViewModel @Inject constructor(
    private val repository: AsmaulHusnaRepository,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AsmaulHusnaUiState())
    val uiState: StateFlow<AsmaulHusnaUiState> = _uiState

    val localeCode = currentLocaleCode()

    init { load() }

    private fun load() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.getNames()
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(names = result.data, isLoading = false)
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

    fun filteredNames(): List<AsmaulHusna> {
        val q = _uiState.value.searchQuery.lowercase()
        if (q.isEmpty()) return _uiState.value.names
        return _uiState.value.names.filter {
            it.transliteration.lowercase().contains(q) ||
            it.meaningEn.lowercase().contains(q) ||
            it.meaningId.lowercase().contains(q) ||
            it.arabic.contains(q)
        }
    }
}
