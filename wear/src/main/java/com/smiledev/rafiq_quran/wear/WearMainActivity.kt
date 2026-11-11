package com.smiledev.rafiq_quran.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.smiledev.rafiq_quran.wear.navigation.WearNavigation
import com.smiledev.rafiq_quran.wear.navigation.WearRoutes
import com.smiledev.rafiq_quran.wear.service.WearPrayerNotificationWorker
import com.smiledev.rafiq_quran.wear.theme.RafiqWearTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            WearPrayerNotificationWorker.scheduleNow(applicationContext)
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF08121E.toInt()
        window.navigationBarColor = 0xFF08121E.toInt()
        requestNotificationPermissionIfNeeded()
        val startRoute = intent?.getStringExtra("start_route")
        val initialSura = intent?.getIntExtra("sura_number", 1) ?: 1
        setContent {
            RafiqWearTheme {
                WearNavigation(
                    startDestination = startRoute ?: WearRoutes.HOME,
                    initialSuraNumber = initialSura
                )
            }
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
