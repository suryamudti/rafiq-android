package com.smiledev.rafiq.ui.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun titleAndSubtitleAreDisplayed() {
        composeTestRule.setContent {
            DashboardScreen(onNavigate = {})
        }

        composeTestRule.onNodeWithText("Rafiq App").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your Islamic companion").assertIsDisplayed()
    }

    @Test
    fun featureCardsAreDisplayed() {
        composeTestRule.setContent {
            DashboardScreen(onNavigate = {})
        }

        composeTestRule.onNodeWithText("Quran").assertIsDisplayed()
        composeTestRule.onNodeWithText("Prayer Times").assertIsDisplayed()
        composeTestRule.onNodeWithText("Qibla").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tasbih").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bookmarks").assertIsDisplayed()
    }
}
