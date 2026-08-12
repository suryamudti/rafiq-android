package com.smiledev.rafiq.domain.model

data class Hadith(
    val id: Int,
    val bookId: String,
    val inBookNumber: Int,
    val narratorAr: String?,
    val narratorEn: String?,
    val textAr: String,
    val textEn: String,
    val textId: String
)