package com.smiledev.rafiq_quran.ui.hadith

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.Hadith
import com.smiledev.rafiq_quran.domain.model.HadithBook
import com.smiledev.rafiq_quran.domain.repository.HadithRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class HadithSearchScreenTest {

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

    private fun viewModel(query: String, result: List<Hadith>): HadithSearchViewModel {
        val repo = mockk<HadithRepository>(relaxed = true)
        every { repo.getBooks() } returns Result.Success(listOf(book))
        every { repo.searchHadiths(any(), 100) } returns Result.Success(result)
        val prefs = mockk<PreferencesManager>(relaxed = true)
        every { prefs.translationLanguage } returns MutableStateFlow("en")
        return HadithSearchViewModel(repo, prefs, dispatcher()).apply {
            search(query)
            testScope.testScheduler.advanceUntilIdle()
        }
    }

    @Test
    fun showsHintWhenQueryBlank() {
        composeTestRule.setContent {
            HadithSearchScreen(onHadithClick = {}, onBack = {}, viewModel = viewModel("", emptyList()))
        }

        composeTestRule.onNodeWithText("Type to search the whole hadith collection").assertIsDisplayed()
    }

    @Test
    fun showsResultAndTappingCallsOnHadithClick() {
        var clicked: Int? = null
        composeTestRule.setContent {
            HadithSearchScreen(onHadithClick = { clicked = it }, onBack = {}, viewModel = viewModel("English", listOf(hadith)))
        }

        composeTestRule.onNodeWithText("English text").assertIsDisplayed()
        composeTestRule.onNodeWithText("English text").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, clicked)
    }

    @Test
    fun showsNoMatchMessageWhenQueryHasNoResults() {
        composeTestRule.setContent {
            HadithSearchScreen(onHadithClick = {}, onBack = {}, viewModel = viewModel("zzz", emptyList()))
        }

        composeTestRule.onNodeWithText("No hadiths match \"zzz\"").assertIsDisplayed()
    }
}
