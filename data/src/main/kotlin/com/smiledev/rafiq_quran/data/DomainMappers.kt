package com.smiledev.rafiq.data

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.models.AyahData
import com.smiledev.rafiq.data.remote.PrayerTimesData as DataPrayerTimesData
import com.smiledev.rafiq.data.remote.PrayerTimings as DataPrayerTimings
import com.smiledev.rafiq.domain.model.Ayah
import com.smiledev.rafiq.domain.model.PrayerTimings
import com.smiledev.rafiq.domain.model.PrayerTimesData
import com.smiledev.rafiq.domain.model.Surah

fun AyahData.toDomain() = Ayah(
    sura = sura, aya = aya, text = text, bismillah = bismillah,
    translation = translation, translationId = translationId, translationEn = translationEn,
    page = page, juz = juz, sajda = sajda, sajdaType = sajdaType,
    isFirstAyaOfJuz = isFirstAyaOfJuz, isFirstAyaOfPage = isFirstAyaOfPage
)

fun DataPrayerTimings.toDomain() = PrayerTimings(
    imsak = Imsak, fajr = Fajr, sunrise = Sunrise,
    dhuhr = Dhuhr, asr = Asr, maghrib = Maghrib, isha = Isha
)

fun DataPrayerTimesData.toDomain(hijriDate: String) = PrayerTimesData(
    timings = timings.toDomain(), hijriDate = hijriDate
)

fun <T> T.asSuccess(): Result<T, AppError> = Result.Success(this)
fun AppError.asError(): Result<Nothing, AppError> = Result.Error(this)
