package com.smiledev.rafiq_quran.domain.usecase

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.PrayerTimesData
import com.smiledev.rafiq_quran.domain.repository.PrayerTimesRepository

class GetPrayerTimesUseCase(
    private val prayerTimesRepository: PrayerTimesRepository
) {
    suspend operator fun invoke(
        lat: Double,
        lon: Double,
        date: String,
        method: Int = 20
    ): Result<PrayerTimesData, AppError> {
        return prayerTimesRepository.fetchPrayerTimes(lat, lon, date, method)
    }
}
