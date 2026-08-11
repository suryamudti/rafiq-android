package com.smiledev.rafiq.domain.model

data class VerseRef(
    val surah: Int,
    val surahNameEn: String,
    val surahNameId: String,
    val ayahStart: Int,
    val ayahEnd: Int
)
