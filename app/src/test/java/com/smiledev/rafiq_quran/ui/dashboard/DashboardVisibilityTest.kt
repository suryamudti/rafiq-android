package com.smiledev.rafiq_quran.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardVisibilityTest {

    @Test
    fun `empty set stays empty`() {
        assertEquals(emptySet<String>(), sanitizeHiddenKeys(emptySet()))
    }

    @Test
    fun `known keys pass through`() {
        val input = setOf("quran", "zakat", "prayer_log")
        assertEquals(input, sanitizeHiddenKeys(input))
    }

    @Test
    fun `unknown keys are dropped`() {
        assertEquals(setOf("quran"), sanitizeHiddenKeys(setOf("quran", "bogus-key")))
    }

    @Test
    fun `known keys cover all 15 shortcuts including future hifz`() {
        assertEquals(15, KNOWN_DASHBOARD_KEYS.size)
        assertTrue("hifz" in KNOWN_DASHBOARD_KEYS)
        assertTrue("prayer_guidance" in KNOWN_DASHBOARD_KEYS)
        assertTrue("sunnah_guidance" in KNOWN_DASHBOARD_KEYS)
    }
}
