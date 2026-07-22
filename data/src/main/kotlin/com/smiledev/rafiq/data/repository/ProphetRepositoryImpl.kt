package com.smiledev.rafiq.data.repository

import android.content.Context
import com.smiledev.rafiq.data.models.ProphetStory
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProphetRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cache: List<ProphetStory>? = null

    fun getProphets(): List<ProphetStory> {
        if (cache != null) return cache!!
        val json = readAssetJsonArray("quran-data/prophets/prophets.json")
        val list = mutableListOf<ProphetStory>()
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
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
                    miraclesId = obj.getString("miracles_id")
                )
            )
        }
        cache = list
        return list
    }

    fun getProphetById(id: Int): ProphetStory? = getProphets().find { it.id == id }

    private fun readAssetJsonArray(path: String): JSONArray {
        val stream = context.assets.open(path)
        val reader = BufferedReader(InputStreamReader(stream))
        val text = reader.readText()
        reader.close()
        return JSONArray(text)
    }
}
