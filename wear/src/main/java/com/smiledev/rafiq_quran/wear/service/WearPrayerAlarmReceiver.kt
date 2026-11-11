package com.smiledev.rafiq_quran.wear.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

class WearPrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Prayer"
        val time = intent.getStringExtra("time") ?: ""
        val cal = Calendar.getInstance()
        WearPrayerNotificationWorker.postPrayerNotification(context, name, time, cal)
    }
}
