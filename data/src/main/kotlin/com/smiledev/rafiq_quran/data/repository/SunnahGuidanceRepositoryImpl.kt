package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.asSuccess
import com.smiledev.rafiq_quran.domain.model.SunnahCategory
import com.smiledev.rafiq_quran.domain.model.SunnahGuidanceItem
import com.smiledev.rafiq_quran.domain.model.SunnahStep
import com.smiledev.rafiq_quran.domain.repository.SunnahGuidanceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SunnahGuidanceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SunnahGuidanceRepository {
    private val gson = Gson()
    private var cachedItems: List<SunnahGuidanceItem>? = null

    override fun getSunnahList(): Result<List<SunnahGuidanceItem>, AppError> {
        cachedItems?.let { return it.asSuccess() }
        return try {
            val json = context.assets.open("quran-data/sunnah_guidance.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<SunnahGuidanceRaw>>() {}.type
            val raw: List<SunnahGuidanceRaw>? = gson.fromJson(json, type)
            val items = raw?.map { it.toDomain() } ?: emptyList()
            cachedItems = items
            items.asSuccess()
        } catch (e: Throwable) {
            Result.Error(AppError.Database("Failed to load sunnah guidance", e))
        }
    }

    override fun getSunnahById(id: String): Result<SunnahGuidanceItem, AppError> {
        return when (val listResult = getSunnahList()) {
            is Result.Success -> {
                val item = listResult.data.find { it.id == id }
                if (item != null) {
                    item.asSuccess()
                } else {
                    Result.Error(AppError.NotFound)
                }
            }
            is Result.Error -> Result.Error(listResult.error)
        }
    }

    override fun getSunnahByCategory(category: SunnahCategory): Result<List<SunnahGuidanceItem>, AppError> {
        return when (val listResult = getSunnahList()) {
            is Result.Success -> {
                listResult.data.filter { it.category == category }.asSuccess()
            }
            is Result.Error -> Result.Error(listResult.error)
        }
    }
}

internal data class SunnahGuidanceRaw(
    @SerializedName("id") val id: String?,
    @SerializedName("titleEn") val titleEn: String?,
    @SerializedName("titleId") val titleId: String?,
    @SerializedName("titleArabic") val titleArabic: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("summaryEn") val summaryEn: String?,
    @SerializedName("summaryId") val summaryId: String?,
    @SerializedName("descriptionEn") val descriptionEn: String?,
    @SerializedName("descriptionId") val descriptionId: String?,
    @SerializedName("hadithReference") val hadithReference: String?,
    @SerializedName("surahReference") val surahReference: String?,
    @SerializedName("dalilArabic") val dalilArabic: String?,
    @SerializedName("dalilTranslationEn") val dalilTranslationEn: String?,
    @SerializedName("dalilTranslationId") val dalilTranslationId: String?,
    @SerializedName("virtuesEn") val virtuesEn: List<String>?,
    @SerializedName("virtuesId") val virtuesId: List<String>?,
    @SerializedName("steps") val steps: List<SunnahStepRaw>?,
    @SerializedName("tipsEn") val tipsEn: List<String>?,
    @SerializedName("tipsId") val tipsId: List<String>?
) {
    fun toDomain(): SunnahGuidanceItem {
        val cat = try {
            if (category != null) SunnahCategory.valueOf(category) else SunnahCategory.PRAYER
        } catch (e: Exception) {
            SunnahCategory.PRAYER
        }
        return SunnahGuidanceItem(
            id = id ?: "",
            titleEn = titleEn ?: "",
            titleId = titleId ?: "",
            titleArabic = titleArabic ?: "",
            category = cat,
            summaryEn = summaryEn ?: "",
            summaryId = summaryId ?: "",
            descriptionEn = descriptionEn ?: "",
            descriptionId = descriptionId ?: "",
            hadithReference = hadithReference,
            surahReference = surahReference,
            dalilArabic = dalilArabic,
            dalilTranslationEn = dalilTranslationEn,
            dalilTranslationId = dalilTranslationId,
            virtuesEn = virtuesEn ?: emptyList(),
            virtuesId = virtuesId ?: emptyList(),
            steps = steps?.map { it.toDomain() } ?: emptyList(),
            tipsEn = tipsEn ?: emptyList(),
            tipsId = tipsId ?: emptyList()
        )
    }
}

internal data class SunnahStepRaw(
    @SerializedName("order") val order: Int?,
    @SerializedName("titleEn") val titleEn: String?,
    @SerializedName("titleId") val titleId: String?,
    @SerializedName("descriptionEn") val descriptionEn: String?,
    @SerializedName("descriptionId") val descriptionId: String?,
    @SerializedName("arabic") val arabic: String?,
    @SerializedName("transliteration") val transliteration: String?,
    @SerializedName("translationEn") val translationEn: String?,
    @SerializedName("translationId") val translationId: String?,
    @SerializedName("hadithReference") val hadithReference: String?,
    @SerializedName("surahReference") val surahReference: String?
) {
    fun toDomain(): SunnahStep {
        return SunnahStep(
            order = order ?: 0,
            titleEn = titleEn ?: "",
            titleId = titleId ?: "",
            descriptionEn = descriptionEn ?: "",
            descriptionId = descriptionId ?: "",
            arabic = arabic,
            transliteration = transliteration,
            translationEn = translationEn,
            translationId = translationId,
            hadithReference = hadithReference,
            surahReference = surahReference
        )
    }
}
