package com.smiledev.rafiq_quran.tv

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.smiledev.rafiq_quran.tv.service.TvPrayerNotificationWorker
import com.smiledev.rafiq_quran.tv.theme.RafiqTvTheme
import com.smiledev.rafiq_quran.tv.ui.ambient.TvAmbientDisplayScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TvMainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            TvPrayerNotificationWorker.scheduleNow(applicationContext)
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = 0xFF07111C.toInt()
        window.navigationBarColor = 0xFF07111C.toInt()
        requestNotificationPermissionIfNeeded()
        handleAdhanPreviewIntent(intent)

        setContent {
            RafiqTvTheme {
                TvAmbientDisplayScreen()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleAdhanPreviewIntent(intent)
    }

    private fun handleAdhanPreviewIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra("preview_adhan", false) == true) {
            val name = intent.getStringExtra("prayer_name") ?: "Maghrib"
            val time = intent.getStringExtra("prayer_time") ?: "17:56"
            TvPrayerNotificationWorker.triggerInAppAdhanAlert(name, time)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
