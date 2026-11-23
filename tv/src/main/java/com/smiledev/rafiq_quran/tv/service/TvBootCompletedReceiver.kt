package com.smiledev.rafiq_quran.tv.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TvBootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                TvPrayerNotificationWorker.schedule(context.applicationContext)
            }
        }
    }
}
