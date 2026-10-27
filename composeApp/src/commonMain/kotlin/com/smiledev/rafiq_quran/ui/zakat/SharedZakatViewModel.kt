package com.smiledev.rafiq_quran.ui.zakat

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.DefaultMetalPrices
import com.smiledev.rafiq_quran.domain.model.MetalPrices
import com.smiledev.rafiq_quran.domain.repository.MetalPriceRepository
import com.smiledev.rafiq_quran.domain.usecase.CalculateZakatUseCase
import com.smiledev.rafiq_quran.domain.usecase.ZakatResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class SharedZakatUiState(
    val goldWeight: String = "",
    val silverWeight: String = "",
    val cashAmount: String = "",
    val selectedCurrency: String = "USD",
    val result: ZakatResult = ZakatResult(),
    val isUsingFallback: Boolean = false,
    val isRefreshing: Boolean = false
)

class SharedZakatViewModel(
    private val calculateZakatUseCase: CalculateZakatUseCase = CalculateZakatUseCase(),
    private val metalPriceRepository: MetalPriceRepository? = null,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedZakatUiState())
    val uiState: StateFlow<SharedZakatUiState> = _uiState.asStateFlow()

    init {
        prefetchPrices()
    }

    fun updateGold(value: String) {
        _uiState.update { it.copy(goldWeight = value) }
    }

    fun updateSilver(value: String) {
        _uiState.update { it.copy(silverWeight = value) }
    }

    fun updateCash(value: String) {
        _uiState.update { it.copy(cashAmount = value) }
    }

    fun updateCurrency(value: String) {
        _uiState.update { it.copy(selectedCurrency = value) }
        calculate()
    }

    fun calculate() {
        val s = _uiState.value
        val cached = metalPriceRepository?.getCachedMetalPrices()
        _uiState.update { it.copy(isRefreshing = true) }
        publishResult(s, cached ?: DefaultMetalPrices, isUsingFallback = cached == null)
        refreshPrices()
    }

    private fun prefetchPrices() {
        if (metalPriceRepository == null) return
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = metalPriceRepository.fetchMetalPrices()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                    recomputeIfHasInputs(result.data)
                }
                is Result.Error -> _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun refreshPrices() {
        if (metalPriceRepository == null) {
            _uiState.update { it.copy(isRefreshing = false) }
            return
        }
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = metalPriceRepository.fetchMetalPrices()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                    recomputeIfHasInputs(result.data)
                }
                is Result.Error -> _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun recomputeIfHasInputs(prices: MetalPrices) {
        val s = _uiState.value
        val hasInputs = s.goldWeight.isNotBlank() || s.silverWeight.isNotBlank() || s.cashAmount.isNotBlank()
        if (hasInputs) publishResult(s, prices, isUsingFallback = false)
    }

    private fun publishResult(s: SharedZakatUiState, prices: MetalPrices, isUsingFallback: Boolean) {
        val goldW = s.goldWeight.toDoubleOrNull() ?: 0.0
        val silverW = s.silverWeight.toDoubleOrNull() ?: 0.0
        val cashV = s.cashAmount.toDoubleOrNull() ?: 0.0
        val result = calculateZakatUseCase(goldW, silverW, cashV, s.selectedCurrency, prices)
        _uiState.update { it.copy(result = result, isUsingFallback = isUsingFallback) }
    }
}
