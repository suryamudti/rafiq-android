package com.smiledev.rafiq_quran.platform

import androidx.compose.runtime.Composable

interface ExternalPlatformActions {
    fun openMapsDirections(lat: Double, lon: Double, label: String = "")
    fun openUrl(url: String)
    fun shareText(text: String)
    fun applyAppLocale(langCode: String)
}

@Composable
expect fun rememberExternalPlatformActions(): ExternalPlatformActions
