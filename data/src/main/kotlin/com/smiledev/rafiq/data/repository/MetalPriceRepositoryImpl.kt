package com.smiledev.rafiq.data.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.retryIO
import com.smiledev.rafiq.data.remote.MetalPriceApi
import com.smiledev.rafiq.domain.model.MetalPrices
import com.smiledev.rafiq.domain.repository.MetalPriceRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class MetalPriceRepositoryImpl @Inject constructor(
    private val metalPriceApi: MetalPriceApi
) : MetalPriceRepository {

    @Volatile
    private var cachedPrices: MetalPrices? = null

    override suspend fun fetchMetalPrices(): Result<MetalPrices, AppError> {
        return retryIO(times = 2, initialDelay = 50, maxDelay = 300) {
            try {
                val prices = coroutineScope {
                    val gold = async { metalPriceApi.getGoldPricePerGram() }
                    val silver = async { metalPriceApi.getSilverPricePerGram() }
                    MetalPrices(gold.await(), silver.await())
                }
                cachedPrices = prices
                Result.Success(prices)
            } catch (e: Exception) {
                Result.Error(AppError.Network("Failed to fetch metal prices", e))
            }
        }
    }

    override fun getCachedMetalPrices(): MetalPrices? = cachedPrices
}
