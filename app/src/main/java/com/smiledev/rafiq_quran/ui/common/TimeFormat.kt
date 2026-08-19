package com.smiledev.rafiq_quran.ui.common

import java.util.Locale

fun formatDuration(ms: Long): String {
    val totalSeconds = if (ms > 0) ms / 1000 else 0
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
