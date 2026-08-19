package com.smiledev.rafiq_quran

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Dashboard : NavKey
@Serializable data class Quran(val initialTab: Int = 0) : NavKey
@Serializable data class Ayah(val suraNumber: Int, val suraName: String, val scrollToAya: Int = 0) : NavKey
@Serializable data object PrayerTimes : NavKey
@Serializable data object Qibla : NavKey
@Serializable data object Mosques : NavKey
@Serializable data object Prophets : NavKey
@Serializable data class ProphetDetail(val prophetId: Int) : NavKey
@Serializable data object HadithBooks : NavKey
@Serializable data object HadithSearch : NavKey
@Serializable data class HadithList(val bookId: String) : NavKey
@Serializable data class HadithDetail(val hadithId: Int) : NavKey
@Serializable data object Recitation : NavKey
@Serializable data object IslamicCalendar : NavKey
@Serializable data object ZakatCalculator : NavKey
@Serializable data object AsmaulHusna : NavKey
@Serializable data object Tasbih : NavKey
@Serializable data object BookmarkList : NavKey
@Serializable data object PrayerLog : NavKey

@Serializable data object Settings : NavKey
@Serializable data object Sources : NavKey
