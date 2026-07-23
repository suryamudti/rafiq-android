package com.smiledev.rafiq

import android.app.Application
import com.smiledev.rafiq.service.PrayerNotificationWorker
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class RafiqApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = cacheDir.resolve("tiles")
        }
        PrayerNotificationWorker.createNotificationChannel(this)
        PrayerNotificationWorker.schedule(this)
    }
}
