package com.smiledev.rafiq_quran.domain.model

data class IslamicEvent(
    val hijriMonth: Int,
    val hijriDay: Int,
    val titleEn: String,
    val titleId: String,
    val descriptionEn: String,
    val descriptionId: String,
    val eventType: String,
    val weekday: Int? = null
)
