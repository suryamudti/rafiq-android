package com.smiledev.rafiq.ui.recitation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.data.models.Reciter
import com.smiledev.rafiq.data.repository.ReciterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecitationUiState(
    val reciters: List<Reciter> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class RecitationViewModel @Inject constructor(
    private val reciterRepository: ReciterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecitationUiState())
    val uiState: StateFlow<RecitationUiState> = _uiState

    init { load() }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val reciters = reciterRepository.getReciters()
            _uiState.value = _uiState.value.copy(reciters = reciters, isLoading = false)
        }
    }
}
