package com.smiledev.rafiq_quran

import android.app.Application
import android.content.Context
import android.os.Build
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.service.PrayerNotificationWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.osmdroid.config.Configuration

@HiltAndroidApp
class RafiqApp : Application() {
    override fun attachBaseContext(base: Context) {
        val lang = runBlocking { PreferencesManager(base).translationLanguage.first() }
        super.attachBaseContext(base.wrapLocale(lang))
    }

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = cacheDir.resolve("tiles")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PrayerNotificationWorker.createNotificationChannel(this)
        }
        PrayerNotificationWorker.schedule(this)
    }
}
