package com.smiledev.rafiq_quran.platform

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

@SuppressLint("MissingPermission")
class AndroidTasbihFeedbackController(
    private val context: Context
) : TasbihFeedbackController {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val toneGenerator: ToneGenerator? by lazy {
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    override fun vibrateTick() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(30)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun vibrateTargetReached() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 150), -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 100, 100, 150), -1)
            }
        }
    }

    override fun playClickSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
    }
}
