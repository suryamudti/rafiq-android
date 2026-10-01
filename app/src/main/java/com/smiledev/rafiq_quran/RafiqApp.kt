package com.smiledev.rafiq_quran

import android.app.Application
import android.content.Context
import android.os.Build
import com.smiledev.rafiq_quran.service.PrayerNotificationWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch

@HiltAndroidApp
class RafiqApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO).launch {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                PrayerNotificationWorker.createNotificationChannel(this@RafiqApp)
            }
            PrayerNotificationWorker.schedule(this@RafiqApp)
        }
    }
}
