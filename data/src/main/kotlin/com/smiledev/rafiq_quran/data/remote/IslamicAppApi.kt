package com.smiledev.rafiq_quran.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class IslamicAppTafsirEntry(
    val resource_id: Int,
    val resource_name: String,
    val language_name: String,
    val text: String
)

data class IslamicAppVerse(
    val tafsirs: List<IslamicAppTafsirEntry>?
)

data class IslamicAppData(
    val verse: IslamicAppVerse
)

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
