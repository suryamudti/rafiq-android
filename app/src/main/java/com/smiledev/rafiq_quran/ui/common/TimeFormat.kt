package com.smiledev.rafiq_quran.ui.common

fun formatDuration(ms: Long): String {
    val totalSeconds = if (ms > 0) ms / 1000 else 0
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
