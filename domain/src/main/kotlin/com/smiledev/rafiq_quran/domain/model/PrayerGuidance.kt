package com.smiledev.rafiq_quran.domain.model

enum class PrayerGuidanceCategory {
    OBLIGATORY,
    SUNNAH,
    PURIFICATION,
    POST_PRAYER
}

data class PrayerStep(
    val order: Int,
    val titleEn: String,
    val titleId: String,
    val descriptionEn: String,
    val descriptionId: String,
    val arabic: String? = null,
    val transliteration: String? = null,
    val translationEn: String? = null,
    val translationId: String? = null
)

data class PrayerGuidanceItem(
    val id: String,
    val nameEn: String,
    val nameId: String,
    val nameArabic: String,
    val category: PrayerGuidanceCategory,
    val rakaat: Int? = null,
    val descriptionEn: String,
    val descriptionId: String,
    val niyyahArabic: String? = null,
    val niyyahTransliteration: String? = null,
    val niyyahTranslationEn: String? = null,
    val niyyahTranslationId: String? = null,
    val steps: List<PrayerStep> = emptyList(),
    val tipsEn: List<String> = emptyList(),
    val tipsId: List<String> = emptyList()
)
