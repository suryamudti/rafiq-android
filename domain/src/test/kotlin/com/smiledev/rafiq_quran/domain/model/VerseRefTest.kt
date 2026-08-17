package com.smiledev.rafiq_quran.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class VerseRefTest {

    @Test
    fun `verse ref holds surah and ayah range`() {
        val ref = VerseRef(
            surah = 2,
            surahNameEn = "Al-Baqarah",
            surahNameId = "Al-Baqarah",
            ayahStart = 30,
            ayahEnd = 39
        )
        assertEquals(2, ref.surah)
        assertEquals("Al-Baqarah", ref.surahNameEn)
        assertEquals("Al-Baqarah", ref.surahNameId)
        assertEquals(30, ref.ayahStart)
        assertEquals(39, ref.ayahEnd)
    }

    @Test
    fun `verse ref supports single ayah when end equals start`() {
        val ref = VerseRef(11, "Hud", "Hud", 25, 25)
        assertEquals(25, ref.ayahEnd)
        assertEquals(25, ref.ayahStart)
    }
}
