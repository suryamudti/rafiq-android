package com.smiledev.rafiq.domain.model

data class Ayah(
    val sura: Int,
    val aya: Int,
    val text: String,
    val bismillah: String?,
    val translation: String? = null,
    val translationId: String? = null,
    val translationEn: String? = null,
    val page: Int = 0,
    val juz: Int = 0,
    val sajda: Boolean = false,
    val sajdaType: String? = null,
    val isFirstAyaOfJuz: Boolean = false,
    val isFirstAyaOfPage: Boolean = false
)
