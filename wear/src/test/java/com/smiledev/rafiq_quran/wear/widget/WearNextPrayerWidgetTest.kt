package com.smiledev.rafiq_quran.wear.widget

import com.smiledev.rafiq_quran.domain.model.PrayerTimesData
import com.smiledev.rafiq_quran.domain.model.PrayerTimings
import com.smiledev.rafiq_quran.wear.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class WearNextPrayerWidgetTest {

    private val sampleData = PrayerTimesData(
        timings = PrayerTimings(
            imsak = "04:25 (WIB)",
            fajr = "04:35 (WIB)",
            sunrise = "05:52 (WIB)",
            dhuhr = "12:00 (WIB)",
            asr = "15:15 (WIB)",
            maghrib = "18:00 (WIB)",
            isha = "19:15 (WIB)"
        ),
        hijriDate = "5 Jumada Al-Akhirah 1448"
    )

    @Test
    fun `computeSnapshot resolves upcoming Maghrib and progress before sunset`() {
        val now = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 45)
        }

        val snapshot = WearNextPrayerCalculator.computeSnapshot(
            data = sampleData,
            cityName = "Jakarta",
            calculationMethod = 20,
            isIndonesian = false,
            now = now
        )

        assertEquals("maghrib", snapshot.nextPrayerKey)
        assertEquals("Maghrib", snapshot.nextPrayerName)
        assertEquals("18:00", snapshot.nextPrayerTime)
        assertEquals("Asr", snapshot.previousPrayerName)
        assertEquals("15:15", snapshot.previousPrayerTime)
        assertEquals("1h 15m", snapshot.countdown)
        assertEquals("Jakarta", snapshot.cityName)
        assertEquals("KEMENAG", snapshot.calculationMethodName)
        assertEquals(R.drawable.ic_prayer_maghrib, snapshot.iconResId)
        assertTrue(snapshot.progressPercent in 50f..60f)

        val maghribItem = snapshot.schedule.first { it.key == "maghrib" }
        assertTrue(maghribItem.isNext)
        assertFalse(maghribItem.isPassed)
    }

    @Test
    fun `computeSnapshot wraps around to tomorrow Fajr when current time is after Isha`() {
        val now = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 15)
        }

        val snapshot = WearNextPrayerCalculator.computeSnapshot(
            data = sampleData,
            cityName = "Bandung",
            calculationMethod = 20,
            isIndonesian = true,
            now = now
        )

        assertEquals("fajr", snapshot.nextPrayerKey)
        assertEquals("Subuh", snapshot.nextPrayerName)
        assertEquals("04:35", snapshot.nextPrayerTime)
        assertEquals("Isya", snapshot.previousPrayerName)
        assertEquals("7h 20m", snapshot.countdown)
        assertEquals(R.drawable.ic_prayer_fajr, snapshot.iconResId)
        assertTrue(snapshot.progressPercent in 20f..25f)
    }

    @Test
    fun `computeSnapshot uses Friday prayer label on Fridays`() {
        val fridayMorning = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 15)
        }

        val snapshotEn = WearNextPrayerCalculator.computeSnapshot(
            data = sampleData,
            isIndonesian = false,
            now = fridayMorning
        )
        val snapshotId = WearNextPrayerCalculator.computeSnapshot(
            data = sampleData,
            isIndonesian = true,
            now = fridayMorning
        )

        assertEquals("Jumu'ah", snapshotEn.nextPrayerName)
        assertEquals("45m", snapshotEn.countdown)
        assertEquals("Jumat", snapshotId.nextPrayerName)
    }

    @Test
    fun `buildTile creates valid Wear OS Tile timeline`() {
        val snapshot = WearNextPrayerCalculator.defaultPreviewSnapshot(isIndonesian = false)
        val tile = WearNextPrayerTileService.buildTile(
            packageName = "com.smiledev.rafiq_quran",
            headerLabel = "NEXT PRAYER",
            snapshot = snapshot
        )

        assertNotNull(tile)
        assertEquals(WearNextPrayerTileService.RESOURCES_VERSION, tile.resourcesVersion)
        assertNotNull(tile.tileTimeline)
        assertTrue(tile.tileTimeline!!.timelineEntries.isNotEmpty())
    }
}
