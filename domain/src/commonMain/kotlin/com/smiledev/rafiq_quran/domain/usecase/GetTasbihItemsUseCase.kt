package com.smiledev.rafiq_quran.domain.usecase

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.TasbihItem
import com.smiledev.rafiq_quran.domain.repository.TasbihRepository

class GetTasbihItemsUseCase(
    private val tasbihRepository: TasbihRepository
) {
    operator fun invoke(): Result<List<TasbihItem>, AppError> {
        return tasbihRepository.getTasbihItems()
    }
}
