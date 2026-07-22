package com.smiledev.rafiq.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smiledev.rafiq.data.models.Reciter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReciterRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    fun getReciters(): List<Reciter> {
        val json = context.assets.open("quran-data/reciters.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<ReciterRaw>>() {}.type
        val raw: List<ReciterRaw> = gson.fromJson(json, type)
        return raw.map {
            Reciter(
                id = it.id,
                nameEn = it.name_en,
                nameAr = it.name_ar,
                style = it.style,
                country = it.country,
                identifier = it.identifier
            )
        }
    }
}

private data class ReciterRaw(
    val id: Int,
    val name_en: String,
    val name_ar: String,
    val style: String,
    val country: String,
    val identifier: String
)
