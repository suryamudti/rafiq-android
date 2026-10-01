package com.smiledev.rafiq_quran.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import java.util.Calendar

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Prayer"
        val time = intent.getStringExtra("time") ?: ""
        val cal = java.util.Calendar.getInstance()
        PrayerNotificationWorker.postPrayerNotification(context, name, time, cal)
    }
}
