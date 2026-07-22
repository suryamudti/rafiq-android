package com.smiledev.rafiq.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smiledev.rafiq.data.models.AsmaulHusna
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AsmaulHusnaRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    fun getNames(): List<AsmaulHusna> {
        val json = context.assets.open("quran-data/asmaul_husna.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<AsmaulHusnaRaw>>() {}.type
        val raw: List<AsmaulHusnaRaw> = gson.fromJson(json, type)
        return raw.map {
            AsmaulHusna(
                id = it.id,
                arabic = it.arabic,
                transliteration = it.transliteration,
                meaningEn = it.meaning_en,
                meaningId = it.meaning_id,
                benefitEn = it.benefit_en,
                benefitId = it.benefit_id
            )
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
