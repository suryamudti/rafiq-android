package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.domain.model.MetalPrices

data class ZakatResult(
    val goldZakat: Double = 0.0,
    val silverZakat: Double = 0.0,
    val cashZakat: Double = 0.0,
    val totalZakat: Double = 0.0,
    val goldPricePerGram: Double = 0.0,
    val silverPricePerGram: Double = 0.0
)

class CalculateZakatUseCase {
    operator fun invoke(
        goldWeight: Double,
        silverWeight: Double,
        cashValue: Double,
        currency: String = "USD",
        prices: MetalPrices
    ): ZakatResult {
        val goldNisab = 85.0
        val silverNisab = 595.0
        val exchangeRate = 16000.0
        val rate = if (currency == "IDR") exchangeRate else 1.0
        val cashVUsd = if (currency == "IDR") cashValue / rate else cashValue
        val cashRateUsd = prices.goldPricePerGram * goldNisab

        val goldZakatUsd = if (goldWeight >= goldNisab) goldWeight * prices.goldPricePerGram * 0.025 else 0.0
        val silverZakatUsd = if (silverWeight >= silverNisab) silverWeight * prices.silverPricePerGram * 0.025 else 0.0
        val cashZakatUsd = if (cashVUsd >= cashRateUsd) cashVUsd * 0.025 else 0.0

        return ZakatResult(
            goldZakat = goldZakatUsd * rate,
            silverZakat = silverZakatUsd * rate,
            cashZakat = cashZakatUsd * rate,
            totalZakat = (goldZakatUsd + silverZakatUsd + cashZakatUsd) * rate,
            goldPricePerGram = prices.goldPricePerGram * rate,
            silverPricePerGram = prices.silverPricePerGram * rate
        )
    }
}
