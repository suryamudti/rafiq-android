package com.smiledev.rafiq_quran.tv.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

class TvPrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Prayer"
        val time = intent.getStringExtra("time") ?: ""
        val cal = Calendar.getInstance()
        TvPrayerNotificationWorker.postPrayerNotification(context, name, time, cal)
    }
}
