package com.smiledev.rafiq_quran.service

object MediaNotificationPolicy {
    fun shouldBeOngoing(isPlaying: Boolean): Boolean = isPlaying
}
