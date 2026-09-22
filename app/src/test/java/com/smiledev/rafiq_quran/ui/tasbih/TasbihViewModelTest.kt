package com.smiledev.rafiq_quran.ui.tasbih

import android.content.Context
import android.os.Vibrator
import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.GregorianDate
import com.smiledev.rafiq_quran.domain.model.TasbihItem
import com.smiledev.rafiq_quran.domain.repository.TasbihHistoryRepository
import com.smiledev.rafiq_quran.domain.repository.TasbihRepository
import com.smiledev.rafiq_quran.domain.util.TodayProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasbihViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)

    private val context: Context = mockk<Context>(relaxed = true).apply {
        every { getSystemService(Context.VIBRATOR_SERVICE) } returns mockk<Vibrator>(relaxed = true)
    }

    private val repository: TasbihRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private val tasbihHistoryRepository: TasbihHistoryRepository = mockk(relaxed = true)
    private val todayProvider: TodayProvider = mockk()

    private val sampleItems = listOf(
        TasbihItem(1, "سُبْحَانَ اللّهُ", "Subhanallah", "Glory be to Allah", "Maha Suci Allah", 33),
        TasbihItem(2, "ٱلْحَمْدُ لِلَّهِ", "Alhamdulillah", "All praise is due to Allah", "Segala puji bagi Allah", 33),
        TasbihItem(3, "اللَّهُ أَكْبَرُ", "Allahu Akbar", "Allah is the Greatest", "Allah Maha Besar", 34)
    )

    @Before
    fun setup() {
        every { todayProvider.today() } returns GregorianDate(2026, 9, 22)
        every { tasbihHistoryRepository.observeDayRecords(any()) } returns flowOf(emptyList())
        coEvery { tasbihHistoryRepository.addOrUpdateCount(any(), any(), any(), any(), any()) } returns Result.Success(Unit)
        every { repository.getTasbihItems() } returns Result.Success(sampleItems)
        every { preferencesManager.tasbihSelectedId } returns flowOf(1)
        every { preferencesManager.tasbihCount } returns flowOf(0)
        every { preferencesManager.tasbihLap } returns flowOf(1)
        every { preferencesManager.tasbihTotal } returns flowOf(0)
        every { preferencesManager.tasbihTarget } returns flowOf(33)
        every { preferencesManager.tasbihVibrationEnabled } returns flowOf(true)
        every { preferencesManager.tasbihSoundEnabled } returns flowOf(false)
    }

    private fun createViewModel(): TasbihViewModel {
        return TasbihViewModel(
            context = context,
            repository = repository,
            preferencesManager = preferencesManager,
            tasbihHistoryRepository = tasbihHistoryRepository,
            todayProvider = todayProvider,
            dispatcherProvider = testDispatcherProvider
        )
    }

    @Test
    fun `initial state loads preferences and items`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(3, state.items.size)
        assertEquals("Subhanallah", state.selectedItem?.transliteration)
        assertEquals(0, state.count)
        assertEquals(1, state.lap)
        assertEquals(0, state.totalCount)
        assertEquals(33, state.target)
        assertTrue(state.isVibrationEnabled)
        assertFalse(state.isSoundEnabled)
    }

    @Test
    fun `increment increases count and total`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.increment()
        assertEquals(1, vm.uiState.value.count)
        assertEquals(1, vm.uiState.value.totalCount)
        assertEquals(1, vm.uiState.value.lap)
    }

    @Test
    fun `multiple increments update correctly`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        repeat(5) { vm.increment() }
        assertEquals(5, vm.uiState.value.count)
        assertEquals(5, vm.uiState.value.totalCount)
    }

    @Test
    fun `reaching target sets milestone notice`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        repeat(33) { vm.increment() }
        assertEquals(33, vm.uiState.value.count)
        assertTrue(vm.uiState.value.isTargetReachedNotice)
        assertEquals(1, vm.uiState.value.lap)
        assertEquals(33, vm.uiState.value.totalCount)
    }

    @Test
    fun `incrementing past target starts next lap at 1`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        repeat(33) { vm.increment() }
        assertEquals(33, vm.uiState.value.count)

        // 34th tap rolls over to next lap
        vm.increment()
        assertEquals(1, vm.uiState.value.count)
        assertEquals(2, vm.uiState.value.lap)
        assertEquals(34, vm.uiState.value.totalCount)
    }

    @Test
    fun `decrement decreases count and total`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        repeat(5) { vm.increment() }
        vm.decrement()

        assertEquals(4, vm.uiState.value.count)
        assertEquals(4, vm.uiState.value.totalCount)
    }

    @Test
    fun `decrement at zero does nothing`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.decrement()
        assertEquals(0, vm.uiState.value.count)
        assertEquals(0, vm.uiState.value.totalCount)
    }

    @Test
    fun `reset current lap sets count to zero but keeps lap and total`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        repeat(10) { vm.increment() }
        vm.reset(resetAll = false)

        assertEquals(0, vm.uiState.value.count)
        assertEquals(1, vm.uiState.value.lap)
        assertEquals(10, vm.uiState.value.totalCount)
    }

    @Test
    fun `reset all sets count to zero, lap to 1, and total to zero`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        repeat(10) { vm.increment() }
        vm.reset(resetAll = true)

        assertEquals(0, vm.uiState.value.count)
        assertEquals(1, vm.uiState.value.lap)
        assertEquals(0, vm.uiState.value.totalCount)
    }

    @Test
    fun `selectDhikr updates selected dhikr and target`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val allahuAkbar = sampleItems[2] // default count 34
        vm.selectDhikr(allahuAkbar)

        assertEquals(allahuAkbar, vm.uiState.value.selectedItem)
        assertEquals(34, vm.uiState.value.target)
        assertEquals(0, vm.uiState.value.count)
    }

    @Test
    fun `select custom dhikr sets item to null`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectDhikr(null)
        assertNull(vm.uiState.value.selectedItem)
    }

    @Test
    fun `setTarget updates target`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.setTarget(100)
        assertEquals(100, vm.uiState.value.target)

        vm.setTarget(0)
        assertEquals(0, vm.uiState.value.target)
    }

    @Test
    fun `toggle vibration updates state`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isVibrationEnabled)
        vm.toggleVibration()
        assertFalse(vm.uiState.value.isVibrationEnabled)
        vm.toggleVibration()
        assertTrue(vm.uiState.value.isVibrationEnabled)
    }

    @Test
    fun `toggle sound updates state`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSoundEnabled)
        vm.toggleSound()
        assertTrue(vm.uiState.value.isSoundEnabled)
        vm.toggleSound()
        assertFalse(vm.uiState.value.isSoundEnabled)
    }

    @Test
    fun `dialog visibility toggles`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.showDhikrPicker)
        vm.setShowDhikrPicker(true)
        assertTrue(vm.uiState.value.showDhikrPicker)

        assertFalse(vm.uiState.value.showResetDialog)
        vm.setShowResetDialog(true)
        assertTrue(vm.uiState.value.showResetDialog)

        assertFalse(vm.uiState.value.showCustomTargetDialog)
        vm.setShowCustomTargetDialog(true)
        assertTrue(vm.uiState.value.showCustomTargetDialog)
    }

    @Test
    fun `increment records count in daily history`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.increment()
        advanceUntilIdle()

        coVerify {
            tasbihHistoryRepository.addOrUpdateCount(
                date = "2026-09-22",
                dhikrId = 1,
                dhikrName = "Subhanallah",
                arabic = "سُبْحَانَ اللّهُ",
                delta = 1
            )
        }
    }

    @Test
    fun `decrement records negative delta in daily history`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.increment()
        advanceUntilIdle()
        vm.decrement()
        advanceUntilIdle()

        coVerify {
            tasbihHistoryRepository.addOrUpdateCount(
                date = "2026-09-22",
                dhikrId = 1,
                dhikrName = "Subhanallah",
                arabic = "سُبْحَانَ اللّهُ",
                delta = -1
            )
        }
    }
}
