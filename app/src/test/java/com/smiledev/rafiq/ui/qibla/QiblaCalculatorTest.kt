package com.smiledev.rafiq.ui.qibla

import org.junit.Assert.assertEquals
import org.junit.Test

class QiblaCalculatorTest {

    @Test
    fun `kaaba to kaaba bearing is zero`() {
        val bearing = calculateBearing(21.4225, 39.8262, 21.4225, 39.8262)
        assertEquals(0, bearing)
    }

    @Test
    fun `jakarta bearing is approximately 295 degrees`() {
        val bearing = calculateBearing(-6.2088, 106.8456, 21.4225, 39.8262)
        assertEquals(295, bearing)
    }

    @Test
    fun `kaaba to kaaba distance is zero`() {
        val distance = calculateDistance(21.4225, 39.8262, 21.4225, 39.8262)
        assertEquals(0, distance)
    }

    @Test
    fun `jakarta to kaaba distance is approximately 8000 km`() {
        val distance = calculateDistance(-6.2088, 106.8456, 21.4225, 39.8262)
        assertEquals(7920, distance)
    }

    @Test
    fun `north pole bearing`() {
        val bearing = calculateBearing(90.0, 0.0, 21.4225, 39.8262)
        assertEquals(140, bearing)
    }

    @Test
    fun `antipodal point`() {
        val bearing = calculateBearing(-21.4225, -140.1738, 21.4225, 39.8262)
        assertEquals(90, bearing)
    }
}
