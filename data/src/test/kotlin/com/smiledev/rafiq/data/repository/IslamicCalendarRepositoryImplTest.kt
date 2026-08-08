package com.smiledev.rafiq.data.repository

import android.content.Context
import android.content.res.AssetManager
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.GregorianDate
import com.smiledev.rafiq.domain.util.TodayProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class IslamicCalendarRepositoryImplTest {

    private val assetManager: AssetManager = mockk()
    private lateinit var repo: IslamicCalendarRepositoryImpl

    @Before
    fun setUp() {
        val context: Context = mockk(relaxed = true)
        every { context.assets } returns assetManager
        repo = IslamicCalendarRepositoryImpl(
            context,
            todayProvider = TodayProvider { GregorianDate(2025, 7, 5) }
        )
    }

    @Test
    fun `getTodayEvents matches today via hijri conversion`() {
        val json = """
            [
              {"hijri_month": 1, "hijri_day": 10, "title_en": "Day of Ashura", "title_id": "Hari Asyura", "description_en": "D", "description_id": "D", "event_type": "observance"},
              {"hijri_month": 1, "hijri_day": 1, "title_en": "Islamic New Year", "title_id": "Tahun Baru Islam", "description_en": "D", "description_id": "D", "event_type": "holiday"}
            ]
        """.trimIndent()
        every { assetManager.open("quran-data/islamic_events.json") } returns ByteArrayInputStream(json.toByteArray())

        val result = repo.getTodayEvents()

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val events = (result as Result.Success).data
        assertEquals(1, events.size)
        assertEquals("Day of Ashura", events.single().titleEn)
    }

    @Test
    fun `getTodayEvents returns empty when nothing matches and no fallback`() {
        val json = """
            [
              {"hijri_month": 1, "hijri_day": 1, "title_en": "Islamic New Year", "title_id": "Tahun Baru Islam", "description_en": "D", "description_id": "D", "event_type": "holiday"}
            ]
        """.trimIndent()
        every { assetManager.open("quran-data/islamic_events.json") } returns ByteArrayInputStream(json.toByteArray())

        val result = repo.getTodayEvents()

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
    }

    @Test
    fun `getTodayEvents returns error on missing asset`() {
        every { assetManager.open("quran-data/islamic_events.json") } throws RuntimeException("File not found")

        val result = repo.getTodayEvents()

        assertTrue("Expected Error but got ${result}", result is Result.Error)
    }

    @Test
    fun `repository parses all 57 expanded events with valid fields`() {
        val resource = javaClass.classLoader.getResourceAsStream("expanded_events.json")
        assertNotNull("expanded_events.json test resource missing", resource)
        every { assetManager.open("quran-data/islamic_events.json") } returns ByteArrayInputStream(resource!!.readBytes())

        val result = repo.getEvents()

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val events = (result as Result.Success).data
        assertEquals(57, events.size)
        events.forEach { event ->
            assertTrue("month out of range: ${event.hijriMonth}", event.hijriMonth in 1..12)
            assertTrue("day out of range: ${event.hijriDay}", event.hijriDay in 1..30)
            assertTrue(event.titleEn.isNotBlank())
            assertTrue(event.titleId.isNotBlank())
            assertTrue(event.descriptionEn.isNotBlank())
            assertTrue(event.descriptionId.isNotBlank())
            assertTrue(event.eventType == "holiday" || event.eventType == "observance")
        }
    }
}
