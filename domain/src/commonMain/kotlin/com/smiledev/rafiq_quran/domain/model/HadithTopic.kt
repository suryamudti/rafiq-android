package com.smiledev.rafiq_quran.domain.model

data class HadithTopic(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val nameId: String,
    val bookIds: List<String>
)
