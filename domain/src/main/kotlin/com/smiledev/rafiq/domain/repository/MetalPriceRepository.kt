package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result

interface MetalPriceRepository {
    suspend fun getGoldPricePerGram(): Result<Double, AppError>
    suspend fun getSilverPricePerGram(): Result<Double, AppError>
}
