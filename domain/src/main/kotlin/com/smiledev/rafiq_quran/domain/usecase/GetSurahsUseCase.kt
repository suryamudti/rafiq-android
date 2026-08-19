package com.smiledev.rafiq_quran.domain.usecase

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.Surah
import com.smiledev.rafiq_quran.domain.repository.QuranRepository

class GetSurahsUseCase(
    private val quranRepository: QuranRepository
) {
    operator fun invoke(localeCode: String): Result<List<Surah>, AppError> {
        return quranRepository.getChapters(localeCode)
    }
}
