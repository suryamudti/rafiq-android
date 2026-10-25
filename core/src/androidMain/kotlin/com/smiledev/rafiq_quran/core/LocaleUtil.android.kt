package com.smiledev.rafiq_quran.core

import java.util.Locale

actual fun currentLocaleCode(): String {
    val lang = Locale.getDefault().language
    return if (lang == "id" || lang == "in") "id" else "en"
}
