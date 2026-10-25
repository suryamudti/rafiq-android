package com.smiledev.rafiq_quran.domain.usecase

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.domain.repository.QuranRepository

class GetAyahsWithTranslationUseCase(
    private val quranRepository: QuranRepository
) {
    operator fun invoke(suraNumber: Int, localeCode: String): Result<List<Ayah>, AppError> {
        return quranRepository.getAyahsWithTranslation(suraNumber, localeCode)
    }
}
