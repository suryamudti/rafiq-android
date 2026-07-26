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
    private var completionListener: (() -> Unit)? = null

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

    fun playAyah(url: String, onComplete: () -> Unit) {
        completionListener = onComplete
        if (player == null) {
            player = ExoPlayer.Builder(context).build()
        }
        player?.apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            play()
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        completionListener?.invoke()
                    }
                }
            })
        }
    }

    fun toggle() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun stop() {
        player?.stop()
        completionListener = null
    }

    fun release() {
        player?.release()
        player = null
        completionListener = null
    }
}
