package com.smiledev.rafiq.ui.zakat

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.DefaultMetalPrices
import com.smiledev.rafiq.domain.model.MetalPrices
import com.smiledev.rafiq.domain.repository.MetalPriceRepository
import com.smiledev.rafiq.domain.usecase.CalculateZakatUseCase
import com.smiledev.rafiq.domain.usecase.ZakatResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ZakatUiState(
    val goldWeight: String = "",
    val silverWeight: String = "",
    val cashAmount: String = "",
    val selectedCurrency: String = "USD",
    val result: ZakatResult = ZakatResult(),
    val isUsingFallback: Boolean = false
)

@HiltViewModel
class ZakatCalculatorViewModel @Inject constructor(
    private val calculateZakatUseCase: CalculateZakatUseCase,
    private val metalPriceRepository: MetalPriceRepository,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ZakatUiState(
            goldWeight = savedStateHandle.get<String>("goldWeight") ?: "",
            silverWeight = savedStateHandle.get<String>("silverWeight") ?: "",
            cashAmount = savedStateHandle.get<String>("cashAmount") ?: "",
            selectedCurrency = savedStateHandle.get<String>("selectedCurrency") ?: "USD"
        )
    )
    val uiState: StateFlow<ZakatUiState> = _uiState

    init {
        prefetchPrices()
    }

    fun updateGold(value: String) { _uiState.value = _uiState.value.copy(goldWeight = value) }
    fun updateSilver(value: String) { _uiState.value = _uiState.value.copy(silverWeight = value) }
    fun updateCash(value: String) { _uiState.value = _uiState.value.copy(cashAmount = value) }

    fun updateCurrency(value: String) {
        _uiState.value = _uiState.value.copy(selectedCurrency = value)
        calculate()
    }

    fun calculate() {
        val s = _uiState.value
        val cached = metalPriceRepository.getCachedMetalPrices()
        publishResult(s, cached ?: DefaultMetalPrices, isUsingFallback = cached == null)
        refreshPrices()
    }

    private fun prefetchPrices() {
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = metalPriceRepository.fetchMetalPrices()) {
                is Result.Success -> recomputeIfHasInputs(result.data)
                is Result.Error -> Unit
            }
        }
    }

    private fun refreshPrices() {
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = metalPriceRepository.fetchMetalPrices()) {
                is Result.Success -> recomputeIfHasInputs(result.data)
                is Result.Error -> Unit
            }
        }
    }

    private fun recomputeIfHasInputs(prices: MetalPrices) {
        val s = _uiState.value
        val hasInputs = s.goldWeight.isNotBlank() || s.silverWeight.isNotBlank() || s.cashAmount.isNotBlank()
        if (hasInputs) publishResult(s, prices, isUsingFallback = false)
    }

    private fun publishResult(s: ZakatUiState, prices: MetalPrices, isUsingFallback: Boolean) {
        val goldW = s.goldWeight.toDoubleOrNull() ?: 0.0
        val silverW = s.silverWeight.toDoubleOrNull() ?: 0.0
        val cashV = s.cashAmount.toDoubleOrNull() ?: 0.0
        val result = calculateZakatUseCase(goldW, silverW, cashV, s.selectedCurrency, prices)
        _uiState.value = _uiState.value.copy(result = result, isUsingFallback = isUsingFallback)
    }
}
