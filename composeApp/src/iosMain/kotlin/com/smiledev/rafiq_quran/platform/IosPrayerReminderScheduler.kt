package com.smiledev.rafiq_quran.platform

class IosPrayerReminderScheduler : PrayerReminderScheduler {
    override fun scheduleDaily() {
        // iOS UNUserNotificationCenter with UNCalendarNotificationTrigger
    }

    override fun scheduleNow() {
        // Immediate UNNotificationRequest
    }

    override fun cancelAll() {
        // iOS UNUserNotificationCenter.currentNotificationCenter().removeAllPendingNotificationRequests()
    }
}
