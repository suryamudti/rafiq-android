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

        if (result is Result.Error) {
            println("Error message: ${result.error}")
        }
        assertTrue("Expected Success but got $result", result is Result.Success)
        val items = (result as Result.Success).data
        assertEquals(19, items.size)
        println("Loaded ${items.size} items successfully!")
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
}
