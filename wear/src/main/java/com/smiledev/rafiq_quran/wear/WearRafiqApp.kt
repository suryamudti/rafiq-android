package com.smiledev.rafiq_quran.wear

import android.app.Application
import android.os.Build
import com.smiledev.rafiq_quran.wear.location.WearLocationSync
import com.smiledev.rafiq_quran.wear.service.WearPrayerNotificationWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WearRafiqApp : Application() {

    @Inject
    lateinit var wearLocationSync: WearLocationSync

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WearPrayerNotificationWorker.createNotificationChannel(this@WearRafiqApp)
            }
            wearLocationSync.syncIfPermitted()
            WearPrayerNotificationWorker.schedule(this@WearRafiqApp)
        }
    }
}
