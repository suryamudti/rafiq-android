package com.smiledev.rafiq_quran.ui.sunnahguidance

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.domain.model.SunnahGuidanceItem
import com.smiledev.rafiq_quran.domain.usecase.GetSunnahGuidanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class SunnahGuidanceDetailUiState(
    val item: SunnahGuidanceItem? = null,
    val isLoading: Boolean = true,
    val error: AppError? = null
)

@HiltViewModel
class SunnahGuidanceDetailViewModel @Inject constructor(
    private val getSunnahGuidanceUseCase: GetSunnahGuidanceUseCase,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SunnahGuidanceDetailUiState())
    val uiState: StateFlow<SunnahGuidanceDetailUiState> = _uiState.asStateFlow()

    val localeCode: String = currentLocaleCode()

    fun loadDetail(sunnahId: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = getSunnahGuidanceUseCase.getById(sunnahId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            item = result.data,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.error
                        )
                    }
                }
            }
        }
    }
}
