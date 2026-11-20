package com.smiledev.rafiq_quran.service

import android.app.Notification

object MediaNotificationPolicy {
    fun shouldBeOngoing(isPlaying: Boolean): Boolean = isPlaying

    fun applyDismissibleFlags(flags: Int, isPlaying: Boolean): Int {
        return if (shouldBeOngoing(isPlaying)) {
            flags or Notification.FLAG_ONGOING_EVENT
        } else {
            flags and Notification.FLAG_ONGOING_EVENT.inv() and Notification.FLAG_NO_CLEAR.inv()
        }
    }
}
