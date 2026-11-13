package com.smiledev.rafiq_quran.wear.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.smiledev.rafiq_quran.wear.ui.home.WearHomeScreen
import com.smiledev.rafiq_quran.wear.ui.prayertimes.WearPrayerTimesScreen
import com.smiledev.rafiq_quran.wear.ui.qibla.WearQiblaScreen
import com.smiledev.rafiq_quran.wear.ui.quran.WearAyahReaderScreen
import com.smiledev.rafiq_quran.wear.ui.quran.WearQuranViewModel
import com.smiledev.rafiq_quran.wear.ui.quran.WearSurahListScreen

object WearRoutes {
    const val HOME = "home"
    const val PRAYER_TIMES = "prayer_times"
    const val QURAN_SURAHS = "quran_surahs"
    const val QURAN_AYAHS = "quran_ayahs/{suraNumber}?aya={ayaNumber}"
    const val QIBLA = "qibla"

    fun quranAyahs(suraNumber: Int, ayaNumber: Int = 1): String =
        "quran_ayahs/$suraNumber?aya=$ayaNumber"
}

@Composable
fun WearNavigation(
    startDestination: String = WearRoutes.HOME,
    initialSuraNumber: Int = 1,
    launchTrigger: Int = 0
) {
    val navController = rememberSwipeDismissableNavController()
    val initialGraphStartDestination = remember { startDestination }

    LaunchedEffect(launchTrigger) {
        if (launchTrigger > 0) {
            val route = when (startDestination) {
                WearRoutes.QURAN_AYAHS -> WearRoutes.quranAyahs(initialSuraNumber)
                else -> startDestination
            }
            navController.navigate(route) {
                if (startDestination == WearRoutes.HOME) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
                launchSingleTop = true
            }
        }
    }

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = initialGraphStartDestination
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
                onSurahClick = { suraNumber, ayaNumber ->
                    navController.navigate(WearRoutes.quranAyahs(suraNumber, ayaNumber))
                }
            )
        }
        composable(
            route = WearRoutes.QURAN_AYAHS,
            arguments = listOf(
                navArgument("suraNumber") { type = NavType.IntType; defaultValue = initialSuraNumber },
                navArgument("ayaNumber") { type = NavType.IntType; defaultValue = 1 }
            )
        ) { backStackEntry ->
            val suraNumber = backStackEntry.arguments?.getInt("suraNumber") ?: initialSuraNumber
            val ayaNumber = backStackEntry.arguments?.getInt("ayaNumber") ?: 1
            val surahListEntry = remember(backStackEntry) {
                runCatching { navController.getBackStackEntry(WearRoutes.QURAN_SURAHS) }.getOrNull()
            }
            val sharedQuranViewModel: WearQuranViewModel = if (surahListEntry != null) {
                hiltViewModel(surahListEntry)
            } else {
                hiltViewModel()
            }
            WearAyahReaderScreen(
                suraNumber = suraNumber,
                initialAyaNumber = ayaNumber,
                onNavigateSurah = { nextSura ->
                    navController.navigate(WearRoutes.quranAyahs(nextSura, 1)) {
                        popUpTo(WearRoutes.QURAN_AYAHS) { inclusive = true }
                    }
                },
                viewModel = sharedQuranViewModel
            )
        }
        composable(WearRoutes.QIBLA) {
            WearQiblaScreen()
        }
    }
}
