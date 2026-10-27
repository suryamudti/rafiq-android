package com.smiledev.rafiq_quran.ui.tasbih

import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.TasbihItem
import com.smiledev.rafiq_quran.domain.repository.TasbihRepository
import com.smiledev.rafiq_quran.domain.usecase.GetTasbihItemsUseCase
import com.smiledev.rafiq_quran.platform.TasbihFeedbackController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeTasbihFeedbackController : TasbihFeedbackController {
    var tickCount = 0
    var targetReachedCount = 0
    var soundClickCount = 0

    override fun vibrateTick() {
        tickCount++
    }

    override fun vibrateTargetReached() {
        targetReachedCount++
    }

    override fun playClickSound() {
        soundClickCount++
    }
}

class SharedTasbihViewModelTest {

    @Test
    fun initialState_hasDefaultDhikrAndZeroCount() {
        val viewModel = SharedTasbihViewModel()
        val state = viewModel.uiState.value

        assertEquals(0, state.count)
        assertEquals(1, state.lap)
        assertEquals(33, state.target)
        assertEquals(0, state.totalCount)
        assertTrue(state.isVibrationEnabled)
        assertFalse(state.isSoundEnabled)
        assertTrue(state.items.isNotEmpty())
    }

    @Test
    fun onIncrement_incrementsCountAndTriggersTickFeedback() {
        val viewModel = SharedTasbihViewModel()
        val fakeFeedback = FakeTasbihFeedbackController()

        viewModel.onIncrement(fakeFeedback)

        val state = viewModel.uiState.value
        assertEquals(1, state.count)
        assertEquals(1, state.totalCount)
        assertEquals(1, state.lap)
        assertEquals(1, fakeFeedback.tickCount)
        assertEquals(0, fakeFeedback.targetReachedCount)
    }

    @Test
    fun onIncrement_reachesTarget_advancesLapAndTriggersTargetFeedback() {
        val viewModel = SharedTasbihViewModel()
        val fakeFeedback = FakeTasbihFeedbackController()
        viewModel.setTarget(3)

        viewModel.onIncrement(fakeFeedback) // 1
        viewModel.onIncrement(fakeFeedback) // 2
        viewModel.onIncrement(fakeFeedback) // 3 -> target reached!

        val state = viewModel.uiState.value
        assertEquals(0, state.count)
        assertEquals(2, state.lap)
        assertEquals(3, state.totalCount)
        assertTrue(state.isTargetReachedNotice)
        assertEquals(2, fakeFeedback.tickCount)
        assertEquals(1, fakeFeedback.targetReachedCount)
    }

    @Test
    fun onDecrement_decrementsCountWhenGreaterThanZero() {
        val viewModel = SharedTasbihViewModel()
        viewModel.onIncrement()
        viewModel.onIncrement()
        assertEquals(2, viewModel.uiState.value.count)

        viewModel.onDecrement()
        assertEquals(1, viewModel.uiState.value.count)

        viewModel.onDecrement()
        assertEquals(0, viewModel.uiState.value.count)

        viewModel.onDecrement()
        assertEquals(0, viewModel.uiState.value.count)
    }

    @Test
    fun onReset_resetsCountAndLap() {
        val viewModel = SharedTasbihViewModel()
        viewModel.onIncrement()
        viewModel.onIncrement()
        assertEquals(2, viewModel.uiState.value.count)

        viewModel.onReset()
        val state = viewModel.uiState.value
        assertEquals(0, state.count)
        assertEquals(1, state.lap)
        assertFalse(state.isTargetReachedNotice)
    }

    @Test
    fun onSelectItem_switchesItemAndTarget() {
        val customItems = listOf(
            TasbihItem(1, "سُبْحَانَ اللهِ", "SubhanAllah", "Glory", "Maha Suci", 33),
            TasbihItem(2, "لَا إِلَهَ إِلَّا اللهُ", "La ilaha illallah", "None", "Tiada tuhan", 100)
        )
        val repo = object : TasbihRepository {
            override fun getTasbihItems() = Result.Success(customItems)
        }
        val useCase = GetTasbihItemsUseCase(repo)
        val viewModel = SharedTasbihViewModel(useCase)

        viewModel.onSelectItem(customItems[1])

        val state = viewModel.uiState.value
        assertEquals(customItems[1], state.selectedItem)
        assertEquals(100, state.target)
        assertEquals(0, state.count)
    }

    @Test
    fun toggleSettings_invertsValues() {
        val viewModel = SharedTasbihViewModel()
        assertTrue(viewModel.uiState.value.isVibrationEnabled)
        assertFalse(viewModel.uiState.value.isSoundEnabled)

        viewModel.toggleVibration()
        assertFalse(viewModel.uiState.value.isVibrationEnabled)

        viewModel.toggleSound()
        assertTrue(viewModel.uiState.value.isSoundEnabled)
    }
}
