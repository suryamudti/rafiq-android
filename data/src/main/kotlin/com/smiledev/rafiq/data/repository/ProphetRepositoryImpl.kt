package com.smiledev.rafiq.data.repository

import android.content.Context
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.asSuccess
import com.smiledev.rafiq.domain.model.ProphetStory
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

    private fun readAssetJsonArray(path: String): JSONArray {
        val stream = context.assets.open(path)
        val reader = BufferedReader(InputStreamReader(stream))
        val text = reader.readText()
        reader.close()
        return JSONArray(text)
    }
}
