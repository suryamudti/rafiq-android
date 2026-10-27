package com.smiledev.rafiq_quran.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.smiledev.rafiq_quran.ui.prophets.SharedProphetsScreen
import com.smiledev.rafiq_quran.ui.tasbih.SharedTasbihScreen
import com.smiledev.rafiq_quran.ui.theme.RafiqMultiplatformTheme

enum class RafiqAppTab(val title: String) {
    TASBIH("Digital Tasbih"),
    ASMAUL_HUSNA("Asmaul Husna"),
    PROPHETS("25 Prophets"),
    APP_INFO("App Info")
}

@Composable
fun RafiqAppRoot(
    initialTab: RafiqAppTab = RafiqAppTab.TASBIH,
    onNavigateBack: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    RafiqMultiplatformTheme {
        Column(modifier = Modifier.fillMaxSize()) {
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
                RafiqAppTab.APP_INFO -> {
                    AppInfoScreen(
                        onBackClick = onNavigateBack
                    )
                }
            }
        }
    }
}
