package com.smiledev.rafiq.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

data class PrayerTimings(
    val Imsak: String,
    val Fajr: String,
    val Sunrise: String,
    val Dhuhr: String,
    val Asr: String,
    val Maghrib: String,
    val Isha: String
)

data class PrayerTimesData(
    val timings: PrayerTimings,
    val date: Map<String, Any>?
)

data class PrayerTimesResponse(
    val code: Int,
    val status: String,
    val data: PrayerTimesData
)

interface AladhanApiService {
    @GET("v1/timings/{date}")
    suspend fun getTimings(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 20
    ): PrayerTimesResponse
}

@Singleton
class AladhanApi @Inject constructor() {
    private val service: AladhanApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        service = retrofit.create(AladhanApiService::class.java)
    }

    suspend fun fetchPrayerTimes(
        latitude: Double,
        longitude: Double,
        date: String
    ): PrayerTimesResponse {
        return service.getTimings(date, latitude, longitude)
    }
}
