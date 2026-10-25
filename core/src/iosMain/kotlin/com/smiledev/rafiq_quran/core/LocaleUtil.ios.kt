package com.smiledev.rafiq_quran.core

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

actual fun currentLocaleCode(): String {
    val languages = NSLocale.preferredLanguages
    val firstLang = languages.firstOrNull()?.toString()?.take(2)?.lowercase() ?: "en"
    return if (firstLang == "id" || firstLang == "in") "id" else "en"
}
