package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.Ayah
import com.smiledev.rafiq.domain.repository.QuranRepository

class GetAyahsWithTranslationUseCase(
    private val quranRepository: QuranRepository
) {
    operator fun invoke(suraNumber: Int, localeCode: String): Result<List<Ayah>, AppError> {
        return quranRepository.getAyahsWithTranslation(suraNumber, localeCode)
    }
}
