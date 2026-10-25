package com.smiledev.rafiq_quran.domain.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.SunnahCategory
import com.smiledev.rafiq_quran.domain.model.SunnahGuidanceItem

interface SunnahGuidanceRepository {
    fun getSunnahList(): Result<List<SunnahGuidanceItem>, AppError>
    fun getSunnahById(id: String): Result<SunnahGuidanceItem, AppError>
    fun getSunnahByCategory(category: SunnahCategory): Result<List<SunnahGuidanceItem>, AppError>
}
