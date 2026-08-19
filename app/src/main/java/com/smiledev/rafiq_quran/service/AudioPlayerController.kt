package com.smiledev.rafiq_quran.service

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.main)
    private var controller: MediaController? = null
    private var connecting = false
    private var connectGeneration = 0
    private var connectAttempts = 0
    private val pendingCommands = mutableListOf<(MediaController) -> Unit>()
    private var completionListener: (() -> Unit)? = null
    private var pollJob: Job? = null

    private companion object {
        const val MAX_CONNECT_ATTEMPTS = 3
        const val CONNECT_RETRY_DELAY_MS = 1500L
    }

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState

    init {
        connect()
    }

    fun play(url: String, title: String, artist: String) {
        completionListener = null
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).build())
            .build()
        runOrQueue { c ->
            c.stop()
            c.clearMediaItems()
            c.setMediaItem(mediaItem)
            c.prepare()
            c.play()
        }
    }

    fun playAyah(url: String, title: String, artist: String, onComplete: () -> Unit) {
        completionListener = onComplete
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).build())
            .build()
        runOrQueue { c ->
            c.stop()
            c.clearMediaItems()
            c.setMediaItem(mediaItem)
            c.prepare()
            c.play()
        }
    }

    fun toggle() {
        runOrQueue { c -> if (c.isPlaying) c.pause() else c.play() }
    }

    fun stop() {
        completionListener = null
        runOrQueue { c -> c.stop() }
    }

    fun seekTo(positionMs: Long) {
        runOrQueue { c -> c.seekTo(positionMs) }
    }

    fun release() {
        connectGeneration++
        controller?.release()
        controller = null
        connecting = false
        connectAttempts = 0
        pendingCommands.clear()
        pollJob?.cancel()
        completionListener = null
    }

    private fun connect() {
        if (controller != null || connecting) return
        connecting = true
        val generation = connectGeneration
        val sessionToken = SessionToken(context, ComponentName(context, AudioRecitationService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            if (generation != connectGeneration) {
                try {
                    future.get().release()
                } catch (_: Exception) {
                    // controller future was already released or failed; nothing to clean up
                }
                return@addListener
            }
            connecting = false
            try {
                onControllerConnected(future.get())
            } catch (e: Exception) {
                controller = null
                if (pendingCommands.isNotEmpty() && connectAttempts < MAX_CONNECT_ATTEMPTS) {
                    connectAttempts++
                    scope.launch {
                        delay(CONNECT_RETRY_DELAY_MS)
                        if (controller == null) connect()
                    }
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun onControllerConnected(c: MediaController) {
        connectAttempts = 0
        controller = c
        c.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    val callback = completionListener
                    completionListener = null
                    callback?.invoke()
                }
                updatePlaybackState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
            }
        })
        val commands = pendingCommands.toList()
        pendingCommands.clear()
        commands.forEach { it(c) }
        startPoller()
    }

    private fun runOrQueue(command: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            command(c)
        } else {
            pendingCommands.add(command)
            connect()
        }
    }

    private fun startPoller() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                updatePlaybackState()
                delay(500)
            }
        }
    }

    private fun updatePlaybackState() {
        val c = controller ?: return
        val duration = if (c.duration > 0) c.duration else 0L
        _playbackState.value = PlaybackState(
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            isPlaying = c.isPlaying
        )
    }
}
