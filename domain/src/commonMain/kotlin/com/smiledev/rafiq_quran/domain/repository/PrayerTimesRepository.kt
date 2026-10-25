package com.smiledev.rafiq_quran.domain.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.PrayerTimesData

interface PrayerTimesRepository {
    suspend fun fetchPrayerTimes(
        lat: Double,
        lon: Double,
        date: String,
        method: Int = 20,
        forceRefresh: Boolean = false
    ): Result<PrayerTimesData, AppError>

    suspend fun getCachedPrayerTimes(
        lat: Double,
        lon: Double,
        date: String,
        method: Int = 20
    ): PrayerTimesData? = null
}
