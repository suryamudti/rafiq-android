package com.smiledev.rafiq.core

import java.util.Locale

fun currentLocaleCode(): String {
    val lang = Locale.getDefault().language
    return if (lang == "id" || lang == "in") "id" else "en"
}
