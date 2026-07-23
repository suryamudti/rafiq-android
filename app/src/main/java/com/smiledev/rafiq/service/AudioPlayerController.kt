package com.smiledev.rafiq.service

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var player: ExoPlayer? = null

    val isPlaying: Boolean get() = player?.isPlaying ?: false
    val currentPosition: Long get() = player?.currentPosition ?: 0L
    val duration: Long get() = player?.duration ?: 0L

    fun play(url: String) {
        if (player == null) {
            player = ExoPlayer.Builder(context).build()
        }
        player?.apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            play()
        }
    }

    fun toggle() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun stop() {
        player?.stop()
    }

    fun release() {
        player?.release()
        player = null
    }
}
