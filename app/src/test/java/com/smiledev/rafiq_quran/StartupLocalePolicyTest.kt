package com.smiledev.rafiq_quran

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupLocalePolicyTest {
    @Test
    fun recreatesOnlyOnMismatch() {
        assertTrue(StartupLocalePolicy.shouldRecreate("system", "id"))
        assertTrue(StartupLocalePolicy.shouldRecreate("en", "id"))
        assertFalse(StartupLocalePolicy.shouldRecreate("id", "id"))
        assertFalse(StartupLocalePolicy.shouldRecreate("en", "system"))
    }
}
