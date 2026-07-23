package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.repository.MetalPriceRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateZakatUseCaseTest {

    private val repository: MetalPriceRepository = mockk()
    private val useCase = CalculateZakatUseCase(repository)

    @Test
    fun `returns error when gold price fetch fails`() = runTest {
        coEvery { repository.getGoldPricePerGram() } returns Result.Error(AppError.Network("timeout"))

        val result = useCase(0.0, 0.0, 0.0)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `returns error when silver price fetch fails`() = runTest {
        coEvery { repository.getGoldPricePerGram() } returns Result.Success(70.0)
        coEvery { repository.getSilverPricePerGram() } returns Result.Error(AppError.Network("timeout"))

        val result = useCase(0.0, 0.0, 0.0)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `all zero when below all nisab thresholds`() = runTest {
        coEvery { repository.getGoldPricePerGram() } returns Result.Success(70.0)
        coEvery { repository.getSilverPricePerGram() } returns Result.Success(0.9)

        val result = useCase(10.0, 50.0, 100.0)

        assertTrue(result is Result.Success)
        val zakat = (result as Result.Success).data
        assertEquals(0.0, zakat.goldZakat, 0.001)
        assertEquals(0.0, zakat.silverZakat, 0.001)
        assertEquals(0.0, zakat.cashZakat, 0.001)
        assertEquals(0.0, zakat.totalZakat, 0.001)
    }

    @Test
    fun `calculates gold zakat when above gold nisab`() = runTest {
        coEvery { repository.getGoldPricePerGram() } returns Result.Success(70.0)
        coEvery { repository.getSilverPricePerGram() } returns Result.Success(0.9)

        val result = useCase(100.0, 0.0, 0.0)

        assertTrue(result is Result.Success)
        val zakat = (result as Result.Success).data
        val expectedGoldZakat = 100.0 * 70.0 * 0.025
        assertEquals(expectedGoldZakat, zakat.goldZakat, 0.001)
        assertEquals(0.0, zakat.silverZakat, 0.001)
        assertEquals(0.0, zakat.cashZakat, 0.001)
        assertEquals(expectedGoldZakat, zakat.totalZakat, 0.001)
    }

    @Test
    fun `calculates silver zakat when above silver nisab`() = runTest {
        coEvery { repository.getGoldPricePerGram() } returns Result.Success(70.0)
        coEvery { repository.getSilverPricePerGram() } returns Result.Success(0.9)

        val result = useCase(0.0, 700.0, 0.0)

        assertTrue(result is Result.Success)
        val zakat = (result as Result.Success).data
        val expectedSilverZakat = 700.0 * 0.9 * 0.025
        assertEquals(expectedSilverZakat, zakat.silverZakat, 0.001)
        assertEquals(0.0, zakat.goldZakat, 0.001)
        assertEquals(expectedSilverZakat, zakat.totalZakat, 0.001)
    }

    @Test
    fun `calculates cash zakat when above cash nisab`() = runTest {
        coEvery { repository.getGoldPricePerGram() } returns Result.Success(70.0)
        coEvery { repository.getSilverPricePerGram() } returns Result.Success(0.9)

        val result = useCase(0.0, 0.0, 10000.0)

        assertTrue(result is Result.Success)
        val zakat = (result as Result.Success).data
        val expectedCashZakat = 10000.0 * 0.025
        assertEquals(expectedCashZakat, zakat.cashZakat, 0.001)
        assertEquals(expectedCashZakat, zakat.totalZakat, 0.001)
    }

    @Test
    fun `calculates total zakat when all above nisab`() = runTest {
        coEvery { repository.getGoldPricePerGram() } returns Result.Success(70.0)
        coEvery { repository.getSilverPricePerGram() } returns Result.Success(0.9)

        val result = useCase(100.0, 700.0, 10000.0)

        assertTrue(result is Result.Success)
        val zakat = (result as Result.Success).data
        val expectedGold = 100.0 * 70.0 * 0.025
        val expectedSilver = 700.0 * 0.9 * 0.025
        val expectedCash = 10000.0 * 0.025
        assertEquals(expectedGold, zakat.goldZakat, 0.001)
        assertEquals(expectedSilver, zakat.silverZakat, 0.001)
        assertEquals(expectedCash, zakat.cashZakat, 0.001)
        assertEquals(expectedGold + expectedSilver + expectedCash, zakat.totalZakat, 0.001)
    }

    @Test
    fun `converts IDR to USD internally for nisab check`() = runTest {
        coEvery { repository.getGoldPricePerGram() } returns Result.Success(70.0)
        coEvery { repository.getSilverPricePerGram() } returns Result.Success(0.9)

        val result = useCase(0.0, 0.0, 500000.0, currency = "IDR")

        assertTrue(result is Result.Success)
        val zakat = (result as Result.Success).data
        val cashInUsd = 500000.0 / 16000.0
        val cashRateUsd = 70.0 * 85.0
        val rate = 16000.0
        val expectedCashZakat = if (cashInUsd >= cashRateUsd) cashInUsd * 0.025 * rate else 0.0
        assertEquals(expectedCashZakat, zakat.cashZakat, 0.001)
        assertEquals(70.0 * rate, zakat.goldPricePerGram, 0.001)
        assertEquals(0.9 * rate, zakat.silverPricePerGram, 0.001)
    }
}
