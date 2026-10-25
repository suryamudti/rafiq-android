package com.smiledev.rafiq_quran.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class EQuranTafsirEntry(
    val ayat: Int,
    val teks: String
)

@Serializable
data class EQuranTafsirData(
    val nomor: Int,
    val nama: String,
    val namaLatin: String,
    val jumlahAyat: Int,
    val tafsir: List<EQuranTafsirEntry>
)

@Serializable
data class EQuranTafsirResponse(
    val code: Int,
    val message: String,
    val data: EQuranTafsirData
)

interface EQuranApiService {
    @GET("api/v2/tafsir/{surah}")
    suspend fun getTafsir(@Path("surah") surah: Int): EQuranTafsirResponse
}
