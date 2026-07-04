package com.smiledev.rafiq.ui.prophets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.data.models.ProphetStory
import com.smiledev.rafiq.data.repository.ProphetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class ProphetsUiState(
    val prophets: List<ProphetStory> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProphetsViewModel @Inject constructor(
    private val prophetRepository: ProphetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProphetsUiState())
    val uiState: StateFlow<ProphetsUiState> = _uiState

    val localeCode = if (Locale.getDefault().language == "id") "id" else "en"

    init { loadProphets() }

    fun loadProphets() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val prophets = prophetRepository.getProphets()
                _uiState.value = _uiState.value.copy(prophets = prophets, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
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
