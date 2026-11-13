package com.smiledev.rafiq_quran.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.smiledev.rafiq_quran.wear.location.WearLocationSync
import com.smiledev.rafiq_quran.wear.navigation.WearNavigation
import com.smiledev.rafiq_quran.wear.navigation.WearRoutes
import com.smiledev.rafiq_quran.wear.service.WearPrayerNotificationWorker
import com.smiledev.rafiq_quran.wear.theme.RafiqWearTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {

    @Inject
    lateinit var wearLocationSync: WearLocationSync

    private var launchRoute by mutableStateOf(WearRoutes.HOME)
    private var launchSura by mutableIntStateOf(1)
    private var launchTrigger by mutableIntStateOf(0)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            lifecycleScope.launch(Dispatchers.IO) {
                wearLocationSync.syncIfPermitted(forceGeocode = true)
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            WearPrayerNotificationWorker.scheduleNow(applicationContext)
        }
        requestInitialLocationPermissionIfNeeded()
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF08121E.toInt()
        window.navigationBarColor = 0xFF08121E.toInt()
        applyLaunchIntent(intent)
        requestStartupPermissions()
        setContent {
            RafiqWearTheme {
                WearNavigation(
                    startDestination = launchRoute,
                    initialSuraNumber = launchSura,
                    launchTrigger = launchTrigger
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyLaunchIntent(intent)
    }

    private fun applyLaunchIntent(intent: Intent?) {
        val route = intent?.getStringExtra("start_route")
        if (!route.isNullOrBlank()) {
            launchRoute = route
            launchSura = intent.getIntExtra("sura_number", 1)
            launchTrigger++
        }
    }

    private fun requestStartupPermissions() {
        val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

        if (needsNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestInitialLocationPermissionIfNeeded()
        }
    }

    private fun requestInitialLocationPermissionIfNeeded() {
        if (wearLocationSync.hasLocationPermission()) {
            lifecycleScope.launch(Dispatchers.IO) {
                wearLocationSync.syncIfPermitted()
            }
            return
        }
        val prefs = getSharedPreferences("wear_permissions", MODE_PRIVATE)
        if (!prefs.getBoolean("asked_location_once", false)) {
            prefs.edit().putBoolean("asked_location_once", true).apply()
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}
