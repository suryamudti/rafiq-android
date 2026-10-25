package com.smiledev.rafiq_quran.data.remote.ktor

import com.smiledev.rafiq_quran.data.remote.EQuranApiService
import com.smiledev.rafiq_quran.data.remote.EQuranTafsirResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class KtorEQuranApiService(
    private val client: HttpClient,
    private val baseUrl: String = "https://equran.id/"
) : EQuranApiService {

    override suspend fun getTafsir(surah: Int): EQuranTafsirResponse {
        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return client.get("${sanitizedBaseUrl}api/v2/tafsir/$surah").body()
    }
}
