package com.smiledev.rafiq_quran.service

import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.smiledev.rafiq_quran.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class AudioRecitationService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val handler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stopSelf() }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> handler.postDelayed(stopRunnable, STOP_DELAY_MS)
                    Player.STATE_BUFFERING, Player.STATE_READY -> handler.removeCallbacks(stopRunnable)
                    else -> Unit
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) handler.removeCallbacks(stopRunnable)
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                handler.removeCallbacks(stopRunnable)
            }
        })
        mediaSession = MediaSession.Builder(this, player).build()
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.notification_channel_name)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_play)
        val baseProvider = notificationProvider
        setMediaNotificationProvider(
            object : androidx.media3.session.MediaNotification.Provider {
                override fun createNotification(
                    mediaSession: MediaSession,
                    customLayout: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
                    actionFactory: androidx.media3.session.MediaNotification.ActionFactory,
                    onNotificationChangedCallback: androidx.media3.session.MediaNotification.Provider.Callback
                ): androidx.media3.session.MediaNotification {
                    val base = baseProvider.createNotification(mediaSession, customLayout, actionFactory, onNotificationChangedCallback)
                    val notif = base.notification
                    val ongoing = MediaNotificationPolicy.shouldBeOngoing(player.isPlaying)
                    if (!ongoing) {
                        notif.flags = notif.flags and android.app.Notification.FLAG_ONGOING_EVENT.inv()
                    }
                    val deleteIntent = android.app.PendingIntent.getService(
                        this@AudioRecitationService,
                        0,
                        android.content.Intent(this@AudioRecitationService, AudioRecitationService::class.java).setAction(ACTION_STOP_FROM_DISMISS),
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    notif.deleteIntent = deleteIntent
                    return base
                }
                override fun handleCustomCommand(
                    session: MediaSession,
                    action: String,
                    extras: android.os.Bundle
                ): Boolean = baseProvider.handleCustomCommand(session, action, extras)
            }
        )
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_FROM_DISMISS) {
            player.stop()
            player.clearMediaItems()
            stopSelf()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        handler.removeCallbacks(stopRunnable)
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "media_playback"
        const val STOP_DELAY_MS = 3000L
        const val ACTION_STOP_FROM_DISMISS = "com.smiledev.rafiq_quran.action.STOP_FROM_DISMISS"
    }
}
