package com.smiledev.rafiq_quran.domain.model

enum class SunnahCategory {
    PRAYER,
    DAILY_LIFESTYLE,
    FRIDAY,
    FASTING,
    DHIKR_DUA
}

data class SunnahStep(
    val order: Int,
    val titleEn: String,
    val titleId: String,
    val descriptionEn: String,
    val descriptionId: String,
    val arabic: String? = null,
    val transliteration: String? = null,
    val translationEn: String? = null,
    val translationId: String? = null,
    val hadithReference: String? = null,
    val surahReference: String? = null
)

data class SunnahGuidanceItem(
    val id: String,
    val titleEn: String,
    val titleId: String,
    val titleArabic: String,
    val category: SunnahCategory,
    val summaryEn: String,
    val summaryId: String,
    val descriptionEn: String,
    val descriptionId: String,
    val hadithReference: String? = null,
    val surahReference: String? = null,
    val dalilArabic: String? = null,
    val dalilTranslationEn: String? = null,
    val dalilTranslationId: String? = null,
    val virtuesEn: List<String> = emptyList(),
    val virtuesId: List<String> = emptyList(),
    val steps: List<SunnahStep> = emptyList(),
    val tipsEn: List<String> = emptyList(),
    val tipsId: List<String> = emptyList()
)
