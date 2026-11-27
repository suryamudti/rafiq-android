package com.smiledev.rafiq_quran.wear

import android.app.Application
import android.os.Build
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.wear.location.WearLocationSync
import com.smiledev.rafiq_quran.wear.service.WearPrayerNotificationWorker
import com.smiledev.rafiq_quran.wear.widget.WearNextPrayerWidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WearRafiqApp : Application() {

    @Inject
    lateinit var wearLocationSync: WearLocationSync

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private val appScope: CoroutineScope by lazy {
        val ioDispatcher = if (::dispatcherProvider.isInitialized) {
            dispatcherProvider.io
        } else {
            DefaultDispatcherProvider.io
        }
        CoroutineScope(SupervisorJob() + ioDispatcher)
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WearPrayerNotificationWorker.createNotificationChannel(this@WearRafiqApp)
            }
            wearLocationSync.syncIfPermitted()
            WearPrayerNotificationWorker.schedule(this@WearRafiqApp)
            WearNextPrayerWidgetUpdater.requestUpdate(this@WearRafiqApp)
        }
    }
}
