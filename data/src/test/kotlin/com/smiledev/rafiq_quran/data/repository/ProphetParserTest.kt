package com.smiledev.rafiq.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProphetParserTest {

    @Test
    fun `parses all new fields`() {
        val json = """
            [
              {
                "id": 1,
                "name_arabic": "آدم",
                "name_en": "Adam",
                "name_id": "Adam",
                "summary_en": "S",
                "summary_id": "S",
                "story_en": "S",
                "story_id": "S",
                "miracles_en": "M",
                "miracles_id": "M",
                "era_en": "Primordial era",
                "era_id": "Zaman purba",
                "people_en": "All of humanity",
                "people_id": "Seluruh umat manusia",
                "lifespan_en": "~1000 years",
                "lifespan_id": "±1000 tahun",
                "events_en": ["Created from clay", "Sent to Earth"],
                "events_id": ["Diciptakan dari tanah liat", "Diturunkan ke Bumi"],
                "lessons_en": ["Humility defeats pride"],
                "lessons_id": ["Kerendahan hati menang"],
                "verses": [
                  {"surah": 2, "surah_name_en": "Al-Baqarah", "surah_name_id": "Al-Baqarah", "ayah_start": 30, "ayah_end": 39}
                ]
              }
            ]
        """.trimIndent()

        val result = parseProphets(json)

        assertEquals(1, result.size)
        val p = result[0]
        assertEquals("Primordial era", p.eraEn)
        assertEquals("Zaman purba", p.eraId)
        assertEquals("All of humanity", p.peopleEn)
        assertEquals("~1000 years", p.lifespanEn)
        assertEquals(listOf("Created from clay", "Sent to Earth"), p.eventsEn)
        assertEquals(listOf("Diciptakan dari tanah liat", "Diturunkan ke Bumi"), p.eventsId)
        assertEquals(listOf("Humility defeats pride"), p.lessonsEn)
        assertEquals(1, p.verses.size)
        assertEquals(2, p.verses[0].surah)
        assertEquals("Al-Baqarah", p.verses[0].surahNameEn)
        assertEquals(39, p.verses[0].ayahEnd)
    }

    @Test
    fun `missing new fields default to empty`() {
        val json = """
            [
              {
                "id": 2,
                "name_arabic": "نوح",
                "name_en": "Nuh",
                "name_id": "Nuh",
                "summary_en": "S",
                "summary_id": "S",
                "story_en": "S",
                "story_id": "S",
                "miracles_en": "M",
                "miracles_id": "M"
              }
            ]
        """.trimIndent()

        val result = parseProphets(json)

        assertEquals(1, result.size)
        val p = result[0]
        assertEquals("", p.eraEn)
        assertTrue(p.eventsEn.isEmpty())
        assertTrue(p.lessonsId.isEmpty())
        assertTrue(p.verses.isEmpty())
    }
}
