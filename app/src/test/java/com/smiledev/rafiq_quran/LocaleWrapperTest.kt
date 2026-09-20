package com.smiledev.rafiq_quran

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

class LocaleWrapperTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun resolveLocale_returnsIndonesianForId() {
        val locale = resolveLocale("id")
        assertEquals("id", locale.language)
    }

    @Test
    fun resolveLocale_returnsEnglishForEn() {
        val locale = resolveLocale("en")
        assertEquals("en", locale.language)
    }

    @Test
    fun resolveLocale_returnsValidLocaleForSystemAndBoth() {
        val systemLocale = resolveLocale("system")
        assertNotNull(systemLocale)
        val isIdOrEn = systemLocale.language == "id" || systemLocale.language == "in" || systemLocale.language == "en"
        assertEquals(true, isIdOrEn)

        val bothLocale = resolveLocale("both")
        assertNotNull(bothLocale)
        val isBothIdOrEn = bothLocale.language == "id" || bothLocale.language == "in" || bothLocale.language == "en"
        assertEquals(true, isBothIdOrEn)
    }

    @Test
    fun resolveLocale_fallsBackToIndonesianWhenSystemLocaleIsIndonesian() {
        Locale.setDefault(Locale.forLanguageTag("id-ID"))
        val resolved = resolveLocale("system")
        assertEquals("id", resolved.language)
    }

    @Test
    fun resolveLocale_fallsBackToEnglishWhenSystemLocaleIsOther() {
        Locale.setDefault(Locale.forLanguageTag("fr-FR"))
        val resolved = resolveLocale("system")
        assertEquals("en", resolved.language)
    }
}
