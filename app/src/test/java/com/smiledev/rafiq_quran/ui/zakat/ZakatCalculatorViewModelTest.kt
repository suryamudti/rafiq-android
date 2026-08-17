package com.smiledev.rafiq_quran.ui.zakat

import androidx.lifecycle.SavedStateHandle
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.MetalPrices
import com.smiledev.rafiq_quran.domain.repository.MetalPriceRepository
import com.smiledev.rafiq_quran.domain.usecase.CalculateZakatUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ZakatCalculatorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val metalPriceRepository: MetalPriceRepository = mockk()
    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher get() = testDispatcher
        override val io: CoroutineDispatcher get() = testDispatcher
        override val default: CoroutineDispatcher get() = testDispatcher
        override val unconfined: CoroutineDispatcher get() = testDispatcher
    }

    private fun createViewModel(): ZakatCalculatorViewModel {
        return ZakatCalculatorViewModel(
            CalculateZakatUseCase(),
            metalPriceRepository,
            testDispatcherProvider,
            SavedStateHandle()
        )
    }

    @Test
    fun `calculate shows instant result from defaults then refreshes with fresh prices`() = runTest(testDispatcher) {
        every { metalPriceRepository.getCachedMetalPrices() } returns null
        coEvery { metalPriceRepository.fetchMetalPrices() } returns Result.Success(MetalPrices(60.0, 0.70))

        val viewModel = createViewModel()
        viewModel.updateGold("100.0")
        viewModel.updateSilver("600.0")
        viewModel.updateCash("10000.0")
        viewModel.calculate()

        // Instant: defaults 65.0 gold / 0.75 silver, fallback flag on
        var state = viewModel.uiState.value
        assertEquals(100.0 * 65.0 * 0.025, state.result.goldZakat, 0.01)
        assertEquals(600.0 * 0.75 * 0.025, state.result.silverZakat, 0.01)
        assertEquals(true, state.isUsingFallback)

        advanceUntilIdle()

        // Refreshed: fetched 60.0 / 0.70, fallback flag cleared
        state = viewModel.uiState.value
        assertEquals(150.0, state.result.goldZakat, 0.01)
        assertEquals(10.5, state.result.silverZakat, 0.01)
        assertEquals(250.0, state.result.cashZakat, 0.01)
        assertEquals(410.5, state.result.totalZakat, 0.01)
        assertEquals(false, state.isUsingFallback)
    }

    @Test
    fun `calculate uses cached prices instantly without fallback flag`() = runTest(testDispatcher) {
        every { metalPriceRepository.getCachedMetalPrices() } returns MetalPrices(60.0, 0.70)
        coEvery { metalPriceRepository.fetchMetalPrices() } returns Result.Success(MetalPrices(60.0, 0.70))

        val viewModel = createViewModel()
        viewModel.updateGold("100.0")
        viewModel.updateSilver("600.0")
        viewModel.updateCash("10000.0")
        viewModel.calculate()

        var state = viewModel.uiState.value
        assertEquals(150.0, state.result.goldZakat, 0.01)
        assertEquals(10.5, state.result.silverZakat, 0.01)
        assertEquals(false, state.isUsingFallback)

        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(410.5, state.result.totalZakat, 0.01)
        assertEquals(false, state.isUsingFallback)
    }

    @Test
    fun `calculate in USD computes no zakat when below nisab`() = runTest(testDispatcher) {
        every { metalPriceRepository.getCachedMetalPrices() } returns MetalPrices(60.0, 0.70)
        coEvery { metalPriceRepository.fetchMetalPrices() } returns Result.Success(MetalPrices(60.0, 0.70))

        val viewModel = createViewModel()
        viewModel.updateGold("50.0")
        viewModel.updateSilver("500.0")
        viewModel.updateCash("100.0")
        viewModel.calculate()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0.0, state.result.goldZakat, 0.01)
        assertEquals(0.0, state.result.silverZakat, 0.01)
        assertEquals(0.0, state.result.cashZakat, 0.01)
        assertEquals(0.0, state.result.totalZakat, 0.01)
    }

    @Test
    fun `calculate in IDR correctly converts currency and applies conversion rate`() = runTest(testDispatcher) {
        every { metalPriceRepository.getCachedMetalPrices() } returns MetalPrices(60.0, 0.70)
        coEvery { metalPriceRepository.fetchMetalPrices() } returns Result.Success(MetalPrices(60.0, 0.70))

        val viewModel = createViewModel()
        viewModel.updateCash("100000000.0")
        viewModel.updateCurrency("IDR")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2500000.0, state.result.cashZakat, 0.01)
        assertEquals(2500000.0, state.result.totalZakat, 0.01)
        assertEquals(960000.0, state.result.goldPricePerGram, 0.01)
    }
}
