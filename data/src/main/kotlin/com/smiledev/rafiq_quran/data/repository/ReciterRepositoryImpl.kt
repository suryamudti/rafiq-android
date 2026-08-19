package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.asSuccess
import com.smiledev.rafiq_quran.domain.model.Reciter
import com.smiledev.rafiq_quran.domain.repository.ReciterRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReciterRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ReciterRepository {
    private val gson = Gson()

    override fun getReciters(): Result<List<Reciter>, AppError> {
        return try {
            val json = context.assets.open("quran-data/reciters.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<ReciterRaw>>() {}.type
            val raw: List<ReciterRaw> = gson.fromJson(json, type)
            raw.map {
                Reciter(
                    id = it.id,
                    nameEn = it.name_en,
                    nameAr = it.name_ar,
                    style = it.style,
                    country = it.country,
                    audioBase = it.audio_base
                )
            }.asSuccess()
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to load reciters", e))
        }
    }
}

private data class ReciterRaw(
    val id: Int,
    val name_en: String,
    val name_ar: String,
    val style: String,
    val country: String,
    val audio_base: String
)
