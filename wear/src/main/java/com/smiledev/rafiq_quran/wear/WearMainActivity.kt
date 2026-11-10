package com.smiledev.rafiq_quran.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.smiledev.rafiq_quran.wear.navigation.WearNavigation
import com.smiledev.rafiq_quran.wear.navigation.WearRoutes
import com.smiledev.rafiq_quran.wear.theme.RafiqWearTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF08121E.toInt()
        window.navigationBarColor = 0xFF08121E.toInt()
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
}
