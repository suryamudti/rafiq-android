package com.smiledev.rafiq.ui.asmaulhusna

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.data.models.AsmaulHusna
import com.smiledev.rafiq.data.repository.AsmaulHusnaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class AsmaulHusnaUiState(
    val names: List<AsmaulHusna> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AsmaulHusnaViewModel @Inject constructor(
    private val repository: AsmaulHusnaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AsmaulHusnaUiState())
    val uiState: StateFlow<AsmaulHusnaUiState> = _uiState

    val localeCode = if (Locale.getDefault().language == "id") "id" else "en"

    init { load() }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val names = repository.getNames()
                _uiState.value = _uiState.value.copy(names = names, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
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
