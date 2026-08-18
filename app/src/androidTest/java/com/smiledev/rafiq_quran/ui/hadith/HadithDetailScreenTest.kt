package com.smiledev.rafiq_quran.ui.hadith

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
import org.junit.Rule
import org.junit.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class HadithDetailScreenTest {

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

    private fun viewModel(lang: String): HadithListViewModel {
        val repo = mockk<HadithRepository>(relaxed = true)
        every { repo.getBooks() } returns Result.Success(listOf(book))
        every { repo.getHadithsByBook("bukhari.1") } returns Result.Success(listOf(hadith))
        every { repo.getHadithById(1) } returns Result.Success(hadith)
        val prefs = mockk<PreferencesManager>(relaxed = true)
        every { prefs.translationLanguage } returns MutableStateFlow(lang)
        return HadithListViewModel(repo, prefs, dispatcher()).apply { load("bukhari.1") }
    }

    @Test
    fun showsArabicAndReferenceLine() {
        composeTestRule.setContent {
            HadithDetailScreen(hadithId = 1, onBack = {}, viewModel = viewModel("en"))
        }

        composeTestRule.onNodeWithText("نص عربي").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sahih al-Bukhari · Book 1, Hadith 1").assertIsDisplayed()
    }

    @Test
    fun enModeShowsEnglishTranslationOnly() {
        composeTestRule.setContent {
            HadithDetailScreen(hadithId = 1, onBack = {}, viewModel = viewModel("en"))
        }

        composeTestRule.onNodeWithText("English text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Teks Indonesia").assertDoesNotExist()
    }

    @Test
    fun idModeShowsIndonesianTranslationOnly() {
        composeTestRule.setContent {
            HadithDetailScreen(hadithId = 1, onBack = {}, viewModel = viewModel("id"))
        }

        composeTestRule.onNodeWithText("Teks Indonesia").assertIsDisplayed()
        composeTestRule.onNodeWithText("English text").assertDoesNotExist()
    }

    @Test
    fun bothModeShowsBothTranslationsWithChips() {
        composeTestRule.setContent {
            HadithDetailScreen(hadithId = 1, onBack = {}, viewModel = viewModel("both"))
        }

        composeTestRule.onNodeWithText("English text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Teks Indonesia").assertIsDisplayed()
        composeTestRule.onNodeWithText("ID").assertIsDisplayed()
        composeTestRule.onNodeWithText("EN").assertIsDisplayed()
    }
}
