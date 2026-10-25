package com.smiledev.rafiq_quran.data.remote.ktor

import com.smiledev.rafiq_quran.data.remote.GithubReleaseApiService
import com.smiledev.rafiq_quran.data.remote.GithubReleaseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header

class KtorGithubReleaseApiService(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.github.com/"
) : GithubReleaseApiService {

    override suspend fun getLatestRelease(): GithubReleaseDto {
        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return client.get("${sanitizedBaseUrl}repos/suryamudti/rafiq-android/releases/latest") {
            header("Accept", "application/vnd.github.v3+json")
        }.body()
    }
}
