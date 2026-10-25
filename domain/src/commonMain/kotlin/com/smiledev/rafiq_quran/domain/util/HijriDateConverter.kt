package com.smiledev.rafiq_quran.domain.util

import com.smiledev.rafiq_quran.domain.model.GregorianDate
import com.smiledev.rafiq_quran.domain.model.HijriDate
import kotlin.math.min

object HijriDateConverter {

    const val ISLAMIC_EPOCH = 227015L

    fun gregorianToHijri(year: Int, month: Int, day: Int): HijriDate {
        val fixed = gregorianToFixed(year, month, day)
        val hijriYear = ((30 * (fixed - ISLAMIC_EPOCH) + 10646) / 10631).toInt()
        val yearStart = islamicToFixed(hijriYear, 1, 1)
        val hijriMonth = min(12, ((fixed - yearStart) * 2) / 59 + 1).toInt()
        val hijriDay = (fixed - (islamicToFixed(hijriYear, hijriMonth, 1) - 1)).toInt()
        return HijriDate(hijriYear, hijriMonth, hijriDay)
    }

    fun hijriToGregorian(year: Int, month: Int, day: Int): GregorianDate {
        return jdnToGregorian(islamicToFixed(year, month, day) + RD_TO_JDN)
    }

    fun isIslamicLeapYear(year: Int): Boolean = (11 * year + 14) % 30 < 11

    fun daysInHijriMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 9, 11 -> 30
        12 -> if (isIslamicLeapYear(year)) 30 else 29
        else -> 29
    }

    fun daysInGregorianMonth(year: Int, month: Int): Int = when (month) {
        2 -> if (isGregorianLeapYear(year)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    fun weekdayOf(year: Int, month: Int, day: Int): Int {
        return ((gregorianToJdn(year, month, day) + 1) % 7).toInt()
    }

    private const val RD_TO_JDN = 1721424L

    private fun gregorianToFixed(year: Int, month: Int, day: Int): Long {
        return gregorianToJdn(year, month, day) - RD_TO_JDN
    }

    private fun gregorianToJdn(year: Int, month: Int, day: Int): Long {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return (day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045).toLong()
    }

    private fun islamicToFixed(year: Int, month: Int, day: Int): Long {
        return ISLAMIC_EPOCH + (year - 1) * 354L + (3 + 11L * year) / 30 +
            (59L * (month - 1) + 1) / 2 + (day - 1)
    }

    private fun jdnToGregorian(jdn: Long): GregorianDate {
        val a = jdn + 32044
        val b = (4 * a + 3) / 146097
        val c = a - (146097 * b) / 4
        val d = (4 * c + 3) / 1461
        val e = c - (1461 * d) / 4
        val m = (5 * e + 2) / 153
        val day = (e - (153 * m + 2) / 5 + 1).toInt()
        val month = (m + 3 - 12 * (m / 10)).toInt()
        val year = (100 * b + d - 4800 + m / 10).toInt()
        return GregorianDate(year, month, day)
    }

    private fun isGregorianLeapYear(year: Int): Boolean =
        year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}
