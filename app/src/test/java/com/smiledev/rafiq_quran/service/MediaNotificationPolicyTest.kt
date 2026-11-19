package com.smiledev.rafiq_quran.service

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaNotificationPolicyTest {
    @Test
    fun ongoingOnlyWhenPlaying() {
        assertTrue(MediaNotificationPolicy.shouldBeOngoing(true))
        assertFalse(MediaNotificationPolicy.shouldBeOngoing(false))
    }

    @Test
    fun pausedNotificationClearsOngoingAndNoClearFlags() {
        val initialFlags = Notification.FLAG_ONGOING_EVENT or
            Notification.FLAG_NO_CLEAR or
            Notification.FLAG_ONLY_ALERT_ONCE

        val updated = MediaNotificationPolicy.applyDismissibleFlags(initialFlags, isPlaying = false)

        assertEquals(0, updated and Notification.FLAG_ONGOING_EVENT)
        assertEquals(0, updated and Notification.FLAG_NO_CLEAR)
        assertEquals(Notification.FLAG_ONLY_ALERT_ONCE, updated and Notification.FLAG_ONLY_ALERT_ONCE)
    }

    @Test
    fun playingNotificationSetsOngoingFlag() {
        val updated = MediaNotificationPolicy.applyDismissibleFlags(
            Notification.FLAG_ONLY_ALERT_ONCE,
            isPlaying = true
        )

        assertTrue((updated and Notification.FLAG_ONGOING_EVENT) != 0)
        assertTrue((updated and Notification.FLAG_ONLY_ALERT_ONCE) != 0)
    }
}
