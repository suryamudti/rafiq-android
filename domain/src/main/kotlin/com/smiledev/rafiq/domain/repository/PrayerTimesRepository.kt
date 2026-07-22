package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.PrayerTimesData

interface PrayerTimesRepository {
    suspend fun fetchPrayerTimes(lat: Double, lon: Double, date: String, method: Int = 20): Result<PrayerTimesData, AppError>
}
