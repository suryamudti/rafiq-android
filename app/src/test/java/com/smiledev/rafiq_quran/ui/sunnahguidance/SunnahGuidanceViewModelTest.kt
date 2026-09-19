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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SunnahGuidanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val repository: SunnahGuidanceRepository = mockk()
    private val useCase = GetSunnahGuidanceUseCase(repository)

    private val sampleItems = listOf(
        SunnahGuidanceItem(
            id = "tahajjud",
            titleEn = "Tahajjud Prayer",
            titleId = "Shalat Tahajjud",
            titleArabic = "صلاة التهجد",
            category = SunnahCategory.PRAYER,
            summaryEn = "Night vigil prayer",
            summaryId = "Shalat sunnah malam",
            descriptionEn = "Night vigil prayer after waking from sleep",
            descriptionId = "Shalat sunnah malam setelah bangun tidur",
            hadithReference = "Sahih Muslim 1163",
            surahReference = "Surah Al-Isra 17:79",
            dalilArabic = "وَمِنَ اللَّيْلِ فَتَهَجَّدْ بِهِ نَافِلَةً لَّكَ",
            dalilTranslationEn = "And from part of the night, pray with it as additional worship for you",
            dalilTranslationId = "Dan pada sebagian malam, lakukanlah shalat tahajjud",
            virtuesEn = listOf("Best prayer after obligatory prayers"),
            virtuesId = listOf("Shalat paling utama setelah shalat fardhu"),
            steps = listOf(
                SunnahStep(
                    order = 1,
                    titleEn = "Intention",
                    titleId = "Niat",
                    descriptionEn = "Sincere in heart",
                    descriptionId = "Ikhlas dalam hati"
                )
            )
        ),
        SunnahGuidanceItem(
            id = "friday_sunnahs",
            titleEn = "Friday Sunnah Acts",
            titleId = "Sunnah Hari Jumat",
            titleArabic = "سنن يوم الجمعة",
            category = SunnahCategory.FRIDAY,
            summaryEn = "Sunnah acts on Friday",
            summaryId = "Amalan sunnah hari Jumat",
            descriptionEn = "Sunnah acts recommended on Friday",
            descriptionId = "Amalan sunnah yang dianjurkan pada hari Jumat",
            hadithReference = "Sahih Bukhari 883",
            surahReference = null,
            dalilArabic = "مَنِ اغْتَسَلَ يَوْمَ الْجُمُعَةِ",
            dalilTranslationEn = "Whoever takes a bath on Friday",
            dalilTranslationId = "Barangsiapa mandi pada hari Jumat",
            virtuesEn = listOf("Ghusl and listening to khutbah expiates sins"),
            virtuesId = listOf("Mandi dan mendengar khutbah menghapus dosa"),
            steps = listOf(
                SunnahStep(
                    order = 1,
                    titleEn = "Ghusl",
                    titleId = "Mandi",
                    descriptionEn = "Take a bath",
                    descriptionId = "Mandi sunnah"
                )
            )
        ),
        SunnahGuidanceItem(
            id = "fasting_monday_thursday",
            titleEn = "Monday & Thursday Fasting",
            titleId = "Puasa Senin & Kamis",
            titleArabic = "صيام الإثنين والخميس",
            category = SunnahCategory.FASTING,
            summaryEn = "Fasting on Mondays and Thursdays",
            summaryId = "Puasa pada hari Senin dan Kamis",
            descriptionEn = "Voluntary fasting on Mondays and Thursdays",
            descriptionId = "Puasa sunnah pada hari Senin dan Kamis",
            hadithReference = "Jami at-Tirmidhi 747",
            surahReference = null,
            dalilArabic = "تُعْرَضُ الأَعْمَالُ يَوْمَ الاِثْنَيْنِ وَالْخَمِيسِ",
            dalilTranslationEn = "Deeds are shown on Monday and Thursday",
            dalilTranslationId = "Amalan-amalan dipaparkan pada hari Senin dan Kamis",
            virtuesEn = listOf("Deeds are presented to Allah"),
            virtuesId = listOf("Amal diperlihatkan kepada Allah"),
            steps = listOf(
                SunnahStep(
                    order = 1,
                    titleEn = "Intention",
                    titleId = "Niat",
                    descriptionEn = "Niyyah before Fajr",
                    descriptionId = "Niat sebelum fajar"
                )
            )
        )
    )

    @Test
    fun `initial state has isLoading true`() {
        val vm = SunnahGuidanceViewModel(useCase, testDispatcherProvider)
        assertEquals(true, vm.uiState.value.isLoading)
        assertEquals(0, vm.uiState.value.items.size)
    }

    @Test
    fun `load guidance success on init`() = runTest(testDispatcher) {
        every { repository.getSunnahList() } returns Result.Success(sampleItems)

        val vm = SunnahGuidanceViewModel(useCase, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.items.size)
        assertEquals(false, vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `load guidance error sets error state`() = runTest(testDispatcher) {
        every { repository.getSunnahList() } returns Result.Error(AppError.Database("Failed to load", null))

        val vm = SunnahGuidanceViewModel(useCase, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.items.size)
        assertEquals(false, vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `filterItems filters by category`() = runTest(testDispatcher) {
        every { repository.getSunnahList() } returns Result.Success(sampleItems)

        val vm = SunnahGuidanceViewModel(useCase, testDispatcherProvider)
        advanceUntilIdle()

        vm.selectCategory(SunnahCategory.FRIDAY)
        val filtered = vm.filterItems(vm.uiState.value)

        assertEquals(1, filtered.size)
        assertEquals("friday_sunnahs", filtered[0].id)
    }

    @Test
    fun `filterItems filters by search query`() = runTest(testDispatcher) {
        every { repository.getSunnahList() } returns Result.Success(sampleItems)

        val vm = SunnahGuidanceViewModel(useCase, testDispatcherProvider)
        advanceUntilIdle()

        vm.search("fasting")
        val filtered = vm.filterItems(vm.uiState.value)

        assertEquals(1, filtered.size)
        assertEquals("fasting_monday_thursday", filtered[0].id)
    }

    @Test
    fun `detail viewModel loads item by id`() = runTest(testDispatcher) {
        val targetItem = sampleItems[0] // tahajjud
        every { repository.getSunnahById("tahajjud") } returns Result.Success(targetItem)

        val detailVm = SunnahGuidanceDetailViewModel(useCase, testDispatcherProvider)
        detailVm.loadDetail("tahajjud")
        advanceUntilIdle()

        assertEquals(targetItem, detailVm.uiState.value.item)
        assertEquals(false, detailVm.uiState.value.isLoading)
        assertNull(detailVm.uiState.value.error)
    }

    @Test
    fun `detail viewModel handles error when not found`() = runTest(testDispatcher) {
        every { repository.getSunnahById("unknown") } returns Result.Error(AppError.NotFound)

        val detailVm = SunnahGuidanceDetailViewModel(useCase, testDispatcherProvider)
        detailVm.loadDetail("unknown")
        advanceUntilIdle()

        assertNull(detailVm.uiState.value.item)
        assertNotNull(detailVm.uiState.value.error)
    }
}
