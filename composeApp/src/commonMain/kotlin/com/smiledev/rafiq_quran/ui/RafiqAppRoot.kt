package com.smiledev.rafiq_quran.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.smiledev.rafiq_quran.ui.appinfo.AppInfoScreen
import com.smiledev.rafiq_quran.ui.tasbih.SharedTasbihScreen
import com.smiledev.rafiq_quran.ui.theme.RafiqMultiplatformTheme

enum class RafiqAppTab(val title: String) {
    TASBIH("Digital Tasbih"),
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
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
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
                RafiqAppTab.APP_INFO -> {
                    AppInfoScreen(
                        onBackClick = onNavigateBack
                    )
                }
            }
        }
    }
}
