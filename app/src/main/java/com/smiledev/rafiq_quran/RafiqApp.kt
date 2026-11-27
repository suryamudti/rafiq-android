package com.smiledev.rafiq_quran

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.di.koin.appKoinModules
import com.smiledev.rafiq_quran.service.PrayerNotificationWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import javax.inject.Inject

@HiltAndroidApp
class RafiqApp : Application() {

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

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin Multiplatform
        startKoin {
            androidContext(this@RafiqApp)
            modules(appKoinModules)
        }

        // Crashlytics: only collect in non-debuggable builds. No-op if google-services.json is absent.
        runCatching {
            val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            FirebaseCrashlytics.getInstance()
                .setCrashlyticsCollectionEnabled(!debuggable)
        }
        appScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PrayerNotificationWorker.createNotificationChannel(this@RafiqApp)
            }
            PrayerNotificationWorker.schedule(this@RafiqApp)
        }
    }
}
