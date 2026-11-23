package com.smiledev.rafiq_quran.tv.service

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build

object TvAdhanAudioPlayer {

    @Volatile
    private var activeRingtone: Ringtone? = null

    fun play(context: Context) {
        stop()
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = false
            }
            activeRingtone = ringtone
            ringtone.play()
        }
    }

    fun stop() {
        val current = activeRingtone
        activeRingtone = null
        runCatching {
            if (current?.isPlaying == true) {
                current.stop()
            }
        }
    }
}
