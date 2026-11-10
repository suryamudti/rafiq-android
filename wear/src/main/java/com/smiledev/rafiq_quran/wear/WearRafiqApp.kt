package com.smiledev.rafiq_quran.wear

import android.app.Application
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.PrayerTimesData
import com.smiledev.rafiq_quran.domain.model.PrayerTimings
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class WearRafiqApp : Application() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        runBlocking(Dispatchers.IO) {
            if (preferencesManager.getCachedPrayerTimes() == null) {
                val today = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
                preferencesManager.saveCachedPrayerTimes(
                    date = today,
                    lat = -6.2088,
                    lon = 106.8456,
                    method = 20,
                    data = PrayerTimesData(
                        timings = PrayerTimings(
                            imsak = "04:18",
                            fajr = "04:28",
                            sunrise = "05:44",
                            dhuhr = "11:52",
                            asr = "15:12",
                            maghrib = "17:56",
                            isha = "19:07"
                        ),
                        hijriDate = "9 Jumada al-Ula 1448"
                    )
                )
            }
        }
    }
}
