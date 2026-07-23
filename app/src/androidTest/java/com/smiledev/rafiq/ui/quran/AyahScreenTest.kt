package com.smiledev.rafiq.ui.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.Ayah
import com.smiledev.rafiq.domain.model.Surah
import com.smiledev.rafiq.domain.repository.BookmarkRepository
import com.smiledev.rafiq.domain.repository.QuranRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class AyahScreenTest {

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
    fun ayahListRendersWithTranslation() {
        val quranRepo = mockk<QuranRepository>(relaxed = true)
        val bookmarkRepo = mockk<BookmarkRepository>(relaxed = true)
        val prefs = mockk<PreferencesManager>(relaxed = true)

        every { quranRepo.getChapters("en") } returns Result.Success(
            listOf(Surah(1, 1, "الفاتحة", "Al-Fatiha", "The Opening", 7, "Mecca"))
        )
        every { quranRepo.getAyahsWithTranslation(1, "en") } returns Result.Success(
            listOf(Ayah(1, 1, "بِسْمِ ٱللَّهِ", null, translationEn = "In the name of Allah"))
        )
        every { bookmarkRepo.observeAll() } returns emptyFlow()
        coEvery { bookmarkRepo.isBookmarked(any(), any()) } returns Result.Success(false)
        every { prefs.translationLanguage } returns MutableStateFlow("en")
        every { prefs.ayahFontSize } returns MutableStateFlow(22)
        every { prefs.translationFontSize } returns MutableStateFlow(15)

        val viewModel = QuranViewModel(quranRepo, bookmarkRepo, prefs, createDispatcherProvider())

        composeTestRule.setContent {
            AyahScreen(suraNumber = 1, suraName = "Al-Fatiha", onBack = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("1. Al-Fatiha").assertIsDisplayed()
        composeTestRule.onNodeWithText("1. In the name of Allah").assertIsDisplayed()
    }
}
