package com.smiledev.rafiq_quran.tv.service

import android.content.Context
import android.content.Intent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class TvBootCompletedReceiverTest {

    private lateinit var context: Context
    private lateinit var appContext: Context
    private lateinit var receiver: TvBootCompletedReceiver

    @Before
    fun setUp() {
        appContext = mockk(relaxed = true)
        context = mockk(relaxed = true) {
            every { applicationContext } returns appContext
        }
        receiver = TvBootCompletedReceiver()
        mockkObject(TvPrayerNotificationWorker.Companion)
        every { TvPrayerNotificationWorker.schedule(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `reschedules prayer alarms on boot completed and package replaced`() {
        val bootIntent = mockk<Intent> {
            every { action } returns Intent.ACTION_BOOT_COMPLETED
        }
        val replacedIntent = mockk<Intent> {
            every { action } returns Intent.ACTION_MY_PACKAGE_REPLACED
        }

        receiver.onReceive(context, bootIntent)
        receiver.onReceive(context, replacedIntent)

        verify(exactly = 2) { TvPrayerNotificationWorker.schedule(appContext) }
    }

    @Test
    fun `reschedules prayer alarms on time and timezone changes`() {
        val timeIntent = mockk<Intent> {
            every { action } returns Intent.ACTION_TIME_CHANGED
        }
        val tzIntent = mockk<Intent> {
            every { action } returns Intent.ACTION_TIMEZONE_CHANGED
        }

        receiver.onReceive(context, timeIntent)
        receiver.onReceive(context, tzIntent)

        verify(exactly = 2) { TvPrayerNotificationWorker.schedule(appContext) }
    }

    @Test
    fun `ignores unrelated intents`() {
        val unrelated = mockk<Intent> {
            every { action } returns Intent.ACTION_BATTERY_LOW
        }

        receiver.onReceive(context, unrelated)
        receiver.onReceive(context, null)

        verify(exactly = 0) { TvPrayerNotificationWorker.schedule(any()) }
    }
}
