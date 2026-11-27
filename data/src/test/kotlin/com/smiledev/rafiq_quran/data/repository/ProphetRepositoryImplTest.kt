package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import android.content.res.AssetManager
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class ProphetRepositoryImplTest {

    private lateinit var assetManager: AssetManager
    private lateinit var context: Context

    private val sampleJson = """
        [
          {
            "id": 1,
            "name_arabic": "آدم",
            "name_en": "Adam",
            "name_id": "Adam",
            "summary_en": "First prophet",
            "summary_id": "Nabi pertama",
            "story_en": "Story of Adam",
            "story_id": "Kisah Nabi Adam",
            "miracles_en": "Taught all names",
            "miracles_id": "Diajarkan semua nama",
            "era_en": "Primordial",
            "era_id": "Zaman purba",
            "verses": [
              {"surah": 2, "surah_name_en": "Al-Baqarah", "surah_name_id": "Al-Baqarah", "ayah_start": 30, "ayah_end": 39}
            ]
          },
          {
            "id": 2,
            "name_arabic": "نوح",
            "name_en": "Nuh",
            "name_id": "Nuh",
            "summary_en": "Builder of the Ark",
            "summary_id": "Pembuat bahtera",
            "story_en": "Story of Nuh",
            "story_id": "Kisah Nabi Nuh",
            "miracles_en": "The Great Flood",
            "miracles_id": "Banjir besar"
          }
        ]
    """.trimIndent()

    @Before
    fun setUp() {
        assetManager = mockk()
        context = mockk(relaxed = true)
        every { context.assets } returns assetManager
    }

    @Test
    fun `getProphets parses asset json and caches subsequent calls`() {
        every { assetManager.open("quran-data/prophets/prophets.json") } returns ByteArrayInputStream(
            sampleJson.toByteArray()
        )

        val repo = ProphetRepositoryImpl(context)

        val first = repo.getProphets()
        assertTrue(first is Result.Success)
        val prophets = (first as Result.Success).data
        assertEquals(2, prophets.size)
        assertEquals("Adam", prophets[0].nameEn)
        assertEquals("Primordial", prophets[0].eraEn)
        assertEquals(1, prophets[0].verses.size)
        assertEquals("Nuh", prophets[1].nameEn)

        val second = repo.getProphets()
        assertTrue(second is Result.Success)
        assertEquals(2, (second as Result.Success).data.size)
        verify(exactly = 1) { assetManager.open("quran-data/prophets/prophets.json") }
    }

    @Test
    fun `getProphetById finds matching prophet and returns null when absent`() {
        every { assetManager.open("quran-data/prophets/prophets.json") } returns ByteArrayInputStream(
            sampleJson.toByteArray()
        )

        val repo = ProphetRepositoryImpl(context)

        val found = repo.getProphetById(2)
        assertTrue(found is Result.Success)
        assertEquals("Nuh", (found as Result.Success).data?.nameEn)

        val missing = repo.getProphetById(999)
        assertTrue(missing is Result.Success)
        assertNull((missing as Result.Success).data)
        verify(exactly = 1) { assetManager.open("quran-data/prophets/prophets.json") }
    }

    @Test
    fun `getProphets and getProphetById return Database error when asset fails`() {
        every { assetManager.open("quran-data/prophets/prophets.json") } throws RuntimeException("Missing asset")

        val repo = ProphetRepositoryImpl(context)

        val listResult = repo.getProphets()
        assertTrue(listResult is Result.Error)
        assertTrue((listResult as Result.Error).error is AppError.Database)

        val itemResult = repo.getProphetById(1)
        assertTrue(itemResult is Result.Error)
        assertTrue((itemResult as Result.Error).error is AppError.Database)
    }

    @Test
    fun `getProphets returns Database error on malformed json`() {
        every { assetManager.open("quran-data/prophets/prophets.json") } returns ByteArrayInputStream(
            "{not valid json".toByteArray()
        )

        val repo = ProphetRepositoryImpl(context)
        val result = repo.getProphets()

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Database)
    }
}
