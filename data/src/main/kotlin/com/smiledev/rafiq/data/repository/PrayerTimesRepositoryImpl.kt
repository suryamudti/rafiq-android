package com.smiledev.rafiq.data.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.retryIO
import com.smiledev.rafiq.data.remote.AladhanApi
import com.smiledev.rafiq.data.toDomain
import com.smiledev.rafiq.domain.model.PrayerTimesData
import com.smiledev.rafiq.domain.repository.PrayerTimesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerTimesRepositoryImpl @Inject constructor(
    private val aladhanApi: AladhanApi
) : PrayerTimesRepository {
    override suspend fun fetchPrayerTimes(lat: Double, lon: Double, date: String, method: Int): Result<PrayerTimesData, AppError> {
        return retryIO {
            try {
                val response = aladhanApi.fetchPrayerTimes(lat, lon, date, method)
                if (response.code == 200) {
                    val hijriDate = extractHijriDate(response.data.date)
                    Result.Success(response.data.toDomain(hijriDate))
                } else {
                    Result.Error(AppError.Network("API error: ${response.status}", null))
                }
            } catch (e: Exception) {
                Result.Error(AppError.Network("Failed to fetch prayer times", e))
            }
        }
    }

    private fun extractHijriDate(dateMap: Map<String, Any>?): String {
        if (dateMap == null) return ""
        val hijri = dateMap["hijri"] as? Map<*, *> ?: return ""
        val day = hijri["day"] ?: ""
        val month = (hijri["month"] as? Map<*, *>)?.get("en") ?: ""
        val year = hijri["year"] ?: ""
        return "$day $month $year"
    }
}
