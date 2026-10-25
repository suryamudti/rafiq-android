package com.smiledev.rafiq_quran.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PrayerTimings(
    val Imsak: String,
    val Fajr: String,
    val Sunrise: String,
    val Dhuhr: String,
    val Asr: String,
    val Maghrib: String,
    val Isha: String
)

@Serializable
data class HijriMonthDto(
    val en: String = ""
)

@Serializable
data class HijriDateDto(
    val day: String = "",
    val month: HijriMonthDto? = null,
    val year: String = ""
)

@Serializable
data class AladhanDateDto(
    val hijri: HijriDateDto? = null
)

@Serializable
data class PrayerTimesData(
    val timings: PrayerTimings,
    val date: AladhanDateDto? = null
)

@Serializable
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
class AladhanApi @Inject constructor(
    private val service: AladhanApiService
) {
    suspend fun fetchPrayerTimes(
        latitude: Double,
        longitude: Double,
        date: String,
        method: Int = 20
    ): PrayerTimesResponse {
        return service.getTimings(date, latitude, longitude, method)
    }
}
