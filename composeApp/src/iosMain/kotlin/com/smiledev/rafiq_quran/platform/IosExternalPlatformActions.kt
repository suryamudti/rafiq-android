package com.smiledev.rafiq_quran.platform

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
