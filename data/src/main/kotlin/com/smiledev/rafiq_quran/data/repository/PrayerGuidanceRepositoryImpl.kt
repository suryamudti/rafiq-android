package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.asSuccess
import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceCategory
import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceItem
import com.smiledev.rafiq_quran.domain.model.PrayerStep
import com.smiledev.rafiq_quran.domain.repository.PrayerGuidanceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerGuidanceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PrayerGuidanceRepository {
    private val gson = Gson()
    private var cachedItems: List<PrayerGuidanceItem>? = null

    override fun getGuidanceList(): Result<List<PrayerGuidanceItem>, AppError> {
        cachedItems?.let { return it.asSuccess() }
        return try {
            val json = context.assets.open("quran-data/prayer_guidance.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<PrayerGuidanceRaw>>() {}.type
            val raw: List<PrayerGuidanceRaw>? = gson.fromJson(json, type)
            val items = raw?.map { it.toDomain() } ?: emptyList()
            cachedItems = items
            items.asSuccess()
        } catch (e: Throwable) {
            Result.Error(AppError.Database("Failed to load prayer guidance", e))
        }
    }

    override fun getGuidanceById(id: String): Result<PrayerGuidanceItem, AppError> {
        return when (val listResult = getGuidanceList()) {
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
}

internal data class PrayerGuidanceRaw(
    @SerializedName("id") val id: String?,
    @SerializedName("nameEn") val nameEn: String?,
    @SerializedName("nameId") val nameId: String?,
    @SerializedName("nameArabic") val nameArabic: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("rakaat") val rakaat: Int?,
    @SerializedName("descriptionEn") val descriptionEn: String?,
    @SerializedName("descriptionId") val descriptionId: String?,
    @SerializedName("niyyahArabic") val niyyahArabic: String?,
    @SerializedName("niyyahTransliteration") val niyyahTransliteration: String?,
    @SerializedName("niyyahTranslationEn") val niyyahTranslationEn: String?,
    @SerializedName("niyyahTranslationId") val niyyahTranslationId: String?,
    @SerializedName("steps") val steps: List<PrayerStepRaw>?,
    @SerializedName("tipsEn") val tipsEn: List<String>?,
    @SerializedName("tipsId") val tipsId: List<String>?
) {
    fun toDomain(): PrayerGuidanceItem {
        val cat = try {
            if (category != null) PrayerGuidanceCategory.valueOf(category) else PrayerGuidanceCategory.OBLIGATORY
        } catch (e: Exception) {
            PrayerGuidanceCategory.OBLIGATORY
        }
        return PrayerGuidanceItem(
            id = id ?: "",
            nameEn = nameEn ?: "",
            nameId = nameId ?: "",
            nameArabic = nameArabic ?: "",
            category = cat,
            rakaat = rakaat,
            descriptionEn = descriptionEn ?: "",
            descriptionId = descriptionId ?: "",
            niyyahArabic = niyyahArabic,
            niyyahTransliteration = niyyahTransliteration,
            niyyahTranslationEn = niyyahTranslationEn,
            niyyahTranslationId = niyyahTranslationId,
            steps = steps?.map { it.toDomain() } ?: emptyList(),
            tipsEn = tipsEn ?: emptyList(),
            tipsId = tipsId ?: emptyList()
        )
    }
}

internal data class PrayerStepRaw(
    @SerializedName("order") val order: Int?,
    @SerializedName("titleEn") val titleEn: String?,
    @SerializedName("titleId") val titleId: String?,
    @SerializedName("descriptionEn") val descriptionEn: String?,
    @SerializedName("descriptionId") val descriptionId: String?,
    @SerializedName("arabic") val arabic: String?,
    @SerializedName("transliteration") val transliteration: String?,
    @SerializedName("translationEn") val translationEn: String?,
    @SerializedName("translationId") val translationId: String?
) {
    fun toDomain(): PrayerStep {
        return PrayerStep(
            order = order ?: 0,
            titleEn = titleEn ?: "",
            titleId = titleId ?: "",
            descriptionEn = descriptionEn ?: "",
            descriptionId = descriptionId ?: "",
            arabic = arabic,
            transliteration = transliteration,
            translationEn = translationEn,
            translationId = translationId
        )
    }
}
