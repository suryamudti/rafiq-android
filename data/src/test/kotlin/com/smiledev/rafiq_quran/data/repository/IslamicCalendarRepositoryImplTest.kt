package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import android.content.res.AssetManager
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.GregorianDate
import com.smiledev.rafiq_quran.domain.util.TodayProvider
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
    private val context: Context = mockk(relaxed = true)
    private lateinit var repo: IslamicCalendarRepositoryImpl

    @Before
    fun setUp() {
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
    fun `getEvents parses optional weekday field`() {
        val json = """
            [
              {"hijri_month": 0, "hijri_day": 0, "weekday": 1, "title_en": "Monday Fasting (Sunnah)", "title_id": "Puasa Senin (Sunnah)", "description_en": "D", "description_id": "D", "event_type": "fasting"}
            ]
        """.trimIndent()
        every { assetManager.open("quran-data/islamic_events.json") } returns ByteArrayInputStream(json.toByteArray())

        val result = repo.getEvents()

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val event = (result as Result.Success).data.single()
        assertEquals(1, event.weekday)
        assertEquals(0, event.hijriMonth)
        assertEquals(0, event.hijriDay)
    }

    @Test
    fun `repository parses all 75 expanded events with valid fields`() {
        val resource = javaClass.classLoader.getResourceAsStream("expanded_events.json")
        assertNotNull("expanded_events.json test resource missing", resource)
        every { assetManager.open("quran-data/islamic_events.json") } returns ByteArrayInputStream(resource!!.readBytes())

        val result = repo.getEvents()

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val events = (result as Result.Success).data
        assertEquals(75, events.size)
        events.forEach { event ->
            assertTrue("month out of range: ${event.hijriMonth}", event.hijriMonth in 0..12)
            assertTrue("day out of range: ${event.hijriDay}", event.hijriDay in 0..30)
            event.weekday?.let { assertTrue("weekday out of range: $it", it in 0..6) }
            assertTrue(event.titleEn.isNotBlank())
            assertTrue(event.titleId.isNotBlank())
            assertTrue(event.descriptionEn.isNotBlank())
            assertTrue(event.descriptionId.isNotBlank())
            assertTrue(event.eventType in setOf("holiday", "observance", "fasting", "recommendation"))
        }
    }

    @Test
    fun `getTodayEvents matches weekly fasting event on a Monday`() {
        val json = """
            [
              {"hijri_month": 0, "hijri_day": 0, "weekday": 1, "title_en": "Monday Fasting (Sunnah)", "title_id": "Puasa Senin (Sunnah)", "description_en": "D", "description_id": "D", "event_type": "fasting"},
              {"hijri_month": 1, "hijri_day": 10, "title_en": "Day of Ashura", "title_id": "Hari Asyura", "description_en": "D", "description_id": "D", "event_type": "observance"}
            ]
        """.trimIndent()
        every { assetManager.open("quran-data/islamic_events.json") } returns ByteArrayInputStream(json.toByteArray())
        val mondayRepo = IslamicCalendarRepositoryImpl(
            context,
            todayProvider = TodayProvider { GregorianDate(2025, 7, 7) }
        )

        val result = mondayRepo.getTodayEvents()

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val events = (result as Result.Success).data
        assertEquals(1, events.size)
        assertEquals("Monday Fasting (Sunnah)", events.single().titleEn)
    }

    @Test
    fun `getTodayEvents matches month-wide recommendation for today's hijri month`() {
        val json = """
            [
              {"hijri_month": 8, "hijri_day": 0, "title_en": "Recommended: Fast in Sha'ban", "title_id": "Dianjurkan: Puasa di Bulan Sya'ban", "description_en": "D", "description_id": "D", "event_type": "recommendation"}
            ]
        """.trimIndent()
        every { assetManager.open("quran-data/islamic_events.json") } returns ByteArrayInputStream(json.toByteArray())
        val shaBanRepo = IslamicCalendarRepositoryImpl(
            context,
            todayProvider = TodayProvider { GregorianDate(2026, 1, 20) }
        )

        val result = shaBanRepo.getTodayEvents()

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val events = (result as Result.Success).data
        assertEquals(1, events.size)
        assertEquals("Recommended: Fast in Sha'ban", events.single().titleEn)
    }
}
