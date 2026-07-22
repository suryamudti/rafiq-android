package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.repository.MetalPriceRepository

data class ZakatResult(
    val goldZakat: Double = 0.0,
    val silverZakat: Double = 0.0,
    val cashZakat: Double = 0.0,
    val totalZakat: Double = 0.0,
    val goldPricePerGram: Double = 0.0,
    val silverPricePerGram: Double = 0.0
)

class CalculateZakatUseCase(
    private val metalPriceRepository: MetalPriceRepository
) {
    suspend operator fun invoke(
        goldWeight: Double,
        silverWeight: Double,
        cashValue: Double,
        currency: String = "USD"
    ): Result<ZakatResult, AppError> {
        return when (val goldPrice = metalPriceRepository.getGoldPricePerGram()) {
            is Result.Error -> goldPrice
            is Result.Success -> {
                when (val silverPrice = metalPriceRepository.getSilverPricePerGram()) {
                    is Result.Error -> silverPrice
                    is Result.Success -> {
                        val goldNisab = 85.0
                        val silverNisab = 595.0
                        val rate = if (currency == "IDR") 16000.0 else 1.0
                        val cashVUsd = if (currency == "IDR") cashValue / rate else cashValue
                        val cashRateUsd = goldPrice.data * goldNisab

                        val goldZakatUsd = if (goldWeight >= goldNisab) goldWeight * goldPrice.data * 0.025 else 0.0
                        val silverZakatUsd = if (silverWeight >= silverNisab) silverWeight * silverPrice.data * 0.025 else 0.0
                        val cashZakatUsd = if (cashVUsd >= cashRateUsd) cashVUsd * 0.025 else 0.0

                        Result.Success(
                            ZakatResult(
                                goldZakat = goldZakatUsd * rate,
                                silverZakat = silverZakatUsd * rate,
                                cashZakat = cashZakatUsd * rate,
                                totalZakat = (goldZakatUsd + silverZakatUsd + cashZakatUsd) * rate,
                                goldPricePerGram = goldPrice.data * rate,
                                silverPricePerGram = silverPrice.data * rate
                            )
                        )
                    }
                }
            }
        }
    }
}
