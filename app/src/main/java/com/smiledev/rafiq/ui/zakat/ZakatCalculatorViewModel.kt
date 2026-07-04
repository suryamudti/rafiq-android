package com.smiledev.rafiq.ui.zakat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.data.remote.MetalPriceApi
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
    val result: ZakatResult = ZakatResult(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ZakatCalculatorViewModel @Inject constructor(
    private val metalPriceApi: MetalPriceApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(ZakatUiState())
    val uiState: StateFlow<ZakatUiState> = _uiState

    fun calculate(goldGrams: String, silverGrams: String, cash: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val goldW = goldGrams.toDoubleOrNull() ?: 0.0
                val silverW = silverGrams.toDoubleOrNull() ?: 0.0
                val cashV = cash.toDoubleOrNull() ?: 0.0

                val goldPrice = metalPriceApi.getGoldPricePerGram()
                val silverPrice = metalPriceApi.getSilverPricePerGram()

                val goldNisab = 85.0
                val silverNisab = 595.0

                val goldZakat = if (goldW >= goldNisab) goldW * goldPrice * 0.025 else 0.0
                val silverZakat = if (silverW >= silverNisab) silverW * silverPrice * 0.025 else 0.0
                val cashRate = goldPrice * goldNisab
                val cashZakat = if (cashV >= cashRate) cashV * 0.025 else 0.0

                _uiState.value = _uiState.value.copy(
                    result = ZakatResult(
                        goldZakat = goldZakat,
                        silverZakat = silverZakat,
                        cashZakat = cashZakat,
                        totalZakat = goldZakat + silverZakat + cashZakat,
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
