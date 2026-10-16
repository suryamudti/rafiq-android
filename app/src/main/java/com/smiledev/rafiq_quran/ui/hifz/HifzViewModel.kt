package com.smiledev.rafiq_quran.ui.hifz

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.domain.repository.HifzGoal
import com.smiledev.rafiq_quran.domain.repository.HifzRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class HifzUiState(
    val goals: List<HifzGoal> = emptyList(),
    val isLoading: Boolean = false,
    val formError: String? = null
)

@HiltViewModel
class HifzViewModel @Inject constructor(
    private val hifzRepository: HifzRepository,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HifzUiState())
    val uiState: StateFlow<HifzUiState> = _uiState

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            hifzRepository.observeAll().collect { goals ->
                _uiState.value = _uiState.value.copy(
                    goals = goals,
                    isLoading = false
                )
            }
        }
    }

    fun toggleAyah(goalId: Long, aya: Int) {
        val goal = _uiState.value.goals.find { it.id == goalId } ?: return
        val updated = goal.copy(
            memorizedAyahs = if (aya in goal.memorizedAyahs) goal.memorizedAyahs - aya else goal.memorizedAyahs + aya
        )
        viewModelScope.launch(dispatcherProvider.io) {
            hifzRepository.upsert(updated)
        }
    }

    fun addGoal(suraNumber: Int, suraName: String, ayaStart: Int, ayaEnd: Int) {
        if (suraNumber < 1 || ayaStart < 1 || ayaStart > ayaEnd) {
            _uiState.value = _uiState.value.copy(formError = "invalid_range")
            return
        }
        _uiState.value = _uiState.value.copy(formError = null)
        viewModelScope.launch(dispatcherProvider.io) {
            hifzRepository.upsert(
                HifzGoal(
                    suraNumber = suraNumber,
                    suraName = suraName.ifBlank { "Surah $suraNumber" },
                    ayaStart = ayaStart,
                    ayaEnd = ayaEnd
                )
            )
        }
    }

    fun clearFormError() {
        _uiState.value = _uiState.value.copy(formError = null)
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch(dispatcherProvider.io) {
            hifzRepository.delete(id)
        }
    }
}
