package com.smiledev.rafiq_quran.platform

import android.content.Context

class AndroidPrayerReminderScheduler(
    private val context: Context
) : PrayerReminderScheduler {

    override fun scheduleDaily() {
        // Android triggers via WorkManager and AlarmManager
    }

    override fun scheduleNow() {
        // Immediate schedule trigger
    }

    override fun cancelAll() {
        // Cancel alarms/workers
    }
}
