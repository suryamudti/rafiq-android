package com.smiledev.rafiq_quran.ui.prayerguidance

import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceCategory
import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceItem
import com.smiledev.rafiq_quran.domain.model.PrayerStep
import com.smiledev.rafiq_quran.domain.repository.PrayerGuidanceRepository
import com.smiledev.rafiq_quran.domain.usecase.GetPrayerGuidanceUseCase
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
class PrayerGuidanceDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val repository: PrayerGuidanceRepository = mockk()
    private val useCase = GetPrayerGuidanceUseCase(repository)
    private lateinit var originalLocale: Locale

    private val sampleItem = PrayerGuidanceItem(
        id = "fajr",
        nameEn = "Fajr Prayer",
        nameId = "Sholat Subuh",
        nameArabic = "صلاة الفجر",
        category = PrayerGuidanceCategory.OBLIGATORY,
        rakaat = 2,
        descriptionEn = "Two rak'ahs before sunrise",
        descriptionId = "Dua rakaat sebelum matahari terbit",
        steps = listOf(
            PrayerStep(1, "Intention", "Niat", "Make intention", "Membaca niat")
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
        val vm = PrayerGuidanceDetailViewModel(useCase, testDispatcherProvider)

        assertTrue(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.item)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `loadDetail success populates item and stops loading`() = runTest(testDispatcher) {
        every { repository.getGuidanceById("fajr") } returns Result.Success(sampleItem)

        val vm = PrayerGuidanceDetailViewModel(useCase, testDispatcherProvider)
        vm.loadDetail("fajr")
        assertTrue(vm.uiState.value.isLoading)

        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(sampleItem, vm.uiState.value.item)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `loadDetail error sets error and stops loading`() = runTest(testDispatcher) {
        every { repository.getGuidanceById("missing") } returns Result.Error(AppError.NotFound)

        val vm = PrayerGuidanceDetailViewModel(useCase, testDispatcherProvider)
        vm.loadDetail("missing")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.item)
        assertEquals(AppError.NotFound, vm.uiState.value.error)
    }

    @Test
    fun `loadDetail clears previous error when retry succeeds`() = runTest(testDispatcher) {
        every { repository.getGuidanceById("fajr") } returnsMany listOf(
            Result.Error(AppError.Database("Read failed")),
            Result.Success(sampleItem)
        )

        val vm = PrayerGuidanceDetailViewModel(useCase, testDispatcherProvider)
        vm.loadDetail("fajr")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.error is AppError.Database)

        vm.loadDetail("fajr")
        assertNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.isLoading)

        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
        assertEquals(sampleItem, vm.uiState.value.item)
    }

    @Test
    fun `localeCode reflects current default locale`() {
        val vm = PrayerGuidanceDetailViewModel(useCase, testDispatcherProvider)

        Locale.setDefault(Locale.ENGLISH)
        assertEquals("en", vm.localeCode)

        Locale.setDefault(Locale.forLanguageTag("id-ID"))
        assertEquals("id", vm.localeCode)
    }
}
