package com.smiledev.rafiq_quran.domain.util

enum class PrayerNameKey {
    IMSAK,
    FAJR,
    SUNRISE,
    DHUHA,
    DHUHR,
    FRIDAY,
    ASR,
    MAGHRIB,
    ISHA
}

object PrayerScheduleUtils {
    val DAILY_PRAYER_NAMES: List<String> = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    fun cleanPrayerTime(raw: String): String = raw.substringBefore(" ").trim()

    fun parsePrayerTimeHourMinute(raw: String): Pair<Int, Int>? {
        val clean = cleanPrayerTime(raw)
        val parts = clean.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    fun resolvePrayerNameKey(name: String, isFriday: Boolean): PrayerNameKey? {
        return when (name.trim().lowercase()) {
            "imsak" -> PrayerNameKey.IMSAK
            "fajr", "subuh" -> PrayerNameKey.FAJR
            "sunrise", "terbit" -> PrayerNameKey.SUNRISE
            "dhuha" -> PrayerNameKey.DHUHA
            "dhuhr", "dzuhur" -> if (isFriday) PrayerNameKey.FRIDAY else PrayerNameKey.DHUHR
            "friday", "friday prayer", "friday_prayer", "jumat", "jum'at", "jumuah", "jumu'ah" ->
                PrayerNameKey.FRIDAY
            "asr", "ashar" -> PrayerNameKey.ASR
            "maghrib" -> PrayerNameKey.MAGHRIB
            "isha", "isya" -> PrayerNameKey.ISHA
            else -> null
        }
    }
}
