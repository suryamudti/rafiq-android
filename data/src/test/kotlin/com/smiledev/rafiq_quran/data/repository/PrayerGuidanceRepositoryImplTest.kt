package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import android.content.res.AssetManager
import com.smiledev.rafiq_quran.core.Result
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PrayerGuidanceRepositoryImplTest {

    private val context: Context = mockk()
    private val assetManager: AssetManager = mockk()

    @Test
    fun `getGuidanceList parses actual prayer_guidance json file`() {
        val file = File("../app/src/main/assets/quran-data/prayer_guidance.json")
        assertTrue("Asset file must exist at ${file.canonicalPath}", file.exists())

        every { context.assets } returns assetManager
        every { assetManager.open("quran-data/prayer_guidance.json") } answers {
            file.inputStream()
        }

        val repo = PrayerGuidanceRepositoryImpl(context)
        val result = repo.getGuidanceList()

        assertTrue("Expected Success but got $result", result is Result.Success)
        val items = (result as Result.Success).data
        assertEquals(19, items.size)
    }

    @Test
    fun `getGuidanceById returns item when found`() {
        val file = File("../app/src/main/assets/quran-data/prayer_guidance.json")
        every { context.assets } returns assetManager
        every { assetManager.open("quran-data/prayer_guidance.json") } answers { file.inputStream() }

        val repo = PrayerGuidanceRepositoryImpl(context)
        val result = repo.getGuidanceById("wudhu")

        assertTrue("Expected Success but got $result", result is Result.Success)
        val item = (result as Result.Success).data
        assertEquals("wudhu", item.id)
        assertEquals("Tata Cara Wudhu", item.nameId)
        assertEquals(8, item.steps.size)
    }

    @Test
    fun `all guidance items have non-empty dalil and authentic references`() {
        val file = File("../app/src/main/assets/quran-data/prayer_guidance.json")
        every { context.assets } returns assetManager
        every { assetManager.open("quran-data/prayer_guidance.json") } answers { file.inputStream() }

        val repo = PrayerGuidanceRepositoryImpl(context)
        val result = repo.getGuidanceList()

        assertTrue("Expected Success but got $result", result is Result.Success)
        val items = (result as Result.Success).data
        assertEquals(19, items.size)

        for (item in items) {
            assertTrue("Item ${item.id} must have non-empty dalilArabic", !item.dalilArabic.isNullOrBlank())
            assertTrue(
                "Item ${item.id} must have hadithReference or surahReference",
                !item.hadithReference.isNullOrBlank() || !item.surahReference.isNullOrBlank()
            )
            assertTrue("Item ${item.id} must have steps", item.steps.isNotEmpty())
        }
    }

    @Test
    fun `obligatory prayers have complete step recitations`() {
        val file = File("../app/src/main/assets/quran-data/prayer_guidance.json")
        every { context.assets } returns assetManager
        every { assetManager.open("quran-data/prayer_guidance.json") } answers { file.inputStream() }

        val repo = PrayerGuidanceRepositoryImpl(context)
        val prayerIds = listOf("subuh", "dzuhur", "ashar", "maghrib", "isya")
        for (id in prayerIds) {
            val result = repo.getGuidanceById(id)
            assertTrue("Expected Success for $id", result is Result.Success)
            val item = (result as Result.Success).data
            for (step in item.steps) {
                assertTrue("Step ${step.order} in $id must have Arabic recitation", !step.arabic.isNullOrBlank())
                assertTrue("Step ${step.order} in $id must have transliteration", !step.transliteration.isNullOrBlank())
                assertTrue("Step ${step.order} in $id must have hadith reference", !step.hadithReference.isNullOrBlank())
            }
        }
    }

    @Test
    fun `sunnah prayers have authentic dalil and references`() {
        val file = File("../app/src/main/assets/quran-data/prayer_guidance.json")
        every { context.assets } returns assetManager
        every { assetManager.open("quran-data/prayer_guidance.json") } answers { file.inputStream() }

        val repo = PrayerGuidanceRepositoryImpl(context)
        val sunnahIds = listOf("tahajjud", "dhuha", "witir", "rawatib", "istikharah", "hajat", "taubat")
        for (id in sunnahIds) {
            val result = repo.getGuidanceById(id)
            assertTrue("Expected Success for $id", result is Result.Success)
            val item = (result as Result.Success).data
            assertTrue("Sunnah $id must have dalilArabic", !item.dalilArabic.isNullOrBlank())
            assertTrue("Sunnah $id must have hadithReference", !item.hadithReference.isNullOrBlank())
            assertTrue("Sunnah $id must have dalilTranslationId", !item.dalilTranslationId.isNullOrBlank())
        }
    }
}
