package com.smiledev.rafiq_quran.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PrayerScheduleUtilsTest {

    @Test
    fun dailyPrayerNamesContainsFiveObligatoryPrayersInOrder() {
        assertEquals(
            listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"),
            PrayerScheduleUtils.DAILY_PRAYER_NAMES
        )
    }

    @Test
    fun cleanPrayerTimeStripsTimezoneSuffixAndWhitespace() {
        assertEquals("04:32", PrayerScheduleUtils.cleanPrayerTime("04:32 (WIB)"))
        assertEquals("18:05", PrayerScheduleUtils.cleanPrayerTime("18:05"))
        assertEquals("05:10", PrayerScheduleUtils.cleanPrayerTime("05:10 +07"))
        assertEquals("", PrayerScheduleUtils.cleanPrayerTime("   "))
    }

    @Test
    fun parsePrayerTimeHourMinuteParsesValidTimes() {
        assertEquals(0 to 0, PrayerScheduleUtils.parsePrayerTimeHourMinute("00:00"))
        assertEquals(5 to 30, PrayerScheduleUtils.parsePrayerTimeHourMinute("05:30"))
        assertEquals(4 to 32, PrayerScheduleUtils.parsePrayerTimeHourMinute("04:32 (WIB)"))
        assertEquals(23 to 59, PrayerScheduleUtils.parsePrayerTimeHourMinute("23:59"))
    }

    @Test
    fun parsePrayerTimeHourMinuteRejectsInvalidTimes() {
        assertNull(PrayerScheduleUtils.parsePrayerTimeHourMinute(""))
        assertNull(PrayerScheduleUtils.parsePrayerTimeHourMinute("bad"))
        assertNull(PrayerScheduleUtils.parsePrayerTimeHourMinute("12:30:00"))
        assertNull(PrayerScheduleUtils.parsePrayerTimeHourMinute("24:00"))
        assertNull(PrayerScheduleUtils.parsePrayerTimeHourMinute("25:00"))
        assertNull(PrayerScheduleUtils.parsePrayerTimeHourMinute("-1:30"))
        assertNull(PrayerScheduleUtils.parsePrayerTimeHourMinute("12:60"))
        assertNull(PrayerScheduleUtils.parsePrayerTimeHourMinute("12:-5"))
        assertNull(PrayerScheduleUtils.parsePrayerTimeHourMinute("ab:cd"))
    }

    @Test
    fun resolvePrayerNameKeyMapsStandardAndIndonesianAliases() {
        assertEquals(PrayerNameKey.IMSAK, PrayerScheduleUtils.resolvePrayerNameKey("Imsak", isFriday = false))
        assertEquals(PrayerNameKey.FAJR, PrayerScheduleUtils.resolvePrayerNameKey("Fajr", isFriday = false))
        assertEquals(PrayerNameKey.FAJR, PrayerScheduleUtils.resolvePrayerNameKey("Subuh", isFriday = true))
        assertEquals(PrayerNameKey.SUNRISE, PrayerScheduleUtils.resolvePrayerNameKey("Sunrise", isFriday = false))
        assertEquals(PrayerNameKey.SUNRISE, PrayerScheduleUtils.resolvePrayerNameKey("Terbit", isFriday = false))
        assertEquals(PrayerNameKey.DHUHA, PrayerScheduleUtils.resolvePrayerNameKey("Dhuha", isFriday = false))
        assertEquals(PrayerNameKey.ASR, PrayerScheduleUtils.resolvePrayerNameKey("Asr", isFriday = false))
        assertEquals(PrayerNameKey.ASR, PrayerScheduleUtils.resolvePrayerNameKey("Ashar", isFriday = true))
        assertEquals(PrayerNameKey.MAGHRIB, PrayerScheduleUtils.resolvePrayerNameKey("Maghrib", isFriday = true))
        assertEquals(PrayerNameKey.ISHA, PrayerScheduleUtils.resolvePrayerNameKey("Isha", isFriday = false))
        assertEquals(PrayerNameKey.ISHA, PrayerScheduleUtils.resolvePrayerNameKey("Isya", isFriday = true))
    }

    @Test
    fun resolvePrayerNameKeyMapsDhuhrToFridayOnlyOnFriday() {
        assertEquals(PrayerNameKey.DHUHR, PrayerScheduleUtils.resolvePrayerNameKey("Dhuhr", isFriday = false))
        assertEquals(PrayerNameKey.DHUHR, PrayerScheduleUtils.resolvePrayerNameKey("Dzuhur", isFriday = false))
        assertEquals(PrayerNameKey.FRIDAY, PrayerScheduleUtils.resolvePrayerNameKey("Dhuhr", isFriday = true))
        assertEquals(PrayerNameKey.FRIDAY, PrayerScheduleUtils.resolvePrayerNameKey("Dzuhur", isFriday = true))
        assertEquals(PrayerNameKey.FRIDAY, PrayerScheduleUtils.resolvePrayerNameKey(" dhuhr ", isFriday = true))
    }

    @Test
    fun resolvePrayerNameKeyMapsExplicitFridayNamesRegardlessOfDay() {
        val fridayAliases = listOf(
            "Friday",
            "Friday prayer",
            "friday_prayer",
            "Jumat",
            "Jum'at",
            "Jumuah",
            "Jumu'ah"
        )
        for (alias in fridayAliases) {
            assertEquals(PrayerNameKey.FRIDAY, PrayerScheduleUtils.resolvePrayerNameKey(alias, isFriday = false))
            assertEquals(PrayerNameKey.FRIDAY, PrayerScheduleUtils.resolvePrayerNameKey(alias, isFriday = true))
        }
    }

    @Test
    fun resolvePrayerNameKeyReturnsNullForUnknownNames() {
        assertNull(PrayerScheduleUtils.resolvePrayerNameKey("Midnight", isFriday = false))
        assertNull(PrayerScheduleUtils.resolvePrayerNameKey("", isFriday = true))
    }
}
