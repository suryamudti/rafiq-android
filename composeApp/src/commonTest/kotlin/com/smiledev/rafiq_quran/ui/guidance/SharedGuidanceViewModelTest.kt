package com.smiledev.rafiq_quran.ui.guidance

import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceCategory
import com.smiledev.rafiq_quran.domain.model.SunnahCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedGuidanceViewModelTest {

    @Test
    fun initialState_loadsDefaults() {
        val viewModel = SharedGuidanceViewModel()
        val state = viewModel.uiState.value

        assertEquals(GuidanceTab.PRAYER, state.selectedTab)
        assertTrue(state.prayerItems.isNotEmpty())
        assertTrue(state.sunnahItems.isNotEmpty())
        assertNull(state.selectedPrayerItem)
        assertNull(state.selectedSunnahItem)
        assertEquals("", state.searchQuery)
    }

    @Test
    fun selectTab_switchesTabAndClearsSelections() {
        val viewModel = SharedGuidanceViewModel()
        viewModel.selectPrayerItem(viewModel.uiState.value.prayerItems.first())
        assertNotNull(viewModel.uiState.value.selectedPrayerItem)

        viewModel.selectTab(GuidanceTab.SUNNAH)
        assertEquals(GuidanceTab.SUNNAH, viewModel.uiState.value.selectedTab)
        assertNull(viewModel.uiState.value.selectedPrayerItem)
        assertNull(viewModel.uiState.value.selectedSunnahItem)
    }

    @Test
    fun filterPrayerItems_matchesSearchQuery() {
        val viewModel = SharedGuidanceViewModel()
        viewModel.onSearchQueryChange("Fajr")

        val filtered = viewModel.getFilteredPrayerItems()
        assertEquals(1, filtered.size)
        assertEquals("Fajr Prayer", filtered.first().nameEn)
    }

    @Test
    fun filterSunnahItems_matchesSearchAndCategory() {
        val viewModel = SharedGuidanceViewModel()
        viewModel.selectSunnahCategory(SunnahCategory.FRIDAY)

        val filtered = viewModel.getFilteredSunnahItems()
        assertEquals(1, filtered.size)
        assertEquals("Friday Sunnahs", filtered.first().titleEn)
    }

    @Test
    fun selectPrayerCategory_filtersByCategory() {
        val viewModel = SharedGuidanceViewModel()
        viewModel.selectPrayerCategory(PrayerGuidanceCategory.PURIFICATION)

        val filtered = viewModel.getFilteredPrayerItems()
        assertEquals(1, filtered.size)
        assertEquals("wudu", filtered.first().id)
    }
}
