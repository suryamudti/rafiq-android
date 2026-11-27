package com.smiledev.rafiq_quran.wear.widget

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.domain.model.PrayerTimesData
import com.smiledev.rafiq_quran.domain.model.PrayerTimings
import com.smiledev.rafiq_quran.wear.R
import com.smiledev.rafiq_quran.wear.service.WearPrayerWorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Immutable
data class WearUpcomingPrayerItem(
    val key: String,
    val name: String,
    val time: String,
    @DrawableRes val iconResId: Int,
    val isNext: Boolean,
    val isPassed: Boolean
)

@Immutable
data class WearNextPrayerSnapshot(
    val nextPrayerKey: String,
    val nextPrayerName: String,
    val nextPrayerTime: String,
    val previousPrayerName: String,
    val previousPrayerTime: String,
    val countdown: String,
    val cityName: String,
    val calculationMethodName: String,
    val hijriDate: String,
    @DrawableRes val iconResId: Int,
    val schedule: List<WearUpcomingPrayerItem>
)

object WearNextPrayerCalculator {

    private data class RawPrayerDef(
        val key: String,
        val nameEn: String,
        val nameId: String,
        val rawTime: String,
        @DrawableRes val iconResId: Int
    )

    fun computeSnapshot(
        data: PrayerTimesData,
        cityName: String = "Jakarta",
        calculationMethod: Int = 20,
        isIndonesian: Boolean = currentLocaleCode() == "id",
        now: Calendar = Calendar.getInstance()
    ): WearNextPrayerSnapshot {
        val isFriday = now.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
        val dhuhrEn = if (isFriday) "Jumu'ah" else "Dhuhr"
        val dhuhrId = if (isFriday) "Jumat" else "Dzuhur"

        val defs = listOf(
            RawPrayerDef("fajr", "Fajr", "Subuh", cleanTime(data.timings.fajr), R.drawable.ic_prayer_fajr),
            RawPrayerDef("dhuhr", dhuhrEn, dhuhrId, cleanTime(data.timings.dhuhr), R.drawable.ic_prayer_dhuhr),
            RawPrayerDef("asr", "Asr", "Ashar", cleanTime(data.timings.asr), R.drawable.ic_prayer_asr),
            RawPrayerDef("maghrib", "Maghrib", "Maghrib", cleanTime(data.timings.maghrib), R.drawable.ic_prayer_maghrib),
            RawPrayerDef("isha", "Isha", "Isya", cleanTime(data.timings.isha), R.drawable.ic_prayer_isha)
        )

        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val parsedMinutes = defs.map { parseMinutes(it.rawTime) ?: 0 }

        var nextIndex = -1
        for (i in defs.indices) {
            if (parsedMinutes[i] > nowMinutes) {
                nextIndex = i
                break
            }
        }

        val wrapsToTomorrowFajr = nextIndex == -1
        val resolvedNextIndex = if (wrapsToTomorrowFajr) 0 else nextIndex
        val resolvedPrevIndex = if (resolvedNextIndex == 0) defs.lastIndex else resolvedNextIndex - 1

        val nextDef = defs[resolvedNextIndex]
        val prevDef = defs[resolvedPrevIndex]
        val nextMinutes = parsedMinutes[resolvedNextIndex]

        val diffMinutes = if (wrapsToTomorrowFajr || nextMinutes <= nowMinutes) {
            (24 * 60 - nowMinutes) + nextMinutes
        } else {
            nextMinutes - nowMinutes
        }.coerceAtLeast(1)

        val hours = diffMinutes / 60
        val mins = diffMinutes % 60
        val countdown = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        val schedule = defs.mapIndexed { idx, def ->
            val localizedName = if (isIndonesian) def.nameId else def.nameEn
            val isPassed = !wrapsToTomorrowFajr && parsedMinutes[idx] <= nowMinutes
            val isNext = idx == resolvedNextIndex
            WearUpcomingPrayerItem(
                key = def.key,
                name = localizedName,
                time = def.rawTime,
                iconResId = def.iconResId,
                isNext = isNext,
                isPassed = isPassed
            )
        }

        val nextLocalizedName = if (isIndonesian) nextDef.nameId else nextDef.nameEn
        val prevLocalizedName = if (isIndonesian) prevDef.nameId else prevDef.nameEn
        val resolvedCity = cityName.ifBlank { "Jakarta" }

        return WearNextPrayerSnapshot(
            nextPrayerKey = nextDef.key,
            nextPrayerName = nextLocalizedName,
            nextPrayerTime = nextDef.rawTime,
            previousPrayerName = prevLocalizedName,
            previousPrayerTime = prevDef.rawTime,
            countdown = countdown,
            cityName = resolvedCity,
            calculationMethodName = getMethodDisplayName(calculationMethod),
            hijriDate = data.hijriDate.orEmpty(),
            iconResId = nextDef.iconResId,
            schedule = schedule
        )
    }

