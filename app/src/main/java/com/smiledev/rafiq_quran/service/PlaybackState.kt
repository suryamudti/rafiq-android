package com.smiledev.rafiq_quran.service

data class PlaybackState(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false
)