package com.smiledev.rafiq_quran.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smiledev.rafiq_quran.platform.rememberExternalPlatformActions
import com.smiledev.rafiq_quran.ui.appinfo.AppInfoScreen
import com.smiledev.rafiq_quran.ui.asmaulhusna.SharedAsmaulHusnaScreen
import com.smiledev.rafiq_quran.ui.bookmarks.SharedBookmarksScreen
import com.smiledev.rafiq_quran.ui.calendar.SharedCalendarScreen
import com.smiledev.rafiq_quran.ui.dashboard.SharedDashboardScreen
import com.smiledev.rafiq_quran.ui.guidance.SharedGuidanceScreen
import com.smiledev.rafiq_quran.ui.hadith.SharedHadithScreen
import com.smiledev.rafiq_quran.ui.hifz.SharedHifzScreen
import com.smiledev.rafiq_quran.ui.mosques.SharedMosquesScreen
import com.smiledev.rafiq_quran.ui.prayerlog.SharedPrayerLogScreen
import com.smiledev.rafiq_quran.ui.prayertimes.SharedPrayerTimesScreen
import com.smiledev.rafiq_quran.ui.prophets.SharedProphetsScreen
import com.smiledev.rafiq_quran.ui.qibla.SharedQiblaScreen
import com.smiledev.rafiq_quran.ui.quran.SharedQuranScreen
import com.smiledev.rafiq_quran.ui.recitation.SharedRecitationScreen
import com.smiledev.rafiq_quran.ui.settings.SharedSettingsScreen
import com.smiledev.rafiq_quran.ui.sources.SharedSourcesScreen
import com.smiledev.rafiq_quran.ui.tasbih.SharedTasbihScreen
import com.smiledev.rafiq_quran.ui.tasbih.history.SharedTasbihHistoryScreen
import com.smiledev.rafiq_quran.ui.theme.RafiqMultiplatformTheme
import com.smiledev.rafiq_quran.ui.zakat.SharedZakatScreen

enum class RafiqAppTab(val title: String) {
    DASHBOARD("Home"),
    PRAYER_TIMES("Prayer Times"),
    QIBLA("Qibla"),
    QURAN("Al-Qur'an"),
    RECITATION("Audio"),
    BOOKMARKS("Bookmarks"),
    MOSQUES("Mosques"),
    HADITH("Hadith"),
    CALENDAR("Calendar"),
    PRAYER_LOG("Prayer Tracker"),
    HIFZ("Hifz Tracker"),
    TASBIH("Digital Tasbih"),
    TASBIH_HISTORY("Tasbih History"),
    ASMAUL_HUSNA("Asmaul Husna"),
    PROPHETS("25 Prophets"),
    ZAKAT("Zakat"),
    GUIDANCE("Guidance"),
    SETTINGS("Settings"),
    SOURCES("Sources"),
    APP_INFO("App Info")
}

@Composable
fun RafiqAppRoot(
    initialTab: RafiqAppTab = RafiqAppTab.DASHBOARD,
    onNavigateBack: (() -> Unit)? = null
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val platformActions = rememberExternalPlatformActions()

    RafiqMultiplatformTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                RafiqAppTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title) }
                    )
                }
            }

            when (selectedTab) {
                RafiqAppTab.DASHBOARD -> {
                    SharedDashboardScreen(
                        onNavigateTab = { target -> selectedTab = target },
                        onNavigateToAyah = { _, _ -> selectedTab = RafiqAppTab.QURAN }
                    )
                }
                RafiqAppTab.PRAYER_TIMES -> {
                    SharedPrayerTimesScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD },
                        onNavigateToQibla = { selectedTab = RafiqAppTab.QIBLA },
                        onNavigateToPrayerLog = { selectedTab = RafiqAppTab.PRAYER_LOG },
                        onNavigateToGuidance = { selectedTab = RafiqAppTab.GUIDANCE }
                    )
                }
                RafiqAppTab.QIBLA -> {
                    SharedQiblaScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.PRAYER_TIMES }
                    )
                }
                RafiqAppTab.QURAN -> {
                    SharedQuranScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD }
                    )
                }
                RafiqAppTab.RECITATION -> {
                    SharedRecitationScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD }
                    )
                }
                RafiqAppTab.BOOKMARKS -> {
                    SharedBookmarksScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD },
                        onNavigateToAyah = { _, _ -> selectedTab = RafiqAppTab.QURAN }
                    )
                }
                RafiqAppTab.MOSQUES -> {
                    SharedMosquesScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD },
                        onGetDirections = { mosque ->
                            platformActions.openMapsDirections(
                                lat = mosque.latitude,
                                lon = mosque.longitude,
                                label = mosque.name
                            )
                        }
                    )
                }
                RafiqAppTab.HADITH -> {
                    SharedHadithScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD }
                    )
                }
                RafiqAppTab.CALENDAR -> {
                    SharedCalendarScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD }
                    )
                }
                RafiqAppTab.PRAYER_LOG -> {
                    SharedPrayerLogScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD }
                    )
                }
                RafiqAppTab.HIFZ -> {
                    SharedHifzScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD }
                    )
                }
                RafiqAppTab.TASBIH -> {
                    SharedTasbihScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD }
                    )
                }
                RafiqAppTab.TASBIH_HISTORY -> {
                    SharedTasbihHistoryScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.TASBIH }
                    )
                }
                RafiqAppTab.ASMAUL_HUSNA -> {
                    SharedAsmaulHusnaScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD }
                    )
                }
                RafiqAppTab.PROPHETS -> {
                    SharedProphetsScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD }
                    )
                }
                RafiqAppTab.ZAKAT -> {
                    SharedZakatScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD }
                    )
                }
                RafiqAppTab.GUIDANCE -> {
                    SharedGuidanceScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD }
                    )
                }
                RafiqAppTab.SETTINGS -> {
                    SharedSettingsScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD },
                        onNavigateToSources = { selectedTab = RafiqAppTab.SOURCES }
                    )
                }
                RafiqAppTab.SOURCES -> {
                    SharedSourcesScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.SETTINGS },
                        onOpenUrl = { url -> platformActions.openUrl(url) }
                    )
                }
                RafiqAppTab.APP_INFO -> {
                    AppInfoScreen(
                        onBackClick = onNavigateBack ?: { selectedTab = RafiqAppTab.DASHBOARD }
                    )
                }
            }
        }
    }
}
