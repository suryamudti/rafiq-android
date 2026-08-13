package com.smiledev.rafiq.ui.hadith

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.Hadith
import com.smiledev.rafiq.domain.model.HadithBook
import com.smiledev.rafiq.domain.repository.HadithRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class HadithListScreenTest {

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
    private val hadith = Hadith(1, "bukhari.1", 1, "Narrator", "Narrator", "نص عربي", "English text", "Teks Indonesia")

    private fun viewModel(): HadithListViewModel {
        val repo = mockk<HadithRepository>(relaxed = true)
        every { repo.getBooks() } returns Result.Success(listOf(book))
        every { repo.getHadithsByBook("bukhari.1") } returns Result.Success(listOf(hadith))
        val prefs = mockk<PreferencesManager>(relaxed = true)
        every { prefs.translationLanguage } returns MutableStateFlow("en")
        return HadithListViewModel(repo, prefs, dispatcher()).apply { load("bukhari.1") }
    }

    @Test
    fun hadithCardShownAndClickNavigates() {
        composeTestRule.setContent {
            HadithListScreen(bookId = "bukhari.1", onHadithClick = {}, onBack = {}, viewModel = viewModel())
        }

        composeTestRule.onNodeWithText("Book 1 · Hadith 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Book 1 · Hadith 1").performClick()
    }

    @Test
    fun titleShowsBookName() {
        composeTestRule.setContent {
            HadithListScreen(bookId = "bukhari.1", onHadithClick = {}, onBack = {}, viewModel = viewModel())
        }

        composeTestRule.onNodeWithText("Revelation").assertIsDisplayed()
        assertEquals(1, composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Book 1 · Hadith 1")).fetchSemanticsNodes().size)
    }
}