package com.smiledev.rafiq_quran.wear.ui.quran

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.domain.model.Surah
import com.smiledev.rafiq_quran.domain.repository.QuranRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class TestDispatcherProvider(
    private val testDispatcher: CoroutineDispatcher
) : DispatcherProvider {
    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
    override val unconfined: CoroutineDispatcher = testDispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class WearQuranViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)

    private val quranRepository: QuranRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)

    private val translationLanguageFlow = MutableStateFlow("system")
    private val lastReadSuraFlow = MutableStateFlow(0)
    private val lastReadAyaFlow = MutableStateFlow(0)
    private val showAyahTranslationFlow = MutableStateFlow(true)

    private val surah1 = Surah(
        id = 1,
        chapterNumber = 1,
        nameArabic = "الفاتحة",
        nameSimple = "Al-Fatihah",
        translatedName = "The Opening",
        versesCount = 7,
        revelationPlace = "makkah"
    )

    private val surah2 = Surah(
        id = 2,
        chapterNumber = 2,
        nameArabic = "البقرة",
        nameSimple = "Al-Baqarah",
        translatedName = "The Cow",
        versesCount = 286,
        revelationPlace = "madinah"
    )

    private val surah36 = Surah(
        id = 36,
        chapterNumber = 36,
        nameArabic = "يس",
        nameSimple = "Ya-Sin",
        translatedName = "Ya Sin",
        versesCount = 83,
        revelationPlace = "meccan"
    )

    private val ayah1 = Ayah(
        sura = 1,
        aya = 1,
        text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        bismillah = null,
        translation = "In the name of Allah",
        translationId = "Dengan nama Allah",
        translationEn = "In the name of Allah",
        page = 1,
        juz = 1
    )

    private val ayah2 = Ayah(
        sura = 1,
        aya = 2,
        text = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ",
        bismillah = null,
        translation = "Praise be to Allah",
        translationId = "Segala puji bagi Allah",
        translationEn = "Praise be to Allah",
        page = 1,
        juz = 1
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { preferencesManager.translationLanguage } returns translationLanguageFlow
        every { preferencesManager.lastReadSura } returns lastReadSuraFlow
        every { preferencesManager.lastReadAya } returns lastReadAyaFlow
        every { preferencesManager.showAyahTranslation } returns showAyahTranslationFlow

        coEvery { preferencesManager.setShowAyahTranslation(any()) } returns Unit
        coEvery { preferencesManager.setLastReadPosition(any(), any()) } returns Unit

        every { quranRepository.getChapters(any()) } returns Result.Success(listOf(surah1, surah2, surah36))
        every { quranRepository.getAyahsWithTranslation(any(), any()) } returns Result.Success(listOf(ayah1, ayah2))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel(): WearQuranViewModel {
        return WearQuranViewModel(
            quranRepository = quranRepository,
            preferencesManager = preferencesManager,
            dispatcherProvider = testDispatcherProvider
        )
    }

    @Test
    fun `loadSurahs populates surahs and setRevelationFilter filters Meccan vs Medinan accurately`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testScheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals(3, state.surahs.size)
        assertEquals("Al-Fatihah", state.surahs[0].nameSimple)
        assertFalse(state.isLoadingSurahs)
        assertNull(state.surahsError)

        // ALL filter
        assertEquals(WearRevelationFilter.ALL, state.revelationFilter)
        assertEquals(3, state.filteredSurahs.size)

        // MECCAN filter (includes "makkah" and "meccan")
        vm.setRevelationFilter(WearRevelationFilter.MECCAN)
        val meccanFiltered = vm.uiState.value.filteredSurahs
        assertEquals(2, meccanFiltered.size)
        assertTrue(meccanFiltered.any { it.chapterNumber == 1 })
        assertTrue(meccanFiltered.any { it.chapterNumber == 36 })
        assertFalse(meccanFiltered.any { it.chapterNumber == 2 })

        // MEDINAN filter (includes "madinah" and "medinan")
        vm.setRevelationFilter(WearRevelationFilter.MEDINAN)
        val medinanFiltered = vm.uiState.value.filteredSurahs
        assertEquals(1, medinanFiltered.size)
        assertEquals(2, medinanFiltered[0].chapterNumber)
    }

    @Test
    fun `loadAyahs loads ayahs for Surah 1 and sets currentSurah`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testScheduler.runCurrent()

        vm.loadAyahs(1)
        testScheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals(1, state.suraNumber)
        assertNotNull(state.currentSurah)
        assertEquals(1, state.currentSurah?.chapterNumber)
        assertEquals("Al-Fatihah", state.currentSurah?.nameSimple)
        assertEquals(2, state.ayahs.size)
        assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", state.ayahs[0].text)
        assertFalse(state.isLoadingAyahs)
        assertNull(state.ayahsError)
    }

    @Test
    fun `toggleShowTranslation and saveLastRead invoke preferencesManager`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testScheduler.runCurrent()

        assertTrue(vm.uiState.value.showTranslation)
        vm.toggleShowTranslation()
        testScheduler.runCurrent()

        coVerify(exactly = 1) { preferencesManager.setShowAyahTranslation(false) }

        vm.saveLastRead(sura = 2, aya = 255)
        testScheduler.runCurrent()

        coVerify(exactly = 1) { preferencesManager.setLastReadPosition(2, 255) }
    }

    @Test
    fun `getTranslationText returns translation according to language preferences`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testScheduler.runCurrent()

        // Default / fallback
        val textEn = vm.getTranslationText(ayah1)
        assertNotNull(textEn)

        // Translation language changed to "id"
        translationLanguageFlow.value = "id"
        testScheduler.runCurrent()
        val textId = vm.getTranslationText(ayah1)
        assertEquals("Dengan nama Allah", textId)
    }

    @Test
    fun `lastRead updates correctly and computes lastReadSurah`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testScheduler.runCurrent()

        assertNull(vm.uiState.value.lastReadSurah)

        lastReadSuraFlow.value = 2
        lastReadAyaFlow.value = 255
        testScheduler.runCurrent()

        assertEquals(2, vm.uiState.value.lastReadSura)
        assertEquals(255, vm.uiState.value.lastReadAya)
        assertEquals(surah2, vm.uiState.value.lastReadSurah)
    }
}
