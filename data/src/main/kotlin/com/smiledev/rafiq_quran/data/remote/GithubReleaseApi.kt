package com.smiledev.rafiq_quran.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

data class GithubAssetDto(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String,
    @SerializedName("content_type") val contentType: String? = null,
    @SerializedName("size") val size: Long? = null
)

data class GithubReleaseDto(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("published_at") val publishedAt: String? = null,
    @SerializedName("assets") val assets: List<GithubAssetDto>? = null
)

interface GithubReleaseApiService {
    @GET("repos/suryamudti/rafiq-android/releases/latest")
    suspend fun getLatestRelease(): GithubReleaseDto
}
