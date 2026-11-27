package com.smiledev.rafiq_quran.ui.sunnahguidance

import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.SunnahCategory
import com.smiledev.rafiq_quran.domain.model.SunnahGuidanceItem
import com.smiledev.rafiq_quran.domain.model.SunnahStep
import com.smiledev.rafiq_quran.domain.repository.SunnahGuidanceRepository
import com.smiledev.rafiq_quran.domain.usecase.GetSunnahGuidanceUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class SunnahGuidanceDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val repository: SunnahGuidanceRepository = mockk()
    private val useCase = GetSunnahGuidanceUseCase(repository)
    private lateinit var originalLocale: Locale

    private val sampleItem = SunnahGuidanceItem(
        id = "friday_sunnah",
        titleEn = "Friday Sunnahs",
        titleId = "Sunnah Hari Jumat",
        titleArabic = "سنن يوم الجمعة",
        category = SunnahCategory.FRIDAY,
        summaryEn = "Recommended practices on Friday",
        summaryId = "Amalan sunnah di hari Jumat",
        descriptionEn = "Perform ghusl, wear clean clothes, and read Surah Al-Kahf.",
        descriptionId = "Mandi Jumat, memakai pakaian bersih, dan membaca Surah Al-Kahfi.",
        steps = listOf(
            SunnahStep(1, "Ghusl", "Mandi Jumat", "Take a ritual bath", "Mandi sebelum sholat Jumat")
        )
    )

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading with null item and null error`() {
        val vm = SunnahGuidanceDetailViewModel(useCase, testDispatcherProvider)

        assertTrue(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.item)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `loadDetail success populates item and stops loading`() = runTest(testDispatcher) {
        every { repository.getSunnahById("friday_sunnah") } returns Result.Success(sampleItem)

        val vm = SunnahGuidanceDetailViewModel(useCase, testDispatcherProvider)
        vm.loadDetail("friday_sunnah")
        assertTrue(vm.uiState.value.isLoading)

        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(sampleItem, vm.uiState.value.item)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `loadDetail error sets error and stops loading`() = runTest(testDispatcher) {
        every { repository.getSunnahById("unknown") } returns Result.Error(AppError.NotFound)

        val vm = SunnahGuidanceDetailViewModel(useCase, testDispatcherProvider)
        vm.loadDetail("unknown")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.item)
        assertEquals(AppError.NotFound, vm.uiState.value.error)
    }

    @Test
    fun `loadDetail clears previous error when retry succeeds`() = runTest(testDispatcher) {
        every { repository.getSunnahById("friday_sunnah") } returnsMany listOf(
            Result.Error(AppError.Database("Asset unavailable")),
            Result.Success(sampleItem)
        )

        val vm = SunnahGuidanceDetailViewModel(useCase, testDispatcherProvider)
        vm.loadDetail("friday_sunnah")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.error is AppError.Database)

        vm.loadDetail("friday_sunnah")
        assertNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.isLoading)

        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
        assertEquals(sampleItem, vm.uiState.value.item)
    }

    @Test
    fun `localeCode reflects current default locale`() {
        val vm = SunnahGuidanceDetailViewModel(useCase, testDispatcherProvider)

        Locale.setDefault(Locale.ENGLISH)
        assertEquals("en", vm.localeCode)

        Locale.setDefault(Locale.forLanguageTag("id-ID"))
        assertEquals("id", vm.localeCode)
    }
}
