package com.smiledev.rafiq_quran.service

import android.app.AlarmManager
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

class BootCompletedReceiverTest {

    private lateinit var context: Context
    private lateinit var appContext: Context
    private lateinit var receiver: BootCompletedReceiver

    @Before
    fun setUp() {
        appContext = mockk(relaxed = true)
        context = mockk(relaxed = true) {
            every { applicationContext } returns appContext
        }
        receiver = BootCompletedReceiver()
        mockkObject(PrayerNotificationWorker.Companion)
        every { PrayerNotificationWorker.schedule(any()) } just runs
        every { PrayerNotificationWorker.scheduleNow(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `reschedules prayer worker on boot, package replaced, time, timezone, and alarm permission changes`() {
        val actions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )

        actions.forEach { actionName ->
            val intent = mockk<Intent> {
                every { action } returns actionName
            }
            receiver.onReceive(context, intent)
        }

        verify(exactly = actions.size) { PrayerNotificationWorker.schedule(appContext) }
        verify(exactly = actions.size) { PrayerNotificationWorker.scheduleNow(appContext) }
    }

    @Test
    fun `ignores unrelated actions and null intents`() {
        val unrelated = mockk<Intent> {
            every { action } returns Intent.ACTION_BATTERY_LOW
        }

        receiver.onReceive(context, unrelated)
        receiver.onReceive(context, null)

        verify(exactly = 0) { PrayerNotificationWorker.schedule(any()) }
        verify(exactly = 0) { PrayerNotificationWorker.scheduleNow(any()) }
    }
}
