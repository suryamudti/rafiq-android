package com.smiledev.rafiq_quran.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Prayer"
        val time = intent.getStringExtra("time") ?: ""
        PrayerNotificationWorker.postPrayerNotification(context, name, time)
    }
}
