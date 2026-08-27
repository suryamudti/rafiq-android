package com.smiledev.rafiq_quran.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PrayerNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        const val CHANNEL_ID = "prayer_times"
        const val WORK_NAME = "prayer_notification_worker"
        private const val PRAYER_NOTIFICATION_ID = 1000
        private val PRAYER_NAMES = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PrayerNotificationWorker>(
                1, TimeUnit.DAYS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun scheduleNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<PrayerNotificationWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Prayer Times",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for prayer times"
                enableVibration(true)
                val soundUri = Uri.parse(
                    "android.resource://${context.packageName}/raw/adzan_default"
                )
                setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        fun postPrayerNotification(context: Context, name: String, time: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Time for $name")
                .setContentText("It's time to pray $name ($time)")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(PRAYER_NOTIFICATION_ID + name.hashCode(), notification)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureChannel() {
        createNotificationChannel(applicationContext)
    }

    override fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ensureChannel()
        }

        val prefs = PreferencesManager(applicationContext)
        val lat = runBlocking { prefs.latitude.first() }.toDoubleOrNull() ?: -6.2088
        val lon = runBlocking { prefs.longitude.first() }.toDoubleOrNull() ?: 106.8456
        val method = runBlocking { prefs.prayerCalculationMethod.first() }

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val today = dateFormat.format(Date())

        val timings = try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val url = "https://api.aladhan.com/v1/timings/$today?latitude=$lat&longitude=$lon&method=$method"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            json.getJSONObject("data").getJSONObject("timings")
        } catch (e: Exception) {
            return Result.retry()
        }

        val now = System.currentTimeMillis()
        val alarmManager = applicationContext.getSystemService(AlarmManager::class.java)
        var scheduled = 0

        for (name in PRAYER_NAMES) {
            val time = timings.optString(name, "")
            if (time.isBlank()) continue
            val trigger = prayerTriggerMillis(time)
            if (trigger <= now) continue
            val intent = Intent(applicationContext, PrayerAlarmReceiver::class.java).apply {
                putExtra("name", name)
                putExtra("time", time)
            }
            val pi = PendingIntent.getBroadcast(
                applicationContext,
                name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val showIntent = applicationContext.packageManager
                .getLaunchIntentForPackage(applicationContext.packageName)
            val showPi = PendingIntent.getActivity(
                applicationContext, 0, showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmInfo = AlarmManager.AlarmClockInfo(trigger, showPi)
            try {
                alarmManager.setAlarmClock(alarmInfo, pi)
                scheduled++
            } catch (_: Exception) {
                // AlarmManager may be unavailable; skip this prayer.
            }
        }

        if (scheduled == 0) {
            postPrayerNotification(applicationContext, "Prayers", "see the app for today's times")
        }

        return Result.success()
    }

    private fun prayerTriggerMillis(time: String): Long {
        val parts = time.split(":")
        if (parts.size != 2) return 0L
        val hour = parts[0].toIntOrNull() ?: return 0L
        val minute = parts[1].toIntOrNull() ?: return 0L
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
