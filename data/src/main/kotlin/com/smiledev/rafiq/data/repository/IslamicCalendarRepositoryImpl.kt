package com.smiledev.rafiq.data.repository

import android.content.Context
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.asSuccess
import com.smiledev.rafiq.domain.model.IslamicEvent
import com.smiledev.rafiq.domain.repository.IslamicCalendarRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IslamicCalendarRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IslamicCalendarRepository {
    private var cache: List<IslamicEvent>? = null

    override val islamicMonthNames = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhul-Qi'dah", "Dhul-Hijjah"
    )

    override val islamicMonthNamesId = listOf(
        "Muharram", "Safar", "Rabi'ul Awwal", "Rabi'ul Tsani",
        "Jumadil Awwal", "Jumadil Tsani", "Rajab", "Sya'ban",
        "Ramadan", "Syawwal", "Dzulqa'dah", "Dzulhijjah"
    )

    override fun getEvents(): Result<List<IslamicEvent>, AppError> {
        return try {
            if (cache != null) return cache!!.asSuccess()
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
            list.asSuccess()
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to load Islamic events", e))
        }
    }

    override fun getEventsForMonth(month: Int): Result<List<IslamicEvent>, AppError> {
        return try {
            when (val result = getEvents()) {
                is Result.Success -> Result.Success(result.data.filter { it.hijriMonth == month })
                is Result.Error -> result
            }
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to get events for month", e))
        }
    }

    override fun getTodayEvents(): Result<List<IslamicEvent>, AppError> {
        return try {
            when (val result = getEvents()) {
                is Result.Success -> {
                    val cal = Calendar.getInstance()
                    val todayMonth = cal.get(Calendar.MONTH) + 1
                    val todayDay = cal.get(Calendar.DAY_OF_MONTH)
                    val filtered = result.data.filter { it.hijriMonth == todayMonth && it.hijriDay == todayDay }
                        .ifEmpty { result.data.filter { it.hijriMonth == 1 && it.hijriDay == 1 } }
                    Result.Success(filtered)
                }
                is Result.Error -> result
            }
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to get today events", e))
        }
    }

    private fun readAssetJsonArray(path: String): JSONArray {
        val stream = context.assets.open(path)
        val reader = BufferedReader(InputStreamReader(stream))
        val text = reader.readText()
        reader.close()
        return JSONArray(text)
    }
}
