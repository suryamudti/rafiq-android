package com.smiledev.rafiq_quran.ui.sources

import com.smiledev.rafiq_quran.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourcesCatalogTest {

    private val sections = sourcesSections()

    @Test
    fun `has exactly three sections in order`() {
        assertEquals(3, sections.size)
        assertEquals(R.string.source_section_quran, sections[0].titleRes)
        assertEquals(R.string.source_section_hadith, sections[1].titleRes)
        assertEquals(R.string.source_section_prayer_times, sections[2].titleRes)
    }

    @Test
    fun `has expected item counts per section`() {
        assertEquals(3, sections[0].items.size)
        assertEquals(2, sections[1].items.size)
        assertEquals(2, sections[2].items.size)
        assertEquals(7, sections.sumOf { it.items.size })
    }

    @Test
    fun `every item has non-zero resource ids`() {
        sections.forEach { section ->
            assertTrue("section titleRes must not be 0", section.titleRes != 0)
            section.items.forEach { item ->
                assertTrue("item titleRes must not be 0", item.titleRes != 0)
                assertTrue("item descriptionRes must not be 0", item.descriptionRes != 0)
                assertTrue("item authenticityRes must not be 0", item.authenticityRes != 0)
            }
        }
    }

    @Test
    fun `every link is https or absent`() {
        sections.forEach { section ->
            section.items.forEach { item ->
                item.linkUrl?.let { url ->
                    assertTrue("link should be https: $url", url.startsWith("https://"))
                }
            }
        }
    }

    @Test
    fun `hadith items carry authentic collection labels`() {
        val hadith = sections[1]
        assertTrue(hadith.items.all { it.authenticityRes == R.string.source_authentic_collection })
    }

    @Test
    fun `only the default location item has no link`() {
        val noLink = sections.flatMap { it.items }.filter { it.linkUrl == null }
        assertEquals(1, noLink.size)
        assertEquals(R.string.source_prayer_location, noLink[0].titleRes)
    }
}
