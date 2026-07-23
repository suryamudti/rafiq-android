package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.Surah
import com.smiledev.rafiq.domain.repository.QuranRepository

class GetSurahsUseCase(
    private val quranRepository: QuranRepository
) {
    operator fun invoke(localeCode: String): Result<List<Surah>, AppError> {
        return quranRepository.getChapters(localeCode)
    }
}
