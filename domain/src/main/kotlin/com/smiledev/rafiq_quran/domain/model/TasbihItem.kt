package com.smiledev.rafiq_quran.domain.model

data class TasbihItem(
    val id: Int,
    val arabic: String,
    val transliteration: String,
    val meaningEn: String,
    val meaningId: String,
    val defaultCount: Int
)
