package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.MetalPrices

interface MetalPriceRepository {
    suspend fun getGoldPricePerGram(): Result<Double, AppError>
    suspend fun getSilverPricePerGram(): Result<Double, AppError>
    suspend fun fetchMetalPrices(): Result<MetalPrices, AppError>
    fun getCachedMetalPrices(): MetalPrices?
}
