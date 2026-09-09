package com.smiledev.rafiq_quran.domain.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceItem

interface PrayerGuidanceRepository {
    fun getGuidanceList(): Result<List<PrayerGuidanceItem>, AppError>
    fun getGuidanceById(id: String): Result<PrayerGuidanceItem, AppError>
}
