package com.smiledev.rafiq.data.remote

import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Singleton

data class MetalSpotPrice(
    val price: Double,
    val unit: String?,
    val date: String?,
    val timestamp: Long?
)

interface MetalPriceApiService {
    @GET("v1/spot/gold")
    suspend fun getGoldPrice(): List<MetalSpotPrice>

    @GET("v1/spot/silver")
    suspend fun getSilverPrice(): List<MetalSpotPrice>
}

@Singleton
class MetalPriceApi @Inject constructor(
    private val service: MetalPriceApiService
) {
    suspend fun getGoldPricePerGram(): Double {
        val prices = service.getGoldPrice()
        if (prices.isEmpty()) return 65.0
        return prices[0].price / 31.1035
    }

    suspend fun getSilverPricePerGram(): Double {
        val prices = service.getSilverPrice()
        if (prices.isEmpty()) return 0.75
        return prices[0].price / 31.1035
    }
}
