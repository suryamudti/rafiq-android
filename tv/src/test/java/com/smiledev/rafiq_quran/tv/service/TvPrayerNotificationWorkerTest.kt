package com.smiledev.rafiq_quran.tv.service

import android.content.Context
import androidx.work.WorkerParameters
import com.smiledev.rafiq_quran.tv.R
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class TvPrayerNotificationWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var worker: TvPrayerNotificationWorker

    @Before
    fun setUp() {
        TvPrayerNotificationWorker.dismissInAppAdhanAlert()
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        worker = TvPrayerNotificationWorker(context, workerParams)

        every { context.getString(R.string.prayer_imsak) } returns "Imsak"
        every { context.getString(R.string.prayer_fajr) } returns "Fajr"
        every { context.getString(R.string.prayer_sunrise) } returns "Sunrise"
        every { context.getString(R.string.prayer_dhuha) } returns "Dhuha"
        every { context.getString(R.string.prayer_dhuhr) } returns "Dhuhr"
        every { context.getString(R.string.prayer_friday) } returns "Jumu'ah"
        every { context.getString(R.string.prayer_asr) } returns "Asr"
        every { context.getString(R.string.prayer_maghrib) } returns "Maghrib"
        every { context.getString(R.string.prayer_isha) } returns "Isha"
    }

    @After
    fun tearDown() {
        TvPrayerNotificationWorker.dismissInAppAdhanAlert()
    }

    @Test
    fun `getLocalizedPrayerName returns Jumu'ah for Dhuhr on Friday`() {
        val fridayCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
        }
        assertEquals(
            "Jumu'ah",
            TvPrayerNotificationWorker.getLocalizedPrayerName(context, "Dhuhr", fridayCal)
        )
        assertEquals(
            "Jumu'ah",
            TvPrayerNotificationWorker.getLocalizedPrayerName(context, "Dzuhur", fridayCal)
        )
    }

    @Test
    fun `getLocalizedPrayerName returns Dhuhr on non-Friday`() {
        val mondayCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        assertEquals(
            "Dhuhr",
            TvPrayerNotificationWorker.getLocalizedPrayerName(context, "Dhuhr", mondayCal)
        )
    }

    @Test
    fun `prayerTriggerMillis parses valid HH mm and rejects invalid strings`() {
        val validTrigger = worker.prayerTriggerMillisPublic("04:30 (WIB)")
        assertTrue(validTrigger > 0L)
        assertEquals(0L, worker.prayerTriggerMillisPublic("invalid"))
        assertEquals(0L, worker.prayerTriggerMillisPublic("25:00"))
    }

    @Test
    fun `triggerInAppAdhanAlert and dismissInAppAdhanAlert update state flow`() {
        assertNull(TvPrayerNotificationWorker.activeAdhanAlert.value)
        TvPrayerNotificationWorker.triggerInAppAdhanAlert("Maghrib", "17:56")
        val alert = TvPrayerNotificationWorker.activeAdhanAlert.value
        assertNotNull(alert)
        assertEquals("Maghrib", alert?.prayerName)
        assertEquals("17:56", alert?.prayerTime)

        TvPrayerNotificationWorker.dismissInAppAdhanAlert()
        assertNull(TvPrayerNotificationWorker.activeAdhanAlert.value)
    }
}
