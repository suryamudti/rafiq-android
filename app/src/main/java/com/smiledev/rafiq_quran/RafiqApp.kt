package com.smiledev.rafiq_quran

import android.app.Application
import android.content.Context
import android.os.Build
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.service.PrayerNotificationWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

@HiltAndroidApp
class RafiqApp : Application() {
    override fun attachBaseContext(base: Context) {
        val wrapped = try {
            val lang = runBlocking {
                withTimeoutOrNull(1000) {
                    PreferencesManager(base).translationLanguage.first()
                } ?: "system"
            }
            base.wrapLocale(lang)
        } catch (_: Exception) {
            base
        }
        super.attachBaseContext(wrapped)
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PrayerNotificationWorker.createNotificationChannel(this)
        }
        PrayerNotificationWorker.schedule(this)
    }
}
