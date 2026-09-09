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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrayerGuidanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val repository: PrayerGuidanceRepository = mockk()
    private val useCase = GetPrayerGuidanceUseCase(repository)

    private val sampleItems = listOf(
        PrayerGuidanceItem(
            id = "wudhu",
            nameEn = "Wudu",
            nameId = "Tata Cara Wudhu",
            nameArabic = "الوضوء",
            category = PrayerGuidanceCategory.PURIFICATION,
            rakaat = null,
            descriptionEn = "Ablution before prayer",
            descriptionId = "Bersuci sebelum sholat",
            steps = listOf(
                PrayerStep(1, "Intention", "Niat", "Wash hands", "Membasuh tangan")
            )
        ),
        PrayerGuidanceItem(
            id = "subuh",
            nameEn = "Fajr Prayer",
            nameId = "Sholat Subuh",
            nameArabic = "صلاة الصبح",
            category = PrayerGuidanceCategory.OBLIGATORY,
            rakaat = 2,
            descriptionEn = "Morning obligatory prayer",
            descriptionId = "Sholat wajib pagi",
            steps = listOf(
                PrayerStep(1, "Takbir", "Takbir", "Takbiratul Ihram", "Takbiratul Ihram")
            )
        ),
        PrayerGuidanceItem(
            id = "dhuha",
            nameEn = "Dhuha Prayer",
            nameId = "Sholat Dhuha",
            nameArabic = "صلاة الضحى",
            category = PrayerGuidanceCategory.SUNNAH,
            rakaat = 2,
            descriptionEn = "Forenoon sunnah prayer",
            descriptionId = "Sholat sunnah pagi",
            steps = listOf(
                PrayerStep(1, "Takbir", "Takbir", "Takbiratul Ihram", "Takbiratul Ihram")
            )
        )
    )

    @Test
    fun `load guidance success on init`() = runTest(testDispatcher) {
        every { repository.getGuidanceList() } returns Result.Success(sampleItems)

        val vm = PrayerGuidanceViewModel(useCase, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.items.size)
        assertEquals(false, vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `load guidance error sets error state`() = runTest(testDispatcher) {
        every { repository.getGuidanceList() } returns Result.Error(AppError.Database("Failed to load", null))

        val vm = PrayerGuidanceViewModel(useCase, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.items.size)
        assertEquals(false, vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `filterItems filters by category`() = runTest(testDispatcher) {
        every { repository.getGuidanceList() } returns Result.Success(sampleItems)

        val vm = PrayerGuidanceViewModel(useCase, testDispatcherProvider)
        advanceUntilIdle()

        vm.selectCategory(PrayerGuidanceCategory.OBLIGATORY)
        val filtered = vm.filterItems(vm.uiState.value)

        assertEquals(1, filtered.size)
        assertEquals("subuh", filtered[0].id)
    }

    @Test
    fun `filterItems filters by search query`() = runTest(testDispatcher) {
        every { repository.getGuidanceList() } returns Result.Success(sampleItems)

        val vm = PrayerGuidanceViewModel(useCase, testDispatcherProvider)
        advanceUntilIdle()

        vm.search("dhuha")
        val filtered = vm.filterItems(vm.uiState.value)

        assertEquals(1, filtered.size)
        assertEquals("dhuha", filtered[0].id)
    }

    @Test
    fun `detail viewModel loads item by id`() = runTest(testDispatcher) {
        val targetItem = sampleItems[1] // subuh
        every { repository.getGuidanceById("subuh") } returns Result.Success(targetItem)

        val detailVm = PrayerGuidanceDetailViewModel(useCase, testDispatcherProvider)
        detailVm.loadDetail("subuh")
        advanceUntilIdle()

        assertEquals(targetItem, detailVm.uiState.value.item)
        assertEquals(false, detailVm.uiState.value.isLoading)
        assertNull(detailVm.uiState.value.error)
    }

    @Test
    fun `detail viewModel handles error when not found`() = runTest(testDispatcher) {
        every { repository.getGuidanceById("unknown") } returns Result.Error(AppError.NotFound)

        val detailVm = PrayerGuidanceDetailViewModel(useCase, testDispatcherProvider)
        detailVm.loadDetail("unknown")
        advanceUntilIdle()

        assertNull(detailVm.uiState.value.item)
        assertNotNull(detailVm.uiState.value.error)
    }
}
