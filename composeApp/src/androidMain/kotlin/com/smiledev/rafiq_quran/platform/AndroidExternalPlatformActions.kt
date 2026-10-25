package com.smiledev.rafiq_quran.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

class AndroidExternalPlatformActions(
    private val context: Context
) : ExternalPlatformActions {

    override fun openMapsDirections(lat: Double, lon: Double, label: String) {
        val uri = "geo:$lat,$lon?q=$lat,$lon(${Uri.encode(label)})".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val browserUri = "https://www.google.com/maps/dir/?api=1&destination=$lat,$lon".toUri()
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        }
    }

    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignored if no browser installed
        }
    }

    override fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(intent, null).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }

    override fun applyAppLocale(langCode: String) {
        // Handled via LocaleWrapper on Android
    }
}
