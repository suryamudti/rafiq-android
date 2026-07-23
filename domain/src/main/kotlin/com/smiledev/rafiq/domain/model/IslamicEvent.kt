package com.smiledev.rafiq.domain.model

data class IslamicEvent(
    val hijriMonth: Int,
    val hijriDay: Int,
    val titleEn: String,
    val titleId: String,
    val descriptionEn: String,
    val descriptionId: String,
    val eventType: String
)
