package com.smiledev.rafiq_quran.data.remote.ktor

import com.smiledev.rafiq_quran.data.remote.MetalPriceApiService
import com.smiledev.rafiq_quran.data.remote.MetalSpotPrice
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class KtorMetalPriceApiService(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.gold-api.com/"
) : MetalPriceApiService {

    override suspend fun getGoldPrice(): List<MetalSpotPrice> {
        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return try {
            client.get("${sanitizedBaseUrl}v1/spot/gold").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getSilverPrice(): List<MetalSpotPrice> {
        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return try {
            client.get("${sanitizedBaseUrl}v1/spot/silver").body()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
