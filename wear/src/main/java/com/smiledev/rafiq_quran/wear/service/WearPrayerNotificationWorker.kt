package com.smiledev.rafiq_quran.wear.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result as CoreResult
import com.smiledev.rafiq_quran.core.retryIO
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.PrayerTimesData
import com.smiledev.rafiq_quran.domain.model.PrayerTimings
import com.smiledev.rafiq_quran.wear.R
import com.smiledev.rafiq_quran.wear.WearMainActivity
import com.smiledev.rafiq_quran.wear.navigation.WearRoutes
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class WearPrayerNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        const val CHANNEL_ID = "wear_prayer_times"
        const val WORK_NAME = "wear_prayer_notification_worker"
        const val IMMEDIATE_WORK_NAME = "wear_prayer_notification_immediate"
        private const val PRAYER_NOTIFICATION_ID = 2000
        internal val PRAYER_NAMES = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val midnight = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 5)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val delayMs = (midnight.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            val request = PeriodicWorkRequestBuilder<WearPrayerNotificationWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            scheduleNow(context)
        }

        fun scheduleNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<WearPrayerNotificationWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancelScheduledAlarms(context: Context) {
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            for (name in PRAYER_NAMES) {
                val intent = Intent(context, WearPrayerAlarmReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    context,
                    name.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    alarmManager.cancel(pi)
                } catch (_: Exception) {
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.wear_prayer_times),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.prayer_notification_channel_desc)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        fun getLocalizedPrayerName(
            context: Context,
            name: String,
            calendar: Calendar = Calendar.getInstance()
        ): String {
            val isFriday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            return when (name.lowercase()) {
                "imsak" -> context.getString(R.string.prayer_imsak)
                "fajr", "subuh" -> context.getString(R.string.prayer_fajr)
                "sunrise", "terbit" -> context.getString(R.string.prayer_sunrise)
                "dhuha" -> context.getString(R.string.prayer_dhuha)
                "dhuhr", "dzuhur" -> if (isFriday) {
                    context.getString(R.string.prayer_friday)
                } else {
                    context.getString(R.string.prayer_dhuhr)
                }
                "friday", "friday prayer", "friday_prayer", "jumat", "jum'at", "jumuah", "jumu'ah" ->
                    context.getString(R.string.prayer_friday)
                "asr", "ashar" -> context.getString(R.string.prayer_asr)
                "maghrib" -> context.getString(R.string.prayer_maghrib)
                "isha", "isya" -> context.getString(R.string.prayer_isha)
                else -> name
            }
        }

        @JvmOverloads
        fun postPrayerNotification(
            context: Context,
            name: String,
            time: String,
            calendar: Calendar = Calendar.getInstance()
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(context)
            }
            val intent = Intent(context, WearMainActivity::class.java).apply {
                putExtra("start_route", WearRoutes.PRAYER_TIMES)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val localizedName = getLocalizedPrayerName(context, name, calendar)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_prayer_times)
                .setContentTitle(context.getString(R.string.prayer_time_for, localizedName))
                .setContentText(context.getString(R.string.prayer_notification_body, localizedName, time))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(PRAYER_NOTIFICATION_ID + name.hashCode(), notification)
        }
    }

    override fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(applicationContext)
        }

        val prefs = PreferencesManager(applicationContext)
        data class PrefSnapshot(
            val latStr: String,
            val lonStr: String,
            val method: Int,
            val enabled: Boolean
        )

        val snapshot = runBlocking {
            combine(
                prefs.latitude,
                prefs.longitude,
                prefs.prayerCalculationMethod,
                prefs.prayerNotificationsEnabled
            ) { lat, lon, m, enabled ->
                PrefSnapshot(lat, lon, m, enabled)
            }.first()
        }

        if (!snapshot.enabled) {
            cancelScheduledAlarms(applicationContext)
            return Result.success()
        }

        val lat = snapshot.latStr.toDoubleOrNull() ?: -6.2088
        val lon = snapshot.lonStr.toDoubleOrNull() ?: 106.8456
        val calcMethod = if (snapshot.method == 2) 20 else snapshot.method

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val today = dateFormat.format(Date())

        val cached = runBlocking { prefs.getCachedPrayerTimes() }
        val timings: PrayerTimings = if (cached != null && cached.isValidFor(today, lat, lon, calcMethod)) {
            cached.data.timings
        } else {
            val fetched = fetchTimingsFromNetwork(today, lat, lon, calcMethod)
                ?: return Result.retry()
            runBlocking {
                prefs.saveCachedPrayerTimes(
                    date = today,
                    lat = lat,
                    lon = lon,
                    method = calcMethod,
                    data = fetched
                )
            }
            fetched.timings
        }

        val now = System.currentTimeMillis()
        val alarmManager = applicationContext.getSystemService(AlarmManager::class.java)
            ?: return Result.success()

        val timeByPrayer = mapOf(
            "Fajr" to timings.fajr,
            "Dhuhr" to timings.dhuhr,
            "Asr" to timings.asr,
            "Maghrib" to timings.maghrib,
            "Isha" to timings.isha
        )

        for (name in PRAYER_NAMES) {
            val time = timeByPrayer[name].orEmpty()
            if (time.isBlank()) continue
            val trigger = prayerTriggerMillis(time)
            if (trigger <= now) continue

            val intent = Intent(applicationContext, WearPrayerAlarmReceiver::class.java).apply {
                putExtra("name", name)
                putExtra("time", time)
            }
            val pi = PendingIntent.getBroadcast(
                applicationContext,
                name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val showIntent = Intent(applicationContext, WearMainActivity::class.java).apply {
                putExtra("start_route", WearRoutes.PRAYER_TIMES)
            }
            val showPi = PendingIntent.getActivity(
                applicationContext,
                0,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmInfo = AlarmManager.AlarmClockInfo(trigger, showPi)
            try {
                alarmManager.setAlarmClock(alarmInfo, pi)
            } catch (_: Exception) {
                try {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                } catch (_: Exception) {
                }
            }
        }

        return Result.success()
    }

    private fun fetchTimingsFromNetwork(
        today: String,
        lat: Double,
        lon: Double,
        method: Int
    ): PrayerTimesData? {
        return try {
            val fetchResult = runBlocking {
                retryIO(times = 3, initialDelay = 200) {
                    try {
                        val client = OkHttpClient.Builder()
                            .connectTimeout(20, TimeUnit.SECONDS)
                            .readTimeout(20, TimeUnit.SECONDS)
                            .writeTimeout(20, TimeUnit.SECONDS)
                            .build()
                        val url = "https://api.aladhan.com/v1/timings/$today?latitude=$lat&longitude=$lon&method=$method"
                        val request = Request.Builder().url(url).build()
                        val response = client.newCall(request).execute()
                        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                        val json = JSONObject(response.body?.string() ?: "")
                        val dataObj = json.getJSONObject("data")
                        val timingsObj = dataObj.getJSONObject("timings")
                        val hijriDate = dataObj.optJSONObject("date")
                            ?.optJSONObject("hijri")
                            ?.let { h ->
                                val day = h.optString("day", "")
                                val month = h.optJSONObject("month")?.optString("en", "") ?: ""
                                val year = h.optString("year", "")
                                "$day $month $year".trim()
                            }
                        CoreResult.Success(
                            PrayerTimesData(
                                timings = PrayerTimings(
                                    imsak = cleanTime(timingsObj.optString("Imsak", "04:18")),
                                    fajr = cleanTime(timingsObj.optString("Fajr", "04:28")),
                                    sunrise = cleanTime(timingsObj.optString("Sunrise", "05:44")),
                                    dhuhr = cleanTime(timingsObj.optString("Dhuhr", "11:52")),
                                    asr = cleanTime(timingsObj.optString("Asr", "15:12")),
                                    maghrib = cleanTime(timingsObj.optString("Maghrib", "17:56")),
                                    isha = cleanTime(timingsObj.optString("Isha", "19:07"))
                                ),
                                hijriDate = hijriDate
                            )
                        )
                    } catch (e: IOException) {
                        CoreResult.Error(AppError.Network(e.message ?: "network error", e))
                    } catch (e: Exception) {
                        CoreResult.Error(AppError.Network(e.message ?: "network error", e))
                    }
                }
            }
            when (fetchResult) {
                is CoreResult.Success -> fetchResult.data
                is CoreResult.Error -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun cleanTime(raw: String): String {
        return raw.substringBefore(" ").trim()
    }

    internal fun prayerTriggerMillis(time: String): Long {
        val clean = cleanTime(time)
        val parts = clean.split(":")
        if (parts.size != 2) return 0L
        val hour = parts[0].toIntOrNull() ?: return 0L
        val minute = parts[1].toIntOrNull() ?: return 0L
        if (hour !in 0..23 || minute !in 0..59) return 0L
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    internal fun prayerTriggerMillisPublic(time: String): Long = prayerTriggerMillis(time)
}
