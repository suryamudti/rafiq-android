package com.smiledev.rafiq_quran

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

fun Context.wrapLocale(lang: String): Context {
    if (lang == "system" || lang == "both" || lang.isBlank()) return this
    val locale = Locale(lang)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}
