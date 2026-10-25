package com.smiledev.rafiq_quran.data.remote.ktor

import com.smiledev.rafiq_quran.data.remote.OverpassApiService
import com.smiledev.rafiq_quran.data.remote.OverpassResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters

class KtorOverpassApiService(
    private val client: HttpClient,
    private val baseUrl: String
) : OverpassApiService {

    override suspend fun query(query: String): OverpassResponse {
        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return client.submitForm(
            url = "${sanitizedBaseUrl}interpreter",
            formParameters = parameters {
                append("data", query)
            }
        ).body()
    }
}
