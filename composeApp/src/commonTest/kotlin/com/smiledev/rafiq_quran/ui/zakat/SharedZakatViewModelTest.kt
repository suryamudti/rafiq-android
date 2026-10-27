package com.smiledev.rafiq_quran.ui.zakat

import com.smiledev.rafiq_quran.domain.usecase.CalculateZakatUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedZakatViewModelTest {

    @Test
    fun initialState_hasEmptyInputsAndUsd() {
        val viewModel = SharedZakatViewModel()
        val state = viewModel.uiState.value

        assertEquals("", state.goldWeight)
        assertEquals("", state.silverWeight)
        assertEquals("", state.cashAmount)
        assertEquals("USD", state.selectedCurrency)
        assertEquals(0.0, state.result.totalZakat)
    }

    @Test
    fun updateInputs_updatesStateValues() {
        val viewModel = SharedZakatViewModel()
        viewModel.updateGold("100")
        viewModel.updateSilver("700")
        viewModel.updateCash("5000")

        val state = viewModel.uiState.value
        assertEquals("100", state.goldWeight)
        assertEquals("700", state.silverWeight)
        assertEquals("5000", state.cashAmount)
    }

    @Test
    fun calculate_withInputsAboveNisab_producesZakat() {
        val viewModel = SharedZakatViewModel()
        viewModel.updateGold("100") // Above 85g nisab
        viewModel.calculate()

        val r = viewModel.uiState.value.result
        assertTrue(r.goldZakat > 0.0)
        assertTrue(r.totalZakat > 0.0)
    }

    @Test
    fun updateCurrency_recalculatesInSelectedCurrency() {
        val viewModel = SharedZakatViewModel()
        viewModel.updateGold("100")
        viewModel.calculate()
        val usdTotal = viewModel.uiState.value.result.totalZakat

        viewModel.updateCurrency("IDR")
        val idrTotal = viewModel.uiState.value.result.totalZakat
        assertEquals("IDR", viewModel.uiState.value.selectedCurrency)
        assertTrue(idrTotal > usdTotal)
    }
}
