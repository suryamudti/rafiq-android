package com.smiledev.rafiq.domain.model

data class PrayerTimings(
    val imsak: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
)

data class PrayerTimesData(
    val timings: PrayerTimings,
    val hijriDate: String? = null
)

data class PrayerTimeEntry(
    val name: String,
    val time: String
)
