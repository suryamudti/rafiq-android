package com.smiledev.rafiq_quran.domain.usecase

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.AsmaulHusna
import com.smiledev.rafiq_quran.domain.repository.AsmaulHusnaRepository

class GetAsmaulHusnaUseCase(
    private val asmaulHusnaRepository: AsmaulHusnaRepository
) {
    operator fun invoke(): Result<List<AsmaulHusna>, AppError> {
        return asmaulHusnaRepository.getNames()
    }
}
