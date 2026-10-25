package com.smiledev.rafiq_quran.platform

import kotlinx.coroutines.flow.StateFlow

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val currentAyah: Int = 0,
    val durationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val isBuffering: Boolean = false
)

interface AudioPlayerController {
    val playbackState: StateFlow<AudioPlaybackState>
    fun play(url: String, ayahNumber: Int = 0)
    fun playAyah(url: String, ayahNumber: Int, onComplete: () -> Unit = {})
    fun toggle()
    fun stop()
    fun seekTo(positionMs: Long)
    fun release()
}
