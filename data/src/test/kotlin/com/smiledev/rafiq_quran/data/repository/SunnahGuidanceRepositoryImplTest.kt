package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import android.content.res.AssetManager
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.SunnahCategory
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SunnahGuidanceRepositoryImplTest {

    private val context: Context = mockk()
    private val assetManager: AssetManager = mockk()

    @Test
    fun `getSunnahList parses actual sunnah_guidance json file successfully`() {
        val file = File("../app/src/main/assets/quran-data/sunnah_guidance.json")
        assertTrue("Asset file must exist at ${file.canonicalPath}", file.exists())

        every { context.assets } returns assetManager
        every { assetManager.open("quran-data/sunnah_guidance.json") } answers {
            file.inputStream()
        }

        val repo = SunnahGuidanceRepositoryImpl(context)
        val result = repo.getSunnahList()

        assertTrue("Expected Success but got $result", result is Result.Success)
        val items = (result as Result.Success).data
        assertEquals(17, items.size)

        // Verify that all items have required fields including Hadith/Surah dalil
        items.forEach { item ->
            assertTrue("Item id should not be blank", item.id.isNotBlank())
            assertTrue("Item titleEn should not be blank", item.titleEn.isNotBlank())
            assertTrue("Item titleId should not be blank", item.titleId.isNotBlank())
            assertTrue("Item titleArabic should not be blank", item.titleArabic.isNotBlank())
            assertTrue("Item summaryEn should not be blank", item.summaryEn.isNotBlank())
            assertTrue("Item steps should not be empty", item.steps.isNotEmpty())
            assertTrue(
                "Item ${item.id} should have hadithReference or surahReference",
                item.hadithReference != null || item.surahReference != null
            )
        }
    }

    @Test
    fun `getSunnahById returns specific item with authentic dalil`() {
        val file = File("../app/src/main/assets/quran-data/sunnah_guidance.json")
        every { context.assets } returns assetManager
        every { assetManager.open("quran-data/sunnah_guidance.json") } answers { file.inputStream() }

        val repo = SunnahGuidanceRepositoryImpl(context)
        val result = repo.getSunnahById("tahajjud")

        assertTrue("Expected Success but got $result", result is Result.Success)
        val item = (result as Result.Success).data
        assertEquals("tahajjud", item.id)
        assertEquals(SunnahCategory.PRAYER, item.category)
        assertNotNull(item.hadithReference)
        assertNotNull(item.surahReference)
        assertNotNull(item.dalilArabic)
        assertEquals(6, item.steps.size)
        assertTrue(item.virtuesEn.isNotEmpty())
    }

    @Test
    fun `getSunnahByCategory filters items correctly`() {
        val file = File("../app/src/main/assets/quran-data/sunnah_guidance.json")
        every { context.assets } returns assetManager
        every { assetManager.open("quran-data/sunnah_guidance.json") } answers { file.inputStream() }

        val repo = SunnahGuidanceRepositoryImpl(context)
        val prayerResult = repo.getSunnahByCategory(SunnahCategory.PRAYER)
        assertTrue(prayerResult is Result.Success)
        val prayerItems = (prayerResult as Result.Success).data
        assertTrue(prayerItems.isNotEmpty())
        assertTrue(prayerItems.all { it.category == SunnahCategory.PRAYER })

        val fridayResult = repo.getSunnahByCategory(SunnahCategory.FRIDAY)
        assertTrue(fridayResult is Result.Success)
        val fridayItems = (fridayResult as Result.Success).data
        assertEquals(1, fridayItems.size)
        assertEquals("sunnah_hari_jumat", fridayItems[0].id)
    }
}
