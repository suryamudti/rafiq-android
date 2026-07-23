package com.smiledev.rafiq.data.repository

import android.content.Context
import android.content.res.AssetManager
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DatabaseCopier
import com.smiledev.rafiq.core.Result
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class QuranRepositoryImplTest {

    private val assetManager: AssetManager = mockk()
    private val databaseCopier: DatabaseCopier = mockk(relaxed = true)
    private lateinit var repo: QuranRepositoryImpl

    @Before
    fun setUp() {
        val context: Context = mockk(relaxed = true)
        every { context.assets } returns assetManager
        repo = QuranRepositoryImpl(context, databaseCopier)
    }

    @Test
    fun `getChapters parses valid JSON`() {
        val json = """
            {
                "chapters": [
                    {
                        "id": 1, "chapter_number": 1, "name_arabic": "الفاتحة",
                        "name_simple": "Al-Fatiha", "translated_name": {"name": "The Opening"},
                        "verses_count": 7, "revelation_place": "makkah"
                    }
                ]
            }
        """.trimIndent()
        every { assetManager.open("quran-data/chapters/chapters.en.json") } returns ByteArrayInputStream(json.toByteArray())

        val result = repo.getChapters("en")

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val chapters = (result as Result.Success).data
        assertEquals(1, chapters.size)
        assertEquals("Al-Fatiha", chapters[0].nameSimple)
        assertEquals("The Opening", chapters[0].translatedName)
    }

    @Test
    fun `getChapters handles missing asset`() {
        every { assetManager.open("quran-data/chapters/chapters.en.json") } throws RuntimeException("File not found")

        val result = repo.getChapters("en")

        assertTrue("Expected Error but got ${result}", result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Database)
    }
}
