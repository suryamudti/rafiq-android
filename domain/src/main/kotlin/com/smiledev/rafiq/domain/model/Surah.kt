package com.smiledev.rafiq.domain.model

data class Surah(
    val id: Int,
    val chapterNumber: Int,
    val nameArabic: String,
    val nameSimple: String,
    val translatedName: String,
    val versesCount: Int,
    val revelationPlace: String
)
