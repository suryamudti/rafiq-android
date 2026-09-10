package com.smiledev.rafiq_quran.ui.prayerguidance

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceItem
import com.smiledev.rafiq_quran.domain.usecase.GetPrayerGuidanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class PrayerGuidanceDetailUiState(
    val item: PrayerGuidanceItem? = null,
    val isLoading: Boolean = true,
    val error: AppError? = null
)

@HiltViewModel
class PrayerGuidanceDetailViewModel @Inject constructor(
    private val getPrayerGuidanceUseCase: GetPrayerGuidanceUseCase,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerGuidanceDetailUiState())
    val uiState: StateFlow<PrayerGuidanceDetailUiState> = _uiState

    val localeCode: String = currentLocaleCode()

    fun loadDetail(guidanceId: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = getPrayerGuidanceUseCase.getById(guidanceId)) {
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
