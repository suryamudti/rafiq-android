package com.smiledev.rafiq_quran.domain.util

import com.smiledev.rafiq_quran.domain.model.GregorianDate
import com.smiledev.rafiq_quran.domain.model.HijriDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HijriDateConverterTest {

    @Test
    fun `gregorianToHijri matches known anchors`() {
        assertEquals(HijriDate(1447, 1, 1), HijriDateConverter.gregorianToHijri(2025, 6, 26))
        assertEquals(HijriDate(1447, 1, 10), HijriDateConverter.gregorianToHijri(2025, 7, 5))
        assertEquals(HijriDate(1446, 7, 2), HijriDateConverter.gregorianToHijri(2025, 1, 1))
        assertEquals(HijriDate(1446, 1, 1), HijriDateConverter.gregorianToHijri(2024, 7, 7))
    }

    @Test
    fun `hijriToGregorian matches known anchors`() {
        assertEquals(GregorianDate(2025, 6, 26), HijriDateConverter.hijriToGregorian(1447, 1, 1))
        assertEquals(GregorianDate(2025, 7, 5), HijriDateConverter.hijriToGregorian(1447, 1, 10))
        assertEquals(GregorianDate(2026, 2, 17), HijriDateConverter.hijriToGregorian(1447, 9, 1))
        assertEquals(GregorianDate(2026, 3, 19), HijriDateConverter.hijriToGregorian(1447, 10, 1))
    }

    @Test
    fun `day 30 of a 30-day month maps correctly (no day-zero bug)`() {
        assertEquals(HijriDate(1447, 1, 30), HijriDateConverter.gregorianToHijri(2025, 7, 25))
        assertEquals(GregorianDate(2025, 7, 25), HijriDateConverter.hijriToGregorian(1447, 1, 30))
    }

    @Test
    fun `weekdayOf is zero based on Sunday`() {
        assertEquals(0, HijriDateConverter.weekdayOf(2025, 6, 1))
        assertEquals(2, HijriDateConverter.weekdayOf(2025, 7, 1))
        assertEquals(4, HijriDateConverter.weekdayOf(2025, 6, 26))
        assertEquals(5, HijriDateConverter.weekdayOf(2025, 8, 1))
    }

    @Test
    fun `gregorian month lengths`() {
        assertEquals(29, HijriDateConverter.daysInGregorianMonth(2024, 2))
        assertEquals(28, HijriDateConverter.daysInGregorianMonth(2025, 2))
        assertEquals(31, HijriDateConverter.daysInGregorianMonth(2025, 7))
        assertEquals(30, HijriDateConverter.daysInGregorianMonth(2025, 6))
    }

    @Test
    fun `islamic leap years and month lengths`() {
        assertTrue(HijriDateConverter.isIslamicLeapYear(1447))
        assertFalse(HijriDateConverter.isIslamicLeapYear(1446))
        assertEquals(30, HijriDateConverter.daysInHijriMonth(1447, 1))
        assertEquals(29, HijriDateConverter.daysInHijriMonth(1447, 2))
        assertEquals(30, HijriDateConverter.daysInHijriMonth(1447, 12))
        assertEquals(29, HijriDateConverter.daysInHijriMonth(1446, 12))
    }

    @Test
    fun `round trip gregorian to hijri to gregorian over a full month`() {
        for (day in 1..30) {
            val hijri = HijriDateConverter.gregorianToHijri(2025, 6, day)
            val back = HijriDateConverter.hijriToGregorian(hijri.year, hijri.month, hijri.day)
            assertEquals("round trip failed for 2025-06-$day -> $hijri", GregorianDate(2025, 6, day), back)
        }
    }

    @Test
    fun `all days of Muharram 1447 round trip`() {
        for (day in 1..30) {
            val g = HijriDateConverter.hijriToGregorian(1447, 1, day)
            val h = HijriDateConverter.gregorianToHijri(g.year, g.month, g.day)
            assertEquals(HijriDate(1447, 1, day), h)
        }
    }
}
