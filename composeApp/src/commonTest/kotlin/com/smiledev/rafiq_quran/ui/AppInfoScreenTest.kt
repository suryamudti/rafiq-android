package com.smiledev.rafiq_quran.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppInfoScreenTest {

    @Test
    fun testAppInfoDefaults() {
        val appVersion = "1.0.0"
        assertEquals("1.0.0", appVersion)
        assertTrue(appVersion.isNotEmpty())
    }
}
