package com.smiledev.rafiq_quran.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

data class EQuranTafsirEntry(
    val ayat: Int,
    val teks: String
)

data class EQuranTafsirData(
    val nomor: Int,
    val nama: String,
    val namaLatin: String,
    val jumlahAyat: Int,
    val tafsir: List<EQuranTafsirEntry>
)

data class EQuranTafsirResponse(
    val code: Int,
    val message: String,
    val data: EQuranTafsirData
)

interface EQuranApiService {
    @GET("api/v2/tafsir/{surah}")
    suspend fun getTafsir(@Path("surah") surah: Int): EQuranTafsirResponse
}
