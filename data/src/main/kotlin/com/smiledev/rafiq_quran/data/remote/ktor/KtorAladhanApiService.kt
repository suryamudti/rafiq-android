package com.smiledev.rafiq_quran.data.remote.ktor

import com.smiledev.rafiq_quran.data.remote.AladhanApiService
import com.smiledev.rafiq_quran.data.remote.PrayerTimesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class KtorAladhanApiService(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.aladhan.com/"
) : AladhanApiService {

    override suspend fun getTimings(
        date: String,
        latitude: Double,
        longitude: Double,
        method: Int
    ): PrayerTimesResponse {
        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return client.get("${sanitizedBaseUrl}v1/timings/$date") {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("method", method)
        }.body()
    }
}
