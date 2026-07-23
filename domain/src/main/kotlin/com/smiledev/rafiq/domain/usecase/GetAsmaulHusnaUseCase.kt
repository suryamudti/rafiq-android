package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.AsmaulHusna
import com.smiledev.rafiq.domain.repository.AsmaulHusnaRepository

class GetAsmaulHusnaUseCase(
    private val asmaulHusnaRepository: AsmaulHusnaRepository
) {
    operator fun invoke(): Result<List<AsmaulHusna>, AppError> {
        return asmaulHusnaRepository.getNames()
    }
}
