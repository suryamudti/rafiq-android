package com.smiledev.rafiq_quran.ui.asmaulhusna

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.AsmaulHusna
import com.smiledev.rafiq_quran.domain.repository.AsmaulHusnaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


@Immutable
data class AsmaulHusnaUiState(
    val names: List<AsmaulHusna> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
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

    fun filterNames(names: List<AsmaulHusna>, query: String): List<AsmaulHusna> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return names
        return names.filter {
            it.transliteration.lowercase().contains(q) ||
            it.meaningEn.lowercase().contains(q) ||
            it.meaningId.lowercase().contains(q) ||
            it.arabic.contains(q)
        }
    }

    fun filterNames(state: AsmaulHusnaUiState): List<AsmaulHusna> {
        return filterNames(state.names, state.searchQuery)
    }

    fun filteredNames(): List<AsmaulHusna> {
        return filterNames(_uiState.value.names, _uiState.value.searchQuery)
    }
}
