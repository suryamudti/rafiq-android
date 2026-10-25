package com.smiledev.rafiq_quran.ui

import androidx.compose.runtime.Composable
import com.smiledev.rafiq_quran.ui.appinfo.AppInfoScreen
import com.smiledev.rafiq_quran.ui.theme.RafiqMultiplatformTheme

@Composable
fun RafiqAppRoot(
    onNavigateBack: (() -> Unit)? = null
) {
    RafiqMultiplatformTheme {
        AppInfoScreen(
            onBackClick = onNavigateBack
        )
    }
}
