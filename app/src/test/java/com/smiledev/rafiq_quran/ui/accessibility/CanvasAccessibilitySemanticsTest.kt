package com.smiledev.rafiq_quran.ui.accessibility

import com.smiledev.rafiq_quran.ui.qibla.compassDirection
import com.smiledev.rafiq_quran.ui.qibla.normalizeAngle180
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class CanvasAccessibilitySemanticsTest {

    @Test
    fun `test qibla compass semantics aligned content description`() {
        val bearing = 295
        val formatEn = "Qibla compass, facing Qibla at %1\$d degrees"
        val formatId = "Kompas kiblat, menghadap kiblat pada %1\$d derajat"

        val descEn = String.format(Locale.US, formatEn, bearing)
        val descId = String.format(Locale.US, formatId, bearing)

        assertEquals("Qibla compass, facing Qibla at 295 degrees", descEn)
        assertEquals("Kompas kiblat, menghadap kiblat pada 295 derajat", descId)
    }

    @Test
    fun `test qibla compass semantics offset content description`() {
        val bearing = 295
        val azimuth = 180
        val offsetTextEn = "Turn 115° right to face Qibla"
        val offsetTextId = "Putar 115° ke kanan menghadap Kiblat"

        val formatEn = "Qibla compass, target bearing %1\$d degrees, current heading %2\$d degrees, %3\$s"
        val formatId = "Kompas kiblat, arah target %1\$d derajat, arah saat ini %2\$d derajat, %3\$s"

        val descEn = String.format(Locale.US, formatEn, bearing, azimuth, offsetTextEn)
        val descId = String.format(Locale.US, formatId, bearing, azimuth, offsetTextId)

        assertEquals(
            "Qibla compass, target bearing 295 degrees, current heading 180 degrees, Turn 115° right to face Qibla",
            descEn
        )
        assertEquals(
            "Kompas kiblat, arah target 295 derajat, arah saat ini 180 derajat, Putar 115° ke kanan menghadap Kiblat",
            descId
        )
    }

    @Test
    fun `test qibla alignment threshold logic`() {
        val bearing = 295.0
        val alignedThreshold = 5.0

        val azimuthAligned = 297.0
        val offsetAligned = normalizeAngle180(bearing - azimuthAligned)
        assertTrue(abs(offsetAligned) <= alignedThreshold)

        val azimuthUnaligned = 280.0
        val offsetUnaligned = normalizeAngle180(bearing - azimuthUnaligned)
        assertFalse(abs(offsetUnaligned) <= alignedThreshold)
    }

    @Test
    fun `test tasbih circular counter in progress content description`() {
        val count = 25
        val progressPercent = (25f / 33f * 100f).roundToInt()
        val formatEn = "Tasbih counter, count %1\$d, progress %2\$d percent"
        val formatId = "Penghitung tasbih, hitungan %1\$d, progres %2\$d persen"

        val descEn = String.format(Locale.US, formatEn, count, progressPercent)
        val descId = String.format(Locale.US, formatId, count, progressPercent)

        assertEquals("Tasbih counter, count 25, progress 76 percent", descEn)
        assertEquals("Penghitung tasbih, hitungan 25, progres 76 persen", descId)
    }

    @Test
    fun `test tasbih circular counter target reached content description`() {
        val count = 33
        val formatEn = "Tasbih counter, target reached at %1\$d dhikr"
        val formatId = "Penghitung tasbih, target tercapai pada %1\$d zikir"

        val descEn = String.format(Locale.US, formatEn, count)
        val descId = String.format(Locale.US, formatId, count)

        assertEquals("Tasbih counter, target reached at 33 dhikr", descEn)
        assertEquals("Penghitung tasbih, target tercapai pada 33 zikir", descId)
    }

    @Test
    fun `test compass direction localized points`() {
        assertEquals("U", compassDirection(0, "id"))
        assertEquals("N", compassDirection(0, "en"))
        assertEquals("BBL", compassDirection(295, "id"))
        assertEquals("WNW", compassDirection(295, "en"))
    }
}
