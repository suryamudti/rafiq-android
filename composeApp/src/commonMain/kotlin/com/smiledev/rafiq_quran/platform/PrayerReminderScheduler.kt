package com.smiledev.rafiq_quran.platform

interface PrayerReminderScheduler {
    fun scheduleDaily()
    fun scheduleNow()
    fun cancelAll()
}
