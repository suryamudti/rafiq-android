package com.smiledev.rafiq_quran.domain.usecase

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.SunnahGuidanceItem
import com.smiledev.rafiq_quran.domain.repository.SunnahGuidanceRepository

class GetSunnahGuidanceUseCase(
    private val repository: SunnahGuidanceRepository
) {
    operator fun invoke(): Result<List<SunnahGuidanceItem>, AppError> {
        return repository.getSunnahList()
    }

    fun getById(id: String): Result<SunnahGuidanceItem, AppError> {
        return repository.getSunnahById(id)
    }
}
