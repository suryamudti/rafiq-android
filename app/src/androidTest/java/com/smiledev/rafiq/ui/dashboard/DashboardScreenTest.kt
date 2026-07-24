package com.smiledev.rafiq.ui.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.PrayerTimings
import com.smiledev.rafiq.domain.model.PrayerTimesData
import com.smiledev.rafiq.domain.repository.PrayerTimesRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createViewModel(): DashboardViewModel {
        val repo = mockk<PrayerTimesRepository>(relaxed = true)
        coEvery { repo.fetchPrayerTimes(any(), any(), any(), any()) } returns Result.Success(
            PrayerTimesData(
                timings = PrayerTimings("05:00", "05:30", "06:30", "12:00", "15:30", "18:00", "19:00"),
                hijriDate = "1 Muharram 1446"
            )
        )
        val prefs = mockk<PreferencesManager>(relaxed = true)
        every { prefs.latitude } returns MutableStateFlow("")
        every { prefs.longitude } returns MutableStateFlow("")
        every { prefs.prayerCalculationMethod } returns MutableStateFlow(2)
        return DashboardViewModel(repo, prefs, DefaultDispatcherProvider)
    }

    @Test
    fun salamAndSubtitleAreDisplayed() {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            DashboardScreen(onNavigate = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("السلام عليكم").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your Islamic companion").assertIsDisplayed()
    }

    @Test
    fun featureCardsAreDisplayed() {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            DashboardScreen(onNavigate = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Quran").assertIsDisplayed()
        composeTestRule.onNodeWithText("Qibla").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tasbih").assertIsDisplayed()
    }
}
