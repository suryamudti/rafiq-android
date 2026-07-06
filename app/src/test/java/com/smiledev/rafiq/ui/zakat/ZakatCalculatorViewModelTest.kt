package com.smiledev.rafiq.ui.zakat

import androidx.lifecycle.SavedStateHandle
import com.smiledev.rafiq.data.repository.MetalPriceRepository
import io.mockk.coEvery
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ZakatCalculatorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val metalPriceRepository: MetalPriceRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `calculate in USD computes correct zakat when above nisab`() = runTest(testDispatcher) {
        coEvery { metalPriceRepository.getGoldPricePerGram() } returns 60.0
        coEvery { metalPriceRepository.getSilverPricePerGram() } returns 0.70

        val savedStateHandle = SavedStateHandle().apply {
            set("goldWeight", "100.0") // above 85g nisab
            set("silverWeight", "600.0") // above 595g nisab
            set("cashAmount", "10000.0") // above USD cash nisab (85 * 60 = 5100)
            set("selectedCurrency", "USD")
        }

        val viewModel = ZakatCalculatorViewModel(metalPriceRepository, savedStateHandle)
        viewModel.calculate()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Gold: 100 * 60 * 0.025 = 150.0
        assertEquals(150.0, state.result.goldZakat, 0.01)
        // Silver: 600 * 0.70 * 0.025 = 10.5
        assertEquals(10.5, state.result.silverZakat, 0.01)
        // Cash: 10000 * 0.025 = 250.0
        assertEquals(250.0, state.result.cashZakat, 0.01)
        // Total: 150 + 10.5 + 250 = 410.5
        assertEquals(410.5, state.result.totalZakat, 0.01)
    }

    @Test
    fun `calculate in USD computes no zakat when below nisab`() = runTest(testDispatcher) {
        coEvery { metalPriceRepository.getGoldPricePerGram() } returns 60.0
        coEvery { metalPriceRepository.getSilverPricePerGram() } returns 0.70

        val savedStateHandle = SavedStateHandle().apply {
            set("goldWeight", "50.0") // below 85g
            set("silverWeight", "500.0") // below 595g
            set("cashAmount", "100.0") // below cash nisab (5100)
            set("selectedCurrency", "USD")
        }

        val viewModel = ZakatCalculatorViewModel(metalPriceRepository, savedStateHandle)
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
        coEvery { metalPriceRepository.getGoldPricePerGram() } returns 60.0 // USD
        coEvery { metalPriceRepository.getSilverPricePerGram() } returns 0.70 // USD

        val savedStateHandle = SavedStateHandle().apply {
            set("goldWeight", "0.0")
            set("silverWeight", "0.0")
            // Cash IDR: 100,000,000 IDR / 16,000 = 6,250 USD
            // cash nisab: 85 * 60 = 5,100 USD. So 6,250 USD is above nisab.
            set("cashAmount", "100000000.0")
            set("selectedCurrency", "IDR")
        }

        val viewModel = ZakatCalculatorViewModel(metalPriceRepository, savedStateHandle)
        viewModel.calculate()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Cash Zakat: 100,000,000 IDR * 0.025 = 2,500,000 IDR
        assertEquals(2500000.0, state.result.cashZakat, 0.01)
        assertEquals(2500000.0, state.result.totalZakat, 0.01)
        // Gold price: 60 USD * 16,000 = 960,000 IDR
        assertEquals(960000.0, state.result.goldPricePerGram, 0.01)
    }
}
