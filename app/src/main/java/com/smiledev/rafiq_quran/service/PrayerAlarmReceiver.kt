package com.smiledev.rafiq_quran.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import java.util.Calendar

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Prayer"
        val time = intent.getStringExtra("time") ?: ""
        val cal = Calendar.getInstance()
        if (intent.hasExtra("isFriday")) {
            val isFriday = intent.getBooleanExtra("isFriday", false)
            if (isFriday) {
                cal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
            } else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                cal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY)
            }
        }
        PrayerNotificationWorker.postPrayerNotification(context, name, time, cal)
    }
}
