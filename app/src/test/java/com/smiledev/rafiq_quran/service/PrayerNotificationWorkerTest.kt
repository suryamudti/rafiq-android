package com.smiledev.rafiq_quran.service

import android.content.Context
import com.smiledev.rafiq_quran.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class PrayerNotificationWorkerTest {

    private val context = mockk<Context>()

    @Before
    fun setup() {
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
    fun prayerFridayResourceIsGenerated() {
        assertNotEquals(0, R.string.prayer_friday)
    }

    @Test
    fun dhuhrReturnsFridayPrayerOnFriday() {
        val fridayCalendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
        }

        val resultDhuhr = PrayerNotificationWorker.getLocalizedPrayerName(context, "Dhuhr", fridayCalendar)
        assertEquals("Friday prayer", resultDhuhr)

        val resultDzuhur = PrayerNotificationWorker.getLocalizedPrayerName(context, "Dzuhur", fridayCalendar)
        assertEquals("Friday prayer", resultDzuhur)

        val resultDhuhrLower = PrayerNotificationWorker.getLocalizedPrayerName(context, "dhuhr", fridayCalendar)
        assertEquals("Friday prayer", resultDhuhrLower)
    }

    @Test
    fun dhuhrReturnsDhuhrOnOtherDays() {
        val thursdayCalendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY)
        }

        val resultDhuhr = PrayerNotificationWorker.getLocalizedPrayerName(context, "Dhuhr", thursdayCalendar)
        assertEquals("Dhuhr", resultDhuhr)

        val resultDzuhur = PrayerNotificationWorker.getLocalizedPrayerName(context, "Dzuhur", thursdayCalendar)
        assertEquals("Dhuhr", resultDzuhur)
    }

    @Test
    fun otherPrayersAreNotAffectedOnFriday() {
        val fridayCalendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
        }

        assertEquals("Fajr", PrayerNotificationWorker.getLocalizedPrayerName(context, "Fajr", fridayCalendar))
        assertEquals("Asr", PrayerNotificationWorker.getLocalizedPrayerName(context, "Asr", fridayCalendar))
        assertEquals("Maghrib", PrayerNotificationWorker.getLocalizedPrayerName(context, "Maghrib", fridayCalendar))
        assertEquals("Isha", PrayerNotificationWorker.getLocalizedPrayerName(context, "Isha", fridayCalendar))
        assertEquals("Fajr", PrayerNotificationWorker.getLocalizedPrayerName(context, "Subuh", fridayCalendar))
    }

    @Test
    fun explicitFridayPrayerNameReturnsFridayPrayer() {
        val wednesdayCalendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
        }

        assertEquals("Friday prayer", PrayerNotificationWorker.getLocalizedPrayerName(context, "Friday", wednesdayCalendar))
        assertEquals("Friday prayer", PrayerNotificationWorker.getLocalizedPrayerName(context, "Friday prayer", wednesdayCalendar))
        assertEquals("Friday prayer", PrayerNotificationWorker.getLocalizedPrayerName(context, "Jumat", wednesdayCalendar))
    }
}
