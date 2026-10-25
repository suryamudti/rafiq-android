package com.smiledev.rafiq_quran.domain.model

data class VerseRef(
    val surah: Int,
    val surahNameEn: String,
    val surahNameId: String,
    val ayahStart: Int,
    val ayahEnd: Int
)
