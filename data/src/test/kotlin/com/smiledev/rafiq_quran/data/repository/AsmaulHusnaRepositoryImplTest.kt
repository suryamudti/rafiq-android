package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import android.content.res.AssetManager
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class AsmaulHusnaRepositoryImplTest {

    private val assetManager: AssetManager = mockk()
    private val context: Context = mockk(relaxed = true)

    @Test
    fun `getNames parses json correctly and caches result`() {
        every { context.assets } returns assetManager

        val json = """
            [
              {"id":1,"arabic":"الرحمن","transliteration":"Ar-Rahman","meaning_en":"The Most Gracious","meaning_id":"Maha Pengasih","benefit_en":"Recite to increase compassion","benefit_id":"Bacalah untuk meningkatkan rasa kasih sayang"},
              {"id":2,"arabic":"الرحيم","transliteration":"Ar-Rahim","meaning_en":"The Most Merciful","meaning_id":"Maha Penyayang","benefit_en":"Recite for divine mercy","benefit_id":"Bacalah untuk memperoleh rahmat"}
            ]
        """.trimIndent()

        every { assetManager.open("quran-data/asmaul_husna.json") } returns ByteArrayInputStream(json.toByteArray())

        val repo = AsmaulHusnaRepositoryImpl(context)
        val result1 = repo.getNames()

        assertTrue("Expected Success but got $result1", result1 is Result.Success)
        val names = (result1 as Result.Success).data
        assertEquals(2, names.size)
        assertEquals("Ar-Rahman", names[0].transliteration)
        assertEquals("الرحمن", names[0].arabic)
        assertEquals("The Most Gracious", names[0].meaningEn)
        assertEquals("Maha Pengasih", names[0].meaningId)
        assertEquals("Ar-Rahim", names[1].transliteration)

        // Second call should return cached data without re-opening assets
        val result2 = repo.getNames()
        assertTrue(result2 is Result.Success)
        assertEquals(2, (result2 as Result.Success).data.size)
    }

    @Test
    fun `getNames returns Database error when asset cannot be opened`() {
        every { context.assets } returns assetManager
        every { assetManager.open("quran-data/asmaul_husna.json") } throws RuntimeException("File not found")

        val repo = AsmaulHusnaRepositoryImpl(context)
        val result = repo.getNames()

        assertTrue("Expected Error but got $result", result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Database)
    }
}
