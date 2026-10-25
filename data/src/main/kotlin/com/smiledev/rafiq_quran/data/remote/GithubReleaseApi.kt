package com.smiledev.rafiq_quran.data.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class GithubAssetDto(
    @SerializedName("name") @SerialName("name") val name: String,
    @SerializedName("browser_download_url") @SerialName("browser_download_url") val browserDownloadUrl: String,
    @SerializedName("content_type") @SerialName("content_type") val contentType: String? = null,
    @SerializedName("size") @SerialName("size") val size: Long? = null
)

@Serializable
data class GithubReleaseDto(
    @SerializedName("tag_name") @SerialName("tag_name") val tagName: String,
    @SerializedName("name") @SerialName("name") val name: String? = null,
    @SerializedName("body") @SerialName("body") val body: String? = null,
    @SerializedName("html_url") @SerialName("html_url") val htmlUrl: String,
    @SerializedName("published_at") @SerialName("published_at") val publishedAt: String? = null,
    @SerializedName("assets") @SerialName("assets") val assets: List<GithubAssetDto>? = null
)

interface GithubReleaseApiService {
    @GET("repos/suryamudti/rafiq-android/releases/latest")
    suspend fun getLatestRelease(): GithubReleaseDto
}
