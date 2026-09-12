package com.smiledev.rafiq_quran.data.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.core.retryIO
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.data.remote.AladhanApi
import com.smiledev.rafiq_quran.data.toDomain
import com.smiledev.rafiq_quran.domain.model.PrayerTimesData
import com.smiledev.rafiq_quran.domain.repository.PrayerTimesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerTimesRepositoryImpl @Inject constructor(
    private val aladhanApi: AladhanApi,
    private val preferencesManager: PreferencesManager
) : PrayerTimesRepository {

    override suspend fun getCachedPrayerTimes(
        lat: Double,
        lon: Double,
        date: String,
        method: Int
    ): PrayerTimesData? {
        val cached = preferencesManager.getCachedPrayerTimes() ?: return null
        return if (cached.isValidFor(date, lat, lon, method)) {
            cached.data
        } else {
            null
        }
    }

    override suspend fun fetchPrayerTimes(
        lat: Double,
        lon: Double,
        date: String,
        method: Int,
        forceRefresh: Boolean
    ): Result<PrayerTimesData, AppError> {
        if (!forceRefresh) {
            val cachedData = getCachedPrayerTimes(lat, lon, date, method)
            if (cachedData != null) {
                return Result.Success(cachedData)
            }
        }

        val networkResult = retryIO {
            try {
                val response = aladhanApi.fetchPrayerTimes(lat, lon, date, method)
                if (response.code == 200) {
                    val hijriDate = extractHijriDate(response.data.date)
                    val domainData = response.data.toDomain(hijriDate)
                    preferencesManager.saveCachedPrayerTimes(date, lat, lon, method, domainData)
                    Result.Success(domainData)
                } else {
                    Result.Error(AppError.Network("API error: ${response.status}", null))
                }
            } catch (e: Exception) {
                Result.Error(AppError.Network("Failed to fetch prayer times", e))
            }
        }

        return when (networkResult) {
            is Result.Success -> networkResult
            is Result.Error -> {
                val cached = preferencesManager.getCachedPrayerTimes()
                if (cached != null && cached.date == date) {
                    Result.Success(cached.data)
                } else {
                    networkResult
                }
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
