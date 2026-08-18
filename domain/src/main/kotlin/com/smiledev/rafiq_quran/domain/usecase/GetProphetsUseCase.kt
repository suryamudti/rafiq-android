package com.smiledev.rafiq_quran.domain.usecase

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.ProphetStory
import com.smiledev.rafiq_quran.domain.repository.ProphetRepository

class GetProphetsUseCase(
    private val prophetRepository: ProphetRepository
) {
    operator fun invoke(): Result<List<ProphetStory>, AppError> {
        return prophetRepository.getProphets()
    }
}
