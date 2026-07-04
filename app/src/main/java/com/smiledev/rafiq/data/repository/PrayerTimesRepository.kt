package com.smiledev.rafiq.data.repository

import com.smiledev.rafiq.data.remote.AladhanApi
import com.smiledev.rafiq.data.remote.PrayerTimesResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerTimesRepository @Inject constructor(
    private val aladhanApi: AladhanApi
) {
    suspend fun fetchPrayerTimes(lat: Double, lon: Double, date: String): PrayerTimesResponse {
        return aladhanApi.fetchPrayerTimes(lat, lon, date)
    }
}
