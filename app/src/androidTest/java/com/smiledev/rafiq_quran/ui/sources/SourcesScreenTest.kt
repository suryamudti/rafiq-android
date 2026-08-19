package com.smiledev.rafiq_quran.ui.sources

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class SourcesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sectionsAndItemsAreDisplayed() {
        composeTestRule.setContent { SourcesScreen(onBack = {}) }

        composeTestRule.onNodeWithText("Quran").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hadith").assertIsDisplayed()
        composeTestRule.onNodeWithText("Prayer Times").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Sahih al-Bukhari").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Sahih Muslim").performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Open source")[0].performScrollTo().assertIsDisplayed()
    }

    @Test
    fun hadithItemsShowTranslatorCredit() {
        composeTestRule.setContent { SourcesScreen(onBack = {}) }

        composeTestRule.onNodeWithText("Translator: Muhsin Khan").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Translator: Abdul Hamid Siddiqui").performScrollTo().assertIsDisplayed()
    }
}
