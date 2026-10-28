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
import com.smiledev.rafiq_quran.ui.appinfo.AppInfoScreen
import com.smiledev.rafiq_quran.ui.asmaulhusna.SharedAsmaulHusnaScreen
import com.smiledev.rafiq_quran.ui.calendar.SharedCalendarScreen
import com.smiledev.rafiq_quran.ui.guidance.SharedGuidanceScreen
import com.smiledev.rafiq_quran.ui.hadith.SharedHadithScreen
import com.smiledev.rafiq_quran.ui.prophets.SharedProphetsScreen
import com.smiledev.rafiq_quran.ui.quran.SharedQuranScreen
import com.smiledev.rafiq_quran.ui.tasbih.SharedTasbihScreen
import com.smiledev.rafiq_quran.ui.theme.RafiqMultiplatformTheme
import com.smiledev.rafiq_quran.ui.zakat.SharedZakatScreen

enum class RafiqAppTab(val title: String) {
    QURAN("Al-Qur'an"),
    HADITH("Hadith"),
    CALENDAR("Calendar"),
    TASBIH("Digital Tasbih"),
    ASMAUL_HUSNA("Asmaul Husna"),
    PROPHETS("25 Prophets"),
    ZAKAT("Zakat Calculator"),
    GUIDANCE("Prayer & Sunnah"),
    APP_INFO("App Info")
}

@Composable
fun RafiqAppRoot(
    initialTab: RafiqAppTab = RafiqAppTab.QURAN,
    onNavigateBack: (() -> Unit)? = null
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }

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
                RafiqAppTab.QURAN -> {
                    SharedQuranScreen(
                        onBackClick = onNavigateBack
                    )
                }
                RafiqAppTab.HADITH -> {
                    SharedHadithScreen(
                        onBackClick = onNavigateBack
                    )
                }
                RafiqAppTab.CALENDAR -> {
                    SharedCalendarScreen(
                        onBackClick = onNavigateBack
                    )
                }
                RafiqAppTab.TASBIH -> {
                    SharedTasbihScreen(
                        onBackClick = onNavigateBack
                    )
                }
                RafiqAppTab.ASMAUL_HUSNA -> {
                    SharedAsmaulHusnaScreen(
                        onBackClick = onNavigateBack
                    )
                }
                RafiqAppTab.PROPHETS -> {
                    SharedProphetsScreen(
                        onBackClick = onNavigateBack
                    )
                }
                RafiqAppTab.ZAKAT -> {
                    SharedZakatScreen(
                        onBackClick = onNavigateBack
                    )
                }
                RafiqAppTab.GUIDANCE -> {
                    SharedGuidanceScreen(
                        onBackClick = onNavigateBack
                    )
                }
                RafiqAppTab.APP_INFO -> {
                    AppInfoScreen(
                        onBackClick = onNavigateBack
                    )
                }
            }
        }
    }
}
