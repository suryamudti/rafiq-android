package com.smiledev.rafiq_quran.wear.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.smiledev.rafiq_quran.wear.ui.home.WearHomeScreen
import com.smiledev.rafiq_quran.wear.ui.prayertimes.WearPrayerTimesScreen
import com.smiledev.rafiq_quran.wear.ui.qibla.WearQiblaScreen
import com.smiledev.rafiq_quran.wear.ui.quran.WearAyahReaderScreen
import com.smiledev.rafiq_quran.wear.ui.quran.WearSurahListScreen

object WearRoutes {
    const val HOME = "home"
    const val PRAYER_TIMES = "prayer_times"
    const val QURAN_SURAHS = "quran_surahs"
    const val QURAN_AYAHS = "quran_ayahs/{suraNumber}"
    const val QIBLA = "qibla"

    fun quranAyahs(suraNumber: Int): String = "quran_ayahs/$suraNumber"
}

@Composable
fun WearNavigation(
    startDestination: String = WearRoutes.HOME,
    initialSuraNumber: Int = 1
) {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(WearRoutes.HOME) {
            WearHomeScreen(
                onNavigatePrayerTimes = { navController.navigate(WearRoutes.PRAYER_TIMES) },
                onNavigateQuran = { navController.navigate(WearRoutes.QURAN_SURAHS) },
                onNavigateQibla = { navController.navigate(WearRoutes.QIBLA) }
            )
        }
        composable(WearRoutes.PRAYER_TIMES) {
            WearPrayerTimesScreen()
        }
        composable(WearRoutes.QURAN_SURAHS) {
            WearSurahListScreen(
                onSurahClick = { suraNumber ->
                    navController.navigate(WearRoutes.quranAyahs(suraNumber))
                }
            )
        }
        composable(WearRoutes.QURAN_AYAHS) { backStackEntry ->
            val suraNumber = backStackEntry.arguments?.getString("suraNumber")?.toIntOrNull() ?: initialSuraNumber
            WearAyahReaderScreen(suraNumber = suraNumber)
        }
        composable(WearRoutes.QIBLA) {
            WearQiblaScreen()
        }
    }
}
