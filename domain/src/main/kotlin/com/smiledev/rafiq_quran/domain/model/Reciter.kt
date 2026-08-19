package com.smiledev.rafiq_quran.domain.model

data class Reciter(
    val id: Int,
    val nameEn: String,
    val nameAr: String,
    val style: String,
    val country: String,
    val audioBase: String
)
