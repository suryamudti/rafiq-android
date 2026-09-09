package com.smiledev.rafiq_quran.domain.usecase

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceItem
import com.smiledev.rafiq_quran.domain.repository.PrayerGuidanceRepository

class GetPrayerGuidanceUseCase(
    private val repository: PrayerGuidanceRepository
) {
    operator fun invoke(): Result<List<PrayerGuidanceItem>, AppError> {
        return repository.getGuidanceList()
    }

    fun getById(id: String): Result<PrayerGuidanceItem, AppError> {
        return repository.getGuidanceById(id)
    }
}
