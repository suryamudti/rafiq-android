package com.smiledev.rafiq

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Dashboard : NavKey
@Serializable data object Quran : NavKey
@Serializable data class Ayah(val suraNumber: Int, val suraName: String) : NavKey
@Serializable data object PrayerTimes : NavKey
@Serializable data object Qibla : NavKey
@Serializable data object Mosques : NavKey
@Serializable data object Prophets : NavKey
@Serializable data class ProphetDetail(val prophetId: Int) : NavKey
@Serializable data object Recitation : NavKey
@Serializable data object IslamicCalendar : NavKey
@Serializable data object ZakatCalculator : NavKey
@Serializable data object AsmaulHusna : NavKey
@Serializable data object Tasbih : NavKey
@Serializable data object BookmarkList : NavKey
@Serializable data object PrayerLog : NavKey
