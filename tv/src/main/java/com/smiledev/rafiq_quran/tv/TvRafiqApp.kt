package com.smiledev.rafiq_quran.tv

import android.app.Application
import android.os.Build
import com.smiledev.rafiq_quran.tv.service.TvPrayerNotificationWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class TvRafiqApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                TvPrayerNotificationWorker.createNotificationChannel(this@TvRafiqApp)
            }
            TvPrayerNotificationWorker.schedule(this@TvRafiqApp)
        }
    }
}
