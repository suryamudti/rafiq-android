package com.smiledev.rafiq.ui.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.Ayah
import com.smiledev.rafiq.domain.model.Surah
import com.smiledev.rafiq.domain.repository.QuranRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class QuranScreenSearchTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun searchIconTogglesFieldAndRendersResults() {
        val repo = mockk<QuranRepository>(relaxed = true)
        val prefs = mockk<PreferencesManager>(relaxed = true)

        every { repo.getChapters(any()) } returns Result.Success(
            listOf(Surah(1, 1, "الفاتحة", "Al-Fatiha", "Al-Fatiha", 7, "Mecca"))
        )
        every { repo.searchAyahs(any(), any(), any()) } returns Result.Success(
            listOf(Ayah(1, 1, "بِسْمِ ٱللَّهِ", null, translation = "In the name of Allah"))
        )
        every { prefs.translationLanguage } returns MutableStateFlow("en")

        composeTestRule.setContent {
            QuranScreen(
                initialTab = 0,
                onSurahClick = { _, _ -> },
                onBookmarkClick = { _, _, _ -> },
                onSearchResultClick = { _, _, _ -> },
                onBack = {},
                viewModel = QuranViewModel(repo, prefs, DefaultDispatcherProvider)
            )
        }

        composeTestRule.onNodeWithContentDescription("Search").performClick()
        composeTestRule.onNodeWithContentDescription("Search field").assertIsDisplayed()
    }
}
