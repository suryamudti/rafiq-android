package com.smiledev.rafiq_quran.domain.usecase

import com.smiledev.rafiq_quran.domain.model.MetalPrices
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateZakatUseCaseTest {

    private val useCase = CalculateZakatUseCase()
    private val prices = MetalPrices(goldPricePerGram = 70.0, silverPricePerGram = 0.9)

    @Test
    fun `all zero when below all nisab thresholds`() {
        val result = useCase(10.0, 50.0, 100.0, "USD", prices)
        assertEquals(0.0, result.goldZakat, 0.001)
        assertEquals(0.0, result.silverZakat, 0.001)
        assertEquals(0.0, result.cashZakat, 0.001)
        assertEquals(0.0, result.totalZakat, 0.001)
    }

    @Test
    fun `calculates gold zakat when above gold nisab`() {
        val result = useCase(100.0, 0.0, 0.0, "USD", prices)
        val expectedGoldZakat = 100.0 * 70.0 * 0.025
        assertEquals(expectedGoldZakat, result.goldZakat, 0.001)
        assertEquals(expectedGoldZakat, result.totalZakat, 0.001)
    }

    @Test
    fun `calculates silver zakat when above silver nisab`() {
        val result = useCase(0.0, 700.0, 0.0, "USD", prices)
        val expectedSilverZakat = 700.0 * 0.9 * 0.025
        assertEquals(expectedSilverZakat, result.silverZakat, 0.001)
        assertEquals(expectedSilverZakat, result.totalZakat, 0.001)
    }

    @Test
    fun `calculates cash zakat when above cash nisab`() {
        val result = useCase(0.0, 0.0, 10000.0, "USD", prices)
        assertEquals(250.0, result.cashZakat, 0.001)
        assertEquals(250.0, result.totalZakat, 0.001)
    }

    @Test
    fun `calculates total zakat when all above nisab`() {
        val result = useCase(100.0, 700.0, 10000.0, "USD", prices)
        val expectedGold = 100.0 * 70.0 * 0.025
        val expectedSilver = 700.0 * 0.9 * 0.025
        val expectedCash = 10000.0 * 0.025
        assertEquals(expectedGold, result.goldZakat, 0.001)
        assertEquals(expectedSilver, result.silverZakat, 0.001)
        assertEquals(expectedCash, result.cashZakat, 0.001)
        assertEquals(expectedGold + expectedSilver + expectedCash, result.totalZakat, 0.001)
    }

    @Test
    fun `converts IDR to USD internally for nisab check`() {
        val result = useCase(0.0, 0.0, 500000.0, "IDR", prices)
        val cashInUsd = 500000.0 / 16000.0
        val cashRateUsd = 70.0 * 85.0
        val rate = 16000.0
        val expectedCashZakat = if (cashInUsd >= cashRateUsd) cashInUsd * 0.025 * rate else 0.0
        assertEquals(expectedCashZakat, result.cashZakat, 0.001)
        assertEquals(70.0 * rate, result.goldPricePerGram, 0.001)
        assertEquals(0.9 * rate, result.silverPricePerGram, 0.001)
    }
}
