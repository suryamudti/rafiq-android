package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import com.google.gson.Gson
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.asSuccess
import com.smiledev.rafiq_quran.domain.model.TasbihItem
import com.smiledev.rafiq_quran.domain.repository.TasbihRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasbihRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TasbihRepository {
    private val gson = Gson()
    private var cachedItems: List<TasbihItem>? = null

    override fun getTasbihItems(): Result<List<TasbihItem>, AppError> {
        cachedItems?.let { return it.asSuccess() }
        return try {
            val json = context.assets.open("quran-data/dzikir/tasbih.json").bufferedReader().use { it.readText() }
            val response = gson.fromJson(json, TasbihResponse::class.java)
            val items = response?.data?.map {
                TasbihItem(
                    id = it.id,
                    arabic = it.arabic,
                    transliteration = it.transliteration,
                    meaningEn = it.meaning_en,
                    meaningId = it.meaning_id,
                    defaultCount = it.default_count
                )
            } ?: emptyList()
            cachedItems = items
            items.asSuccess()
        } catch (e: Throwable) {
            Result.Error(AppError.Database("Failed to load Tasbih data", e))
        }
    }
}

private data class TasbihResponse(
    val data: List<TasbihItemRaw>
)

private data class TasbihItemRaw(
    val id: Int,
    val arabic: String,
    val transliteration: String,
    val meaning_en: String,
    val meaning_id: String,
    val default_count: Int
)
