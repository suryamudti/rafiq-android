package com.smiledev.rafiq_quran.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedSettingsViewModelTest {

    @Test
    fun initialState_hasCorrectDefaults() {
        val viewModel = SharedSettingsViewModel()
        val state = viewModel.uiState.value

        assertEquals(ThemePreference.SYSTEM, state.theme)
        assertEquals(LanguagePreference.INDONESIAN, state.language)
        assertTrue(state.prayerNotifications)
        assertTrue(state.tasbihVibration)
        assertEquals(QuranFontSize.MEDIUM, state.quranFontSize)
    }

    @Test
    fun setTheme_updatesTheme() {
        val viewModel = SharedSettingsViewModel()

        viewModel.setTheme(ThemePreference.DARK)
        assertEquals(ThemePreference.DARK, viewModel.uiState.value.theme)

        viewModel.setTheme(ThemePreference.LIGHT)
        assertEquals(ThemePreference.LIGHT, viewModel.uiState.value.theme)
    }

    @Test
    fun setLanguage_updatesLanguage() {
        val viewModel = SharedSettingsViewModel()

        viewModel.setLanguage(LanguagePreference.ENGLISH)
        assertEquals(LanguagePreference.ENGLISH, viewModel.uiState.value.language)
    }

    @Test
    fun toggleFlags_invertsState() {
        val viewModel = SharedSettingsViewModel()

        viewModel.togglePrayerNotifications()
        assertFalse(viewModel.uiState.value.prayerNotifications)

        viewModel.toggleTasbihVibration()
        assertFalse(viewModel.uiState.value.tasbihVibration)
    }

    @Test
    fun setQuranFontSize_updatesScale() {
        val viewModel = SharedSettingsViewModel()

        viewModel.setQuranFontSize(QuranFontSize.LARGE)
        assertEquals(QuranFontSize.LARGE, viewModel.uiState.value.quranFontSize)
        assertEquals(1.25f, viewModel.uiState.value.quranFontSize.scaleFactor)
    }
}
