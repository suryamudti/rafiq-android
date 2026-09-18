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
class TasbihRepositoryImplTest {

    private val assetManager: AssetManager = mockk()
    private val context: Context = mockk(relaxed = true)

    @Test
    fun `getTasbihItems parses json correctly and caches result`() {
        every { context.assets } returns assetManager

        val json = """
            {
              "data": [
                {
                  "id": 1,
                  "arabic": "سُبْحَانَ اللّهُ",
                  "transliteration": "Subhanallah",
                  "meaning_en": "Glory be to Allah",
                  "meaning_id": "Maha Suci Allah",
                  "default_count": 33
                },
                {
                  "id": 2,
                  "arabic": "ٱلْحَمْدُ لِلَّهِ",
                  "transliteration": "Alhamdulillah",
                  "meaning_en": "All praise is due to Allah",
                  "meaning_id": "Segala puji bagi Allah",
                  "default_count": 33
                }
              ]
            }
        """.trimIndent()

        every { assetManager.open("quran-data/dzikir/tasbih.json") } returns ByteArrayInputStream(json.toByteArray())

        val repo = TasbihRepositoryImpl(context)
        val result1 = repo.getTasbihItems()

        assertTrue("Expected Success but got $result1", result1 is Result.Success)
        val items = (result1 as Result.Success).data
        assertEquals(2, items.size)
        assertEquals("Subhanallah", items[0].transliteration)
        assertEquals("سُبْحَانَ اللّهُ", items[0].arabic)
        assertEquals("Glory be to Allah", items[0].meaningEn)
        assertEquals("Maha Suci Allah", items[0].meaningId)
        assertEquals(33, items[0].defaultCount)
        assertEquals("Alhamdulillah", items[1].transliteration)

        // Second call should return cached data without reopening asset
        val result2 = repo.getTasbihItems()
        assertTrue(result2 is Result.Success)
        assertEquals(2, (result2 as Result.Success).data.size)
    }

    @Test
    fun `getTasbihItems returns Database error when asset cannot be opened`() {
        every { context.assets } returns assetManager
        every { assetManager.open("quran-data/dzikir/tasbih.json") } throws RuntimeException("File not found")

        val repo = TasbihRepositoryImpl(context)
        val result = repo.getTasbihItems()

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Database)
    }
}
