package com.smiledev.rafiq_quran

object StartupLocalePolicy {
    fun shouldRecreate(currentLang: String, storedLang: String): Boolean {
        if (storedLang == "system") return false
        return currentLang != storedLang
    }
}
