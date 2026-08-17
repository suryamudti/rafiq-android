package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.Reciter
import com.smiledev.rafiq.domain.repository.ReciterRepository

class GetRecitersUseCase(
    private val reciterRepository: ReciterRepository
) {
    operator fun invoke(): Result<List<Reciter>, AppError> {
        return reciterRepository.getReciters()
    }
}
