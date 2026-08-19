package com.smiledev.rafiq_quran.data.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.core.retryIO
import com.smiledev.rafiq_quran.data.remote.MetalPriceApi
import com.smiledev.rafiq_quran.domain.model.MetalPrices
import com.smiledev.rafiq_quran.domain.repository.MetalPriceRepository
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
