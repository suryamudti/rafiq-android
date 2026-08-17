package com.smiledev.rafiq.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.data.preferences.PreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testScope = TestScope()
    private val testDispatcher = UnconfinedTestDispatcher(testScope.testScheduler)

    private fun createDispatcherProvider() = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
        override val unconfined = testDispatcher
    }

    @Test
    fun allSectionsAreDisplayed() {
        val prefs = mockk<PreferencesManager>(relaxed = true)
        every { prefs.themeMode } returns MutableStateFlow("system")
        every { prefs.translationLanguage } returns MutableStateFlow("system")
        every { prefs.ayahFontSize } returns MutableStateFlow(22)
        every { prefs.translationFontSize } returns MutableStateFlow(15)
        coEvery { prefs.setThemeMode(any()) } returns Unit
        coEvery { prefs.setTranslationLanguage(any()) } returns Unit
        coEvery { prefs.setAyahFontSize(any()) } returns Unit
        coEvery { prefs.setTranslationFontSize(any()) } returns Unit
        val viewModel = SettingsViewModel(prefs, createDispatcherProvider())

        composeTestRule.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quran Translation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ayah Font Size: 22sp").assertIsDisplayed()
        composeTestRule.onNodeWithText("Translation Font Size: 15sp").assertIsDisplayed()
    }

    @Test
    fun radioOptionsAreDisplayed() {
        val prefs = mockk<PreferencesManager>(relaxed = true)
        every { prefs.themeMode } returns MutableStateFlow("system")
        every { prefs.translationLanguage } returns MutableStateFlow("system")
        every { prefs.ayahFontSize } returns MutableStateFlow(22)
        every { prefs.translationFontSize } returns MutableStateFlow(15)
        coEvery { prefs.setThemeMode(any()) } returns Unit
        coEvery { prefs.setTranslationLanguage(any()) } returns Unit
        coEvery { prefs.setAyahFontSize(any()) } returns Unit
        coEvery { prefs.setTranslationFontSize(any()) } returns Unit
        val viewModel = SettingsViewModel(prefs, createDispatcherProvider())

        composeTestRule.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Light").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dark").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bahasa Indonesia").assertIsDisplayed()
        composeTestRule.onNodeWithText("English").assertIsDisplayed()
        composeTestRule.onNodeWithText("Both (Bahasa & English)").assertIsDisplayed()
    }

    @Test
    fun clickingLightThemeCallsSetThemeMode() {
        val prefs = mockk<PreferencesManager>(relaxed = true)
        every { prefs.themeMode } returns MutableStateFlow("system")
        every { prefs.translationLanguage } returns MutableStateFlow("system")
        every { prefs.ayahFontSize } returns MutableStateFlow(22)
        every { prefs.translationFontSize } returns MutableStateFlow(15)
        coEvery { prefs.setThemeMode(any()) } returns Unit
        coEvery { prefs.setTranslationLanguage(any()) } returns Unit
        coEvery { prefs.setAyahFontSize(any()) } returns Unit
        coEvery { prefs.setTranslationFontSize(any()) } returns Unit
        val viewModel = SettingsViewModel(prefs, createDispatcherProvider())

        composeTestRule.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Light").performClick()
        coVerify { prefs.setThemeMode("light") }
    }
}
