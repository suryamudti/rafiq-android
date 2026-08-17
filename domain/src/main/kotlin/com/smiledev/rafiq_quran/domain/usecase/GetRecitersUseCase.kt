package com.smiledev.rafiq_quran.domain.usecase

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.Reciter
import com.smiledev.rafiq_quran.domain.repository.ReciterRepository

class GetRecitersUseCase(
    private val reciterRepository: ReciterRepository
) {
    operator fun invoke(): Result<List<Reciter>, AppError> {
        return reciterRepository.getReciters()
    }
}
