package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.PrayerTimesData
import com.smiledev.rafiq.domain.repository.PrayerTimesRepository

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
