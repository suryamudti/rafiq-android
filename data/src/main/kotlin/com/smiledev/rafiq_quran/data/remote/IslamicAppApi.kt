package com.smiledev.rafiq_quran.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class IslamicAppTafsirEntry(
    val resource_id: Int,
    val resource_name: String,
    val language_name: String,
    val text: String
)

@Serializable
data class IslamicAppVerse(
    val tafsirs: List<IslamicAppTafsirEntry>?
)

@Serializable
data class IslamicAppData(
    val verse: IslamicAppVerse
)

@Serializable
data class IslamicAppResponse(
    val code: Int,
    val status: String,
    val data: IslamicAppData
)

interface IslamicAppApiService {
    @GET("v1/verses/by_key/{sura}:{aya}")
    suspend fun getVerseWithTafsir(
        @Path("sura") sura: Int,
        @Path("aya") aya: Int,
        @Query("tafsirs") tafsirs: String = "en-tafisr-ibn-kathir"
    ): IslamicAppResponse
}
