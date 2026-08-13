package com.smiledev.rafiq.ui.qibla

import com.smiledev.rafiq.domain.usecase.CalculateQiblaUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class QiblaCalculatorTest {

    private val useCase = CalculateQiblaUseCase()

    @Test
    fun `kaaba to kaaba bearing is zero`() {
        val bearing = useCase.calculateBearing(21.4225, 39.8262, 21.4225, 39.8262)
        assertEquals(0, bearing)
    }

    @Test
    fun `jakarta bearing is approximately 295 degrees`() {
        val bearing = useCase.calculateBearing(-6.2088, 106.8456, 21.4225, 39.8262)
        assertEquals(295, bearing)
    }

    @Test
    fun `kaaba to kaaba distance is zero`() {
        val distance = useCase.calculateDistance(21.4225, 39.8262, 21.4225, 39.8262)
        assertEquals(0, distance)
    }

    @Test
    fun `jakarta to kaaba distance is approximately 8000 km`() {
        val distance = useCase.calculateDistance(-6.2088, 106.8456, 21.4225, 39.8262)
        assertEquals(7920, distance)
    }

    @Test
    fun `north pole bearing`() {
        val bearing = useCase.calculateBearing(90.0, 0.0, 21.4225, 39.8262)
        assertEquals(140, bearing)
    }

    @Test
    fun `antipodal point`() {
        val bearing = useCase.calculateBearing(-21.4225, -140.1738, 21.4225, 39.8262)
        assertEquals(90, bearing)
    }

    @Test
    fun `compass direction for north`() {
        assertEquals("N", compassDirection(0))
        assertEquals("N", compassDirection(360))
        assertEquals("N", compassDirection(-5))
    }

    @Test
    fun `compass direction for jakarta bearing is west-northwest`() {
        assertEquals("WNW", compassDirection(295))
    }

    @Test
    fun `compass direction for south east quadrants`() {
        assertEquals("E", compassDirection(90))
        assertEquals("SE", compassDirection(135))
        assertEquals("S", compassDirection(180))
        assertEquals("SW", compassDirection(225))
        assertEquals("W", compassDirection(270))
    }

    @Test
    fun `compass direction for intercardinal points`() {
        assertEquals("NE", compassDirection(45))
        assertEquals("NNW", compassDirection(337))
        assertEquals("ENE", compassDirection(67))
    }

    @Test
    fun `normalizeAngle180 keeps small angles unchanged`() {
        assertEquals(0.0, normalizeAngle180(0.0), 0.001)
        assertEquals(90.0, normalizeAngle180(90.0), 0.001)
        assertEquals(-90.0, normalizeAngle180(-90.0), 0.001)
    }

    @Test
    fun `normalizeAngle180 wraps angles above 180 to negative`() {
        assertEquals(-180.0, normalizeAngle180(180.0), 0.001)
        assertEquals(-170.0, normalizeAngle180(190.0), 0.001)
        assertEquals(-10.0, normalizeAngle180(350.0), 0.001)
    }

    @Test
    fun `normalizeAngle180 wraps angles below -180 to positive`() {
        assertEquals(-180.0, normalizeAngle180(-180.0), 0.001)
        assertEquals(170.0, normalizeAngle180(-190.0), 0.001)
        assertEquals(10.0, normalizeAngle180(-350.0), 0.001)
    }

    @Test
    fun `normalizeAngle180 handles large multiples of 360`() {
        assertEquals(45.0, normalizeAngle180(405.0), 0.001)
        assertEquals(-45.0, normalizeAngle180(-405.0), 0.001)
    }
}
