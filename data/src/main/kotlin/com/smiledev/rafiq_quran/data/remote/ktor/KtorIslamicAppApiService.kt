package com.smiledev.rafiq_quran.data.remote.ktor

import com.smiledev.rafiq_quran.data.remote.IslamicAppApiService
import com.smiledev.rafiq_quran.data.remote.IslamicAppResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class KtorIslamicAppApiService(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.quran.com/"
) : IslamicAppApiService {

    override suspend fun getVerseWithTafsir(
        sura: Int,
        aya: Int,
        tafsirs: String
    ): IslamicAppResponse {
        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return client.get("${sanitizedBaseUrl}v1/verses/by_key/$sura:$aya") {
            parameter("tafsirs", tafsirs)
        }.body()
    }
}
