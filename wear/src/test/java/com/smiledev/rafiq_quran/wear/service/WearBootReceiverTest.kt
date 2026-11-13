package com.smiledev.rafiq_quran.wear.service

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

class WearBootReceiverTest {

    private lateinit var context: Context
    private lateinit var appContext: Context

    @Before
    fun setUp() {
        appContext = mockk(relaxed = true)
        context = mockk(relaxed = true)
        every { context.applicationContext } returns appContext
        mockkObject(WearPrayerNotificationWorker.Companion)
        every { WearPrayerNotificationWorker.schedule(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onReceive schedules worker on BOOT_COMPLETED and MY_PACKAGE_REPLACED`() {
        val receiver = WearBootReceiver()
        val bootIntent = mockk<Intent>()
        every { bootIntent.action } returns Intent.ACTION_BOOT_COMPLETED

        val updateIntent = mockk<Intent>()
        every { updateIntent.action } returns Intent.ACTION_MY_PACKAGE_REPLACED

        val alarmPermIntent = mockk<Intent>()
        every { alarmPermIntent.action } returns AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED

        receiver.onReceive(context, bootIntent)
        receiver.onReceive(context, updateIntent)
        receiver.onReceive(context, alarmPermIntent)

        verify(exactly = 3) { WearPrayerNotificationWorker.schedule(appContext) }
    }

    @Test
    fun `onReceive ignores unrelated actions`() {
        val receiver = WearBootReceiver()
        val otherIntent = mockk<Intent>()
        every { otherIntent.action } returns Intent.ACTION_TIME_CHANGED

        receiver.onReceive(context, otherIntent)
        receiver.onReceive(context, null)

        verify(exactly = 0) { WearPrayerNotificationWorker.schedule(any()) }
    }
}
