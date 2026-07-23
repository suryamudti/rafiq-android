package com.smiledev.rafiq.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateQiblaUseCaseTest {

    private val useCase = CalculateQiblaUseCase()

    @Test
    fun `qibla from Jakarta matches expected bearing and distance`() {
        val result = useCase(-6.2088, 106.8456)

        assertEquals(295, result.bearing)
        assertEquals(7920, result.distanceKm)
    }

    @Test
    fun `qibla from Kaaba itself is 0 distance`() {
        val result = useCase(21.4225, 39.8262)

        assertEquals(0, result.distanceKm)
    }

    @Test
    fun `qibla from North Pole has correct bearing`() {
        val result = useCase(90.0, 0.0)

        assertEquals(140, result.bearing)
    }

    @Test
    fun `qibla from South Africa has correct bearing`() {
        val result = useCase(-33.9249, 18.4241)

        assertEquals(23, result.bearing)
    }

    @Test
    fun `qibla from London has correct bearing`() {
        val result = useCase(51.5074, -0.1278)

        assertEquals(119, result.bearing)
    }
}
