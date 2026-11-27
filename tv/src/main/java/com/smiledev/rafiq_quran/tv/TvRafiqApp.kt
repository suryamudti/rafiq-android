package com.smiledev.rafiq_quran.tv

import android.app.Application
import android.os.Build
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.tv.service.TvPrayerNotificationWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TvRafiqApp : Application() {

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
                TvPrayerNotificationWorker.createNotificationChannel(this@TvRafiqApp)
            }
            TvPrayerNotificationWorker.schedule(this@TvRafiqApp)
        }
    }
}
