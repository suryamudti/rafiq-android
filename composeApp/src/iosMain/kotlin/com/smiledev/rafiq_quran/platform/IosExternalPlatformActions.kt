package com.smiledev.rafiq_quran.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class IosExternalPlatformActions : ExternalPlatformActions {
    override fun openMapsDirections(lat: Double, lon: Double, label: String) {
        // iOS UIApplication.sharedApplication.openURL(NSURL.URLWithString("http://maps.apple.com/?daddr=$lat,$lon"))
    }

    override fun openUrl(url: String) {
        // iOS UIApplication.sharedApplication.openURL(NSURL.URLWithString(url))
    }

    override fun shareText(text: String) {
        // iOS UIActivityViewController
    }

    override fun applyAppLocale(langCode: String) {
        // iOS NSUserDefaults override
    }
}

@Composable
actual fun rememberExternalPlatformActions(): ExternalPlatformActions {
    return remember { IosExternalPlatformActions() }
}
