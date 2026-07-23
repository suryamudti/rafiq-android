package com.smiledev.rafiq.data.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.retryIO
import com.smiledev.rafiq.data.remote.MetalPriceApi
import com.smiledev.rafiq.domain.repository.MetalPriceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetalPriceRepositoryImpl @Inject constructor(
    private val metalPriceApi: MetalPriceApi
) : MetalPriceRepository {
    override suspend fun getGoldPricePerGram(): Result<Double, AppError> {
        return retryIO {
            try {
                Result.Success(metalPriceApi.getGoldPricePerGram())
            } catch (e: Exception) {
                Result.Error(AppError.Network("Failed to fetch gold price", e))
            }
        }
    }

    override suspend fun getSilverPricePerGram(): Result<Double, AppError> {
        return retryIO {
            try {
                Result.Success(metalPriceApi.getSilverPricePerGram())
            } catch (e: Exception) {
                Result.Error(AppError.Network("Failed to fetch silver price", e))
            }
        }
    }
}
