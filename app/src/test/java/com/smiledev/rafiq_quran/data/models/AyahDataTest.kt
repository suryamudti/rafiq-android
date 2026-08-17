package com.smiledev.rafiq.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AyahDataTest {

    @Test
    fun `default values are correct`() {
        val ayah = AyahData(sura = 1, aya = 1, text = "بسم الله الرحمن الرحيم", bismillah = null)
        assertEquals(1, ayah.sura)
        assertEquals(1, ayah.aya)
        assertNull(ayah.bismillah)
        assertNull(ayah.translation)
        assertEquals(0, ayah.page)
        assertEquals(0, ayah.juz)
        assertFalse(ayah.sajda)
        assertNull(ayah.sajdaType)
        assertFalse(ayah.isFirstAyaOfJuz)
        assertFalse(ayah.isFirstAyaOfPage)
    }

    @Test
    fun `bismillah can be non-null`() {
        val ayah = AyahData(sura = 1, aya = 1, text = "...", bismillah = "بسم الله الرحمن الرحيم")
        assertEquals("بسم الله الرحمن الرحيم", ayah.bismillah)
    }

    @Test
    fun `sajda flag and type`() {
        val ayah = AyahData(sura = 32, aya = 15, text = "...", bismillah = null, sajda = true, sajdaType = "obligatory")
        assertTrue(ayah.sajda)
        assertEquals("obligatory", ayah.sajdaType)
    }

    @Test
    fun `first aya of juz and page markers`() {
        val ayah = AyahData(sura = 1, aya = 1, text = "...", bismillah = null, isFirstAyaOfJuz = true, isFirstAyaOfPage = true)
        assertTrue(ayah.isFirstAyaOfJuz)
        assertTrue(ayah.isFirstAyaOfPage)
    }
}
