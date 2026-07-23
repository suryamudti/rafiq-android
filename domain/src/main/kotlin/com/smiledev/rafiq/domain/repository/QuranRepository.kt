package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.Ayah
import com.smiledev.rafiq.domain.model.Surah

interface QuranRepository {
    fun getChapters(localeCode: String = "en"): Result<List<Surah>, AppError>
    fun getAyahsWithTranslation(suraNumber: Int, localeCode: String = "en"): Result<List<Ayah>, AppError>
}
