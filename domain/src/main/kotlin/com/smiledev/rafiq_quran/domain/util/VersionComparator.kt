package com.smiledev.rafiq_quran.domain.util

object VersionComparator {
    /**
     * Compares two version strings (e.g. "1.0.73" vs "1.0.74", "v1.0" vs "v1.0.74").
     * Returns true if [latestVersion] is strictly newer than [currentVersion].
     */
    fun isNewer(currentVersion: String, latestVersion: String): Boolean {
        val currentParts = parseVersion(currentVersion)
        val latestParts = parseVersion(latestVersion)

        if (latestParts.isEmpty()) return false
        if (currentParts.isEmpty()) return true

        val maxLen = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLen) {
            val curr = currentParts.getOrElse(i) { 0 }
            val lat = latestParts.getOrElse(i) { 0 }
            if (lat > curr) return true
            if (lat < curr) return false
        }
        return false
    }

    private fun parseVersion(version: String): List<Int> {
        val cleaned = version.trim().removePrefix("v").removePrefix("V")
        val mainPart = cleaned.split("-", "+").firstOrNull().orEmpty()
        return mainPart.split(".")
            .mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
    }
}
