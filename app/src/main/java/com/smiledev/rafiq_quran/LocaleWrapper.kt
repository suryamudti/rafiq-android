package com.smiledev.rafiq_quran

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import java.util.Locale

fun resolveLocale(lang: String): Locale {
    return when (lang) {
        "id" -> Locale.forLanguageTag("id")
        "en" -> Locale.forLanguageTag("en")
        else -> {
            val systemLocale = runCatching {
                val config = Resources.getSystem().configuration
                val locales = ConfigurationCompat.getLocales(config)
                if (!locales.isEmpty) locales.get(0) else null
            }.getOrNull() ?: Locale.getDefault()
            val code = systemLocale.language
            if (code == "id" || code == "in") Locale.forLanguageTag("id") else Locale.forLanguageTag("en")
        }
    }
}

class LocalizedContext(
    base: Context,
    private val configContext: Context
) : ContextWrapper(base) {
    override fun getResources(): Resources = configContext.resources
    override fun getAssets(): AssetManager = configContext.assets
}

fun Context.wrapLocale(lang: String): Context {
    val locale = resolveLocale(lang)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    val configContext = createConfigurationContext(config)
    return LocalizedContext(this, configContext)
}
