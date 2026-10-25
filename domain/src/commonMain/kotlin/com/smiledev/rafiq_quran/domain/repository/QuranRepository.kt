package com.smiledev.rafiq_quran.domain.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.domain.model.Surah

interface QuranRepository {
    fun getChapters(localeCode: String = "en"): Result<List<Surah>, AppError>
    fun getAyahsWithTranslation(suraNumber: Int, localeCode: String = "en"): Result<List<Ayah>, AppError>
    fun searchAyahs(query: String, localeCode: String = "en", limit: Int = 100): Result<List<Ayah>, AppError>
}
