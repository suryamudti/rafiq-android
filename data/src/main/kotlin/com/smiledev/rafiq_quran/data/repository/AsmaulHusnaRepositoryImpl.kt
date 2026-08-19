package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.asSuccess
import com.smiledev.rafiq_quran.domain.model.AsmaulHusna
import com.smiledev.rafiq_quran.domain.repository.AsmaulHusnaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AsmaulHusnaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AsmaulHusnaRepository {
    private val gson = Gson()

    override fun getNames(): Result<List<AsmaulHusna>, AppError> {
        return try {
            val json = context.assets.open("quran-data/asmaul_husna.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<AsmaulHusnaRaw>>() {}.type
            val raw: List<AsmaulHusnaRaw> = gson.fromJson(json, type)
            raw.map {
                AsmaulHusna(
                    id = it.id,
                    arabic = it.arabic,
                    transliteration = it.transliteration,
                    meaningEn = it.meaning_en,
                    meaningId = it.meaning_id,
                    benefitEn = it.benefit_en,
                    benefitId = it.benefit_id
                )
            }.asSuccess()
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to load Asmaul Husna", e))
        }
    }
}

private data class AsmaulHusnaRaw(
    val id: Int,
    val arabic: String,
    val transliteration: String,
    val meaning_en: String,
    val meaning_id: String,
    val benefit_en: String,
    val benefit_id: String
)
