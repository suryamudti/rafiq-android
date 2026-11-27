package com.smiledev.rafiq_quran.service

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                val appContext = context.applicationContext
                PrayerNotificationWorker.schedule(appContext)
                PrayerNotificationWorker.scheduleNow(appContext)
            }
        }
    }
}
