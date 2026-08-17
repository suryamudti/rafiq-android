package com.smiledev.rafiq.data.repository

import android.content.Context
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.asSuccess
import com.smiledev.rafiq.domain.model.ProphetStory
import com.smiledev.rafiq.domain.model.VerseRef
import com.smiledev.rafiq.domain.repository.ProphetRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProphetRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ProphetRepository {
    private var cache: List<ProphetStory>? = null

    override fun getProphets(): Result<List<ProphetStory>, AppError> {
        return try {
            if (cache != null) return cache!!.asSuccess()
            val text = readAssetText("quran-data/prophets/prophets.json")
            val list = parseProphets(text)
            cache = list
            list.asSuccess()
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to load prophets", e))
        }
    }

    override fun getProphetById(id: Int): Result<ProphetStory?, AppError> {
        return try {
            when (val result = getProphets()) {
                is Result.Success -> Result.Success(result.data.find { it.id == id })
                is Result.Error -> result
            }
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to get prophet by id", e))
        }
    }

    private fun readAssetText(path: String): String {
        val stream = context.assets.open(path)
        val reader = BufferedReader(InputStreamReader(stream))
        val text = reader.readText()
        reader.close()
        return text
    }
}

internal fun parseProphets(json: String): List<ProphetStory> {
    val arr = JSONArray(json)
    val list = mutableListOf<ProphetStory>()
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        list.add(
            ProphetStory(
                id = obj.getInt("id"),
                nameArabic = obj.getString("name_arabic"),
                nameEn = obj.getString("name_en"),
                nameId = obj.getString("name_id"),
                summaryEn = obj.getString("summary_en"),
                summaryId = obj.getString("summary_id"),
                storyEn = obj.getString("story_en"),
                storyId = obj.getString("story_id"),
                miraclesEn = obj.getString("miracles_en"),
                miraclesId = obj.getString("miracles_id"),
                eraEn = obj.optString("era_en"),
                eraId = obj.optString("era_id"),
                peopleEn = obj.optString("people_en"),
                peopleId = obj.optString("people_id"),
                lifespanEn = obj.optString("lifespan_en"),
                lifespanId = obj.optString("lifespan_id"),
                eventsEn = obj.optJSONArray("events_en").toStringList(),
                eventsId = obj.optJSONArray("events_id").toStringList(),
                lessonsEn = obj.optJSONArray("lessons_en").toStringList(),
                lessonsId = obj.optJSONArray("lessons_id").toStringList(),
                verses = obj.optJSONArray("verses").toVerseRefs()
            )
        )
    }
    return list
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).map { optString(it) }
}

private fun JSONArray?.toVerseRefs(): List<VerseRef> {
    if (this == null) return emptyList()
    return (0 until length()).map { i ->
        val v = getJSONObject(i)
        VerseRef(
            surah = v.getInt("surah"),
            surahNameEn = v.getString("surah_name_en"),
            surahNameId = v.getString("surah_name_id"),
            ayahStart = v.getInt("ayah_start"),
            ayahEnd = v.getInt("ayah_end")
        )
    }
}
