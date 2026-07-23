package com.smiledev.rafiq

import android.app.Application
import android.os.Build
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PrayerNotificationWorker.createNotificationChannel(this)
        }
        PrayerNotificationWorker.schedule(this)
    }
}
