package com.smiledev.rafiq.ui.tasbih

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test

class TasbihScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun counterStartsAtZero() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = TasbihViewModel(context)

        composeTestRule.setContent {
            TasbihScreen(onBack = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap to count").assertIsDisplayed()
    }

    @Test
    fun incrementOnCardTap() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = TasbihViewModel(context)

        composeTestRule.setContent {
            TasbihScreen(onBack = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun resetSetsCounterBackToZero() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = TasbihViewModel(context)

        composeTestRule.setContent {
            TasbihScreen(onBack = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").assertIsDisplayed()

        composeTestRule.onNodeWithText("Reset").performClick()
        composeTestRule.onNodeWithText("0").assertIsDisplayed()
    }
}
