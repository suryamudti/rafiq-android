package com.smiledev.rafiq

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.smiledev.rafiq.theme.RafiqAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ExampleInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardShowsRafiqApp() {
        composeTestRule.setContent {
            RafiqAppTheme {
                com.smiledev.rafiq.ui.dashboard.DashboardScreen(
                    onNavigate = { },
                    modifier = Modifier
                )
            }
        }
        composeTestRule.onNodeWithText("Rafiq App").assertExists()
    }
}

private val Modifier: androidx.compose.ui.Modifier
    get() = androidx.compose.ui.Modifier.Companion
