package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.ProphetStory
import com.smiledev.rafiq.domain.repository.ProphetRepository

class GetProphetsUseCase(
    private val prophetRepository: ProphetRepository
) {
    operator fun invoke(): Result<List<ProphetStory>, AppError> {
        return prophetRepository.getProphets()
    }
}
