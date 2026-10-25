package com.smiledev.rafiq_quran.domain.model

data class ProphetStory(
    val id: Int,
    val nameArabic: String,
    val nameEn: String,
    val nameId: String,
    val summaryEn: String,
    val summaryId: String,
    val storyEn: String,
    val storyId: String,
    val miraclesEn: String,
    val miraclesId: String,
    val eraEn: String = "",
    val eraId: String = "",
    val peopleEn: String = "",
    val peopleId: String = "",
    val lifespanEn: String = "",
    val lifespanId: String = "",
    val eventsEn: List<String> = emptyList(),
    val eventsId: List<String> = emptyList(),
    val lessonsEn: List<String> = emptyList(),
    val lessonsId: List<String> = emptyList(),
    val verses: List<VerseRef> = emptyList()
)
