package com.smiledev.rafiq_quran.ui.hadith

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasClickAction
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.HadithBook
import com.smiledev.rafiq_quran.domain.repository.HadithRepository
import com.smiledev.rafiq_quran.core.Result
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class HadithBooksScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testScope = TestScope()
    private val testDispatcher = UnconfinedTestDispatcher(testScope.testScheduler)

    private fun dispatcher() = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
        override val unconfined = testDispatcher
    }

    private val book = HadithBook("bukhari.1", "bukhari", 1, "كتاب بدء الوحي", "Revelation", "Permulaan Wahyu")

    @Test
    fun booksAreDisplayedAndClickNavigates() {
        val repo = mockk<HadithRepository>(relaxed = true)
        every { repo.getBooks() } returns Result.Success(listOf(book))
        val prefs = mockk<PreferencesManager>(relaxed = true)
        every { prefs.translationLanguage } returns MutableStateFlow("en")
        val viewModel = HadithBooksViewModel(repo, prefs, dispatcher())

        var clickedId: String? = null
        composeTestRule.setContent {
            HadithBooksScreen(
                onHadithBookClick = { clickedId = it },
                onBack = {},
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Revelation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Revelation").performClick()
        assertEquals("bukhari.1", clickedId)
    }

    @Test
    fun collectionSubtitleShownPerBook() {
        val repo = mockk<HadithRepository>(relaxed = true)
        every { repo.getBooks() } returns Result.Success(listOf(book))
        val prefs = mockk<PreferencesManager>(relaxed = true)
        every { prefs.translationLanguage } returns MutableStateFlow("en")
        val viewModel = HadithBooksViewModel(repo, prefs, dispatcher())

        composeTestRule.setContent {
            HadithBooksScreen(onHadithBookClick = {}, onBack = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Sahih al-Bukhari · Book 1").assertIsDisplayed()
    }
}