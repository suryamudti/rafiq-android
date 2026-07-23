package com.smiledev.rafiq.data.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.getOrNull
import com.smiledev.rafiq.data.remote.MetalPriceApi
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
    fun `getGoldPricePerGram returns converted price`() = runTest {
        coEvery { metalPriceApi.getGoldPricePerGram() } returns 65.0

        val result = repo.getGoldPricePerGram()

        val price = result.getOrNull() ?: 0.0
        assertEquals(65.0, price, 0.001)
    }

    @Test
    fun `getSilverPricePerGram returns converted price`() = runTest {
        coEvery { metalPriceApi.getSilverPricePerGram() } returns 0.75

        val result = repo.getSilverPricePerGram()

        val price = result.getOrNull() ?: 0.0
        assertEquals(0.75, price, 0.001)
    }

    @Test
    fun `network error returns AppError`() = runTest {
        coEvery { metalPriceApi.getGoldPricePerGram() } throws RuntimeException("Timeout")

        val result = repo.getGoldPricePerGram()

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Network)
    }
}