    fun defaultPreviewSnapshot(
        isIndonesian: Boolean = currentLocaleCode() == "id",
        now: Calendar = Calendar.getInstance()
    ): WearNextPrayerSnapshot {
        val sampleData = PrayerTimesData(
            timings = PrayerTimings(
                imsak = "04:25",
                fajr = "04:35",
                sunrise = "05:52",
                dhuhr = "11:58",
                asr = "15:18",
                maghrib = "18:01",
                isha = "19:12"
            ),
            hijriDate = "5 Jumada Al-Akhirah 1448"
        )
        return computeSnapshot(
            data = sampleData,
            cityName = "Jakarta",
            calculationMethod = 20,
            isIndonesian = isIndonesian,
            now = now
        )
    }

    fun getMethodDisplayName(method: Int): String {
        return when (method) {
            20 -> "KEMENAG"
            1 -> "Univ. Karachi"
            2 -> "ISNA"
            3 -> "MWL"
            4 -> "Umm Al-Qura"
            5 -> "Egyptian"
            11 -> "MUIS"
            17 -> "JAKIM"
            else -> "Method $method"
        }
    }

    internal fun cleanTime(raw: String): String {
        return raw.substringBefore(" ").trim()
    }

    internal fun parseMinutes(time: String): Int? {
        val clean = cleanTime(time)
        val parts = clean.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }
}

object WearNextPrayerDataProvider {

    private data class PrefConfig(
        val lat: Double,
        val lon: Double,
        val method: Int,
        val city: String
    )

    suspend fun resolveSnapshot(
        context: Context,
        now: Calendar = Calendar.getInstance()
    ): WearNextPrayerSnapshot {
        val appContext = context.applicationContext
        val entryPoint = runCatching {
            EntryPointAccessors.fromApplication(
                appContext,
                WearPrayerWorkerEntryPoint::class.java
            )
        }.getOrNull() ?: return WearNextPrayerCalculator.defaultPreviewSnapshot(now = now)

        val prefs = entryPoint.preferencesManager()
        val repository = entryPoint.prayerTimesRepository()

        val config = runCatching {
            combine(
                prefs.latitude,
                prefs.longitude,
                prefs.prayerCalculationMethod,
                prefs.cityName
            ) { latStr, lonStr, method, city ->
                val lat = latStr.toDoubleOrNull() ?: -6.2088
                val lon = lonStr.toDoubleOrNull() ?: 106.8456
                val resolvedCity = city.ifBlank { "Jakarta" }
                PrefConfig(lat, lon, method, resolvedCity)
            }.first()
        }.getOrElse {
            PrefConfig(-6.2088, 106.8456, 20, "Jakarta")
        }

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val todayStr = dateFormat.format(Date(now.timeInMillis))

        val prayerData = runCatching {
            repository.getCachedPrayerTimes(config.lat, config.lon, todayStr, config.method)
                ?: when (val res = repository.fetchPrayerTimes(config.lat, config.lon, todayStr, config.method, forceRefresh = false)) {
                    is Result.Success -> res.data
                    is Result.Error -> null
                }
        }.getOrNull()

        return if (prayerData != null) {
            WearNextPrayerCalculator.computeSnapshot(
                data = prayerData,
                cityName = config.city,
                calculationMethod = config.method,
                now = now
            )
        } else {
            WearNextPrayerCalculator.defaultPreviewSnapshot(now = now).copy(
                cityName = config.city,
                calculationMethodName = WearNextPrayerCalculator.getMethodDisplayName(config.method)
            )
        }
    }
}
