package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import android.content.res.AssetManager
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class ReciterRepositoryImplTest {

    private lateinit var assetManager: AssetManager
    private lateinit var context: Context

    @Before
    fun setUp() {
        assetManager = mockk()
        context = mockk(relaxed = true)
        every { context.assets } returns assetManager
    }

    @Test
    fun `getReciters parses json asset into domain Reciter list`() {
        val json = """
            [
              {
                "id": 1,
                "name_en": "Mishary Rashid Alafasy",
                "name_ar": "مشاري راشد العفاسي",
                "style": "Murattal",
                "country": "Kuwait",
                "audio_base": "https://cdn.example.com/alafasy/"
              },
              {
                "id": 2,
                "name_en": "Abdul Basit Abdus Samad",
                "name_ar": "عبد الباسط عبد الصمد",
                "style": "Mujawwad",
                "country": "Egypt",
                "audio_base": "https://cdn.example.com/abdulbasit/"
              }
            ]
        """.trimIndent()
        every { assetManager.open("quran-data/reciters.json") } returns ByteArrayInputStream(json.toByteArray())

        val repo = ReciterRepositoryImpl(context)
        val result = repo.getReciters()

        assertTrue(result is Result.Success)
        val reciters = (result as Result.Success).data
        assertEquals(2, reciters.size)
        assertEquals(1, reciters[0].id)
        assertEquals("Mishary Rashid Alafasy", reciters[0].nameEn)
        assertEquals("مشاري راشد العفاسي", reciters[0].nameAr)
        assertEquals("Murattal", reciters[0].style)
        assertEquals("Kuwait", reciters[0].country)
        assertEquals("https://cdn.example.com/alafasy/", reciters[0].audioBase)
        assertEquals("Abdul Basit Abdus Samad", reciters[1].nameEn)
    }

    @Test
    fun `getReciters handles empty json array`() {
        every { assetManager.open("quran-data/reciters.json") } returns ByteArrayInputStream("[]".toByteArray())

        val repo = ReciterRepositoryImpl(context)
        val result = repo.getReciters()

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
    }

    @Test
    fun `getReciters returns Database error when asset open fails`() {
        every { assetManager.open("quran-data/reciters.json") } throws RuntimeException("File not found")

        val repo = ReciterRepositoryImpl(context)
        val result = repo.getReciters()

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Database)
    }

    @Test
    fun `getReciters returns Database error on malformed json`() {
        every { assetManager.open("quran-data/reciters.json") } returns ByteArrayInputStream("{invalid".toByteArray())

        val repo = ReciterRepositoryImpl(context)
        val result = repo.getReciters()

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Database)
    }
}
