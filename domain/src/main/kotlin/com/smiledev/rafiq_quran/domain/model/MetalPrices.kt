package com.smiledev.rafiq_quran.domain.model

data class MetalPrices(
    val goldPricePerGram: Double,
    val silverPricePerGram: Double
)

val DefaultMetalPrices = MetalPrices(
    goldPricePerGram = 65.0,
    silverPricePerGram = 0.75
)
