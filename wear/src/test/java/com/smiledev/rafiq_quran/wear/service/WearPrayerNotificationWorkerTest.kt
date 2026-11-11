package com.smiledev.rafiq_quran.wear.service

import android.content.Context
import androidx.work.WorkerParameters
import com.smiledev.rafiq_quran.wear.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class WearPrayerNotificationWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var worker: WearPrayerNotificationWorker

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        worker = WearPrayerNotificationWorker(context, workerParams)

        every { context.getString(R.string.prayer_imsak) } returns "Imsak"
        every { context.getString(R.string.prayer_fajr) } returns "Fajr"
        every { context.getString(R.string.prayer_sunrise) } returns "Sunrise"
        every { context.getString(R.string.prayer_dhuha) } returns "Dhuha"
        every { context.getString(R.string.prayer_dhuhr) } returns "Dhuhr"
        every { context.getString(R.string.prayer_friday) } returns "Friday prayer"
        every { context.getString(R.string.prayer_asr) } returns "Asr"
        every { context.getString(R.string.prayer_maghrib) } returns "Maghrib"
        every { context.getString(R.string.prayer_isha) } returns "Isha"
    }

    @Test
    fun `getLocalizedPrayerName returns Friday prayer for Dhuhr on Friday`() {
        val fridayCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
        }
        assertEquals(
            "Friday prayer",
            WearPrayerNotificationWorker.getLocalizedPrayerName(context, "Dhuhr", fridayCal)
        )
        assertEquals(
            "Friday prayer",
            WearPrayerNotificationWorker.getLocalizedPrayerName(context, "Dzuhur", fridayCal)
        )
    }

    @Test
    fun `getLocalizedPrayerName returns Dhuhr on non-Friday`() {
        val mondayCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        assertEquals(
            "Dhuhr",
            WearPrayerNotificationWorker.getLocalizedPrayerName(context, "Dhuhr", mondayCal)
        )
    }

    @Test
    fun `prayerTriggerMillis parses valid HH mm and rejects invalid strings`() {
        val validTrigger = worker.prayerTriggerMillisPublic("04:30 (WIB)")
        assertTrue(validTrigger > 0L)
        assertEquals(0L, worker.prayerTriggerMillisPublic("invalid"))
        assertEquals(0L, worker.prayerTriggerMillisPublic("25:00"))
    }
}
