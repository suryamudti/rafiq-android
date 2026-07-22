package com.smiledev.rafiq.data.repository

import android.content.Context
import com.smiledev.rafiq.data.models.IslamicEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IslamicCalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cache: List<IslamicEvent>? = null

    val islamicMonthNames = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhul-Qi'dah", "Dhul-Hijjah"
    )

    val islamicMonthNamesId = listOf(
        "Muharram", "Safar", "Rabi'ul Awwal", "Rabi'ul Tsani",
        "Jumadil Awwal", "Jumadil Tsani", "Rajab", "Sya'ban",
        "Ramadan", "Syawwal", "Dzulqa'dah", "Dzulhijjah"
    )

    fun getEvents(): List<IslamicEvent> {
        if (cache != null) return cache!!
        val json = readAssetJsonArray("quran-data/islamic_events.json")
        val list = mutableListOf<IslamicEvent>()
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            list.add(
                IslamicEvent(
                    hijriMonth = obj.getInt("hijri_month"),
                    hijriDay = obj.getInt("hijri_day"),
                    titleEn = obj.getString("title_en"),
                    titleId = obj.getString("title_id"),
                    descriptionEn = obj.getString("description_en"),
                    descriptionId = obj.getString("description_id"),
                    eventType = obj.getString("event_type")
                )
            )
        }
        cache = list
        return list
    }

    fun getEventsForMonth(month: Int): List<IslamicEvent> =
        getEvents().filter { it.hijriMonth == month }

    fun getTodayEvents(): List<IslamicEvent> {
        val cal = Calendar.getInstance()
        val todayMonth = cal.get(Calendar.MONTH) + 1
        val todayDay = cal.get(Calendar.DAY_OF_MONTH)
        return getEvents().filter { it.hijriMonth == todayMonth && it.hijriDay == todayDay }
            .ifEmpty { getEvents().filter { it.hijriMonth == 1 && it.hijriDay == 1 } }
    }

    private fun readAssetJsonArray(path: String): JSONArray {
        val stream = context.assets.open(path)
        val reader = BufferedReader(InputStreamReader(stream))
        val text = reader.readText()
        reader.close()
        return JSONArray(text)
    }
}
