package com.smiledev.rafiq.ui.zakat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.data.repository.MetalPriceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ZakatResult(
    val goldZakat: Double = 0.0,
    val silverZakat: Double = 0.0,
    val cashZakat: Double = 0.0,
    val totalZakat: Double = 0.0,
    val goldPricePerGram: Double = 0.0,
    val silverPricePerGram: Double = 0.0
)

data class ZakatUiState(
    val goldWeight: String = "",
    val silverWeight: String = "",
    val cashAmount: String = "",
    val selectedCurrency: String = "USD",
    val result: ZakatResult = ZakatResult(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ZakatCalculatorViewModel @Inject constructor(
    private val metalPriceRepository: MetalPriceRepository,
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

    fun updateGold(value: String) { _uiState.value = _uiState.value.copy(goldWeight = value) }
    fun updateSilver(value: String) { _uiState.value = _uiState.value.copy(silverWeight = value) }
    fun updateCash(value: String) { _uiState.value = _uiState.value.copy(cashAmount = value) }
    
    fun updateCurrency(value: String) { 
        _uiState.value = _uiState.value.copy(selectedCurrency = value)
        calculate()
    }

    fun calculate() {
        val s = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val goldW = s.goldWeight.toDoubleOrNull() ?: 0.0
                val silverW = s.silverWeight.toDoubleOrNull() ?: 0.0
                val cashV = s.cashAmount.toDoubleOrNull() ?: 0.0

                val goldPriceUsd = metalPriceRepository.getGoldPricePerGram()
                val silverPriceUsd = metalPriceRepository.getSilverPricePerGram()

                val goldNisab = 85.0
                val silverNisab = 595.0

                val exchangeRate = 16000.0 // Default 1 USD = 16,000 IDR
                val rate = if (s.selectedCurrency == "IDR") exchangeRate else 1.0

                // Convert cash input to USD for accurate Nisab comparison
                val cashVUsd = if (s.selectedCurrency == "IDR") cashV / rate else cashV
                val cashRateUsd = goldPriceUsd * goldNisab

                val goldZakatUsd = if (goldW >= goldNisab) goldW * goldPriceUsd * 0.025 else 0.0
                val silverZakatUsd = if (silverW >= silverNisab) silverW * silverPriceUsd * 0.025 else 0.0
                val cashZakatUsd = if (cashVUsd >= cashRateUsd) cashVUsd * 0.025 else 0.0

                val goldZakat = goldZakatUsd * rate
                val silverZakat = silverZakatUsd * rate
                val cashZakat = cashZakatUsd * rate
                val totalZakat = goldZakat + silverZakat + cashZakat

                val goldPrice = goldPriceUsd * rate
                val silverPrice = silverPriceUsd * rate

                _uiState.value = _uiState.value.copy(
                    result = ZakatResult(
                        goldZakat = goldZakat,
                        silverZakat = silverZakat,
                        cashZakat = cashZakat,
                        totalZakat = totalZakat,
                        goldPricePerGram = goldPrice,
                        silverPricePerGram = silverPrice
                    ),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
