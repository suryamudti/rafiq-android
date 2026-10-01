package com.smiledev.rafiq_quran.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaNotificationPolicyTest {
    @Test
    fun ongoingOnlyWhenPlaying() {
        assertTrue(MediaNotificationPolicy.shouldBeOngoing(true))
        assertFalse(MediaNotificationPolicy.shouldBeOngoing(false))
    }
}
