package com.smiledev.rafiq.data.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.remote.MetalPriceApi
import com.smiledev.rafiq.domain.model.MetalPrices
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MetalPriceRepositoryImplTest {

    private val metalPriceApi: MetalPriceApi = mockk()
    private lateinit var repo: MetalPriceRepositoryImpl

    @Before
    fun setUp() {
        repo = MetalPriceRepositoryImpl(metalPriceApi)
    }

    @Test
    fun `fetchMetalPrices fetches gold and silver and caches result`() = runTest {
        coEvery { metalPriceApi.getGoldPricePerGram() } returns 65.0
        coEvery { metalPriceApi.getSilverPricePerGram() } returns 0.75

        val result = repo.fetchMetalPrices()

        assertTrue(result is Result.Success)
        val prices = (result as Result.Success).data
        assertEquals(65.0, prices.goldPricePerGram, 0.001)
        assertEquals(0.75, prices.silverPricePerGram, 0.001)
        assertEquals(MetalPrices(65.0, 0.75), repo.getCachedMetalPrices())
    }

    @Test
    fun `getCachedMetalPrices is null before any fetch`() {
        assertEquals(null, repo.getCachedMetalPrices())
    }

    @Test
    fun `network error returns AppError and does not cache`() = runTest {
        coEvery { metalPriceApi.getGoldPricePerGram() } throws RuntimeException("Timeout")

        val result = repo.fetchMetalPrices()

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Network)
        assertEquals(null, repo.getCachedMetalPrices())
    }
}
