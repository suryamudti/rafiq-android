package com.smiledev.rafiq.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.smiledev.rafiq.core.DatabaseCopier
import com.smiledev.rafiq.data.models.AyahData
import com.smiledev.rafiq.data.models.Surah
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

data class VerseMetadata(
    val page: Int,
    val juz: Int,
    val sajda: Boolean,
    val sajdaType: String?
)

@Singleton
class QuranRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseCopier: DatabaseCopier
) {
    private var quranDb: SQLiteDatabase? = null
    private var translationDb: SQLiteDatabase? = null
    private var currentTranslationDbName: String? = null
    private var metadataCache: Map<String, VerseMetadata>? = null

    fun getChapters(localeCode: String = "en"): List<Surah> {
        val json = readAssetJson("quran-data/chapters/chapters.$localeCode.json")
        val chaptersArray = json.getJSONArray("chapters")
        val list = mutableListOf<Surah>()
        for (i in 0 until chaptersArray.length()) {
            val c = chaptersArray.getJSONObject(i)
            list.add(
                Surah(
                    id = c.getInt("id"),
                    chapterNumber = c.getInt("chapter_number"),
                    nameArabic = c.getString("name_arabic"),
                    nameSimple = c.getString("name_simple"),
                    translatedName = c.getJSONObject("translated_name").getString("name"),
                    versesCount = c.getInt("verses_count"),
                    revelationPlace = c.getString("revelation_place")
                )
            )
        }
        return list
    }

    fun getAyahsWithTranslation(suraNumber: Int, localeCode: String = "en"): List<AyahData> {
        val db = getQuranDatabase()
        val cursor = db.rawQuery(
            "SELECT sura, aya, text, bismillah FROM quran WHERE sura = ? ORDER BY CAST(aya AS INTEGER) ASC",
            arrayOf(suraNumber.toString())
        )
        val rawList = mutableListOf<AyahData>()
        while (cursor.moveToNext()) {
            val bismillahStr = if (cursor.isNull(3)) null else cursor.getString(3)
            rawList.add(
                AyahData(
                    sura = cursor.getString(0).toIntOrNull() ?: 0,
                    aya = cursor.getString(1).toIntOrNull() ?: 0,
                    text = cursor.getString(2),
                    bismillah = if (bismillahStr.isNullOrEmpty()) null else bismillahStr
                )
            )
        }
        cursor.close()

        val metadata = getMetadataMap()
        val translationMap = getTranslationForSura(suraNumber, localeCode)

        val enrichedList = rawList.map { ayah ->
            val key = "${ayah.sura}:${ayah.aya}"
            val meta = metadata[key]
            ayah.copy(
                translation = translationMap[ayah.aya],
                page = meta?.page ?: 0,
                juz = meta?.juz ?: 0,
                sajda = meta?.sajda ?: false,
                sajdaType = meta?.sajdaType
            )
        }

        return enrichedList.mapIndexed { index, ayah ->
            val prevJuz = if (index > 0) enrichedList[index - 1].juz else ayah.juz
            val prevPage = if (index > 0) enrichedList[index - 1].page else ayah.page
            ayah.copy(
                isFirstAyaOfJuz = (ayah.juz != 0 && ayah.juz != prevJuz),
                isFirstAyaOfPage = (ayah.page != 0 && ayah.page != prevPage)
            )
        }
    }

    private fun getMetadataMap(): Map<String, VerseMetadata> {
        if (metadataCache != null) return metadataCache!!
        val json = readAssetJson("quran-data/quran-metadata.json")
        val verses = json.getJSONArray("verses")
        val map = mutableMapOf<String, VerseMetadata>()
        for (i in 0 until verses.length()) {
            val v = verses.getJSONObject(i)
            val sura = v.getInt("sura")
            val aya = v.getInt("aya")
            val sajdaObj = v.optJSONObject("sajda")
            val sajda = sajdaObj != null
            val sajdaType = sajdaObj?.optString("type").let { if (it.isNullOrEmpty()) null else it }
            map["$sura:$aya"] = VerseMetadata(
                page = v.getInt("page"),
                juz = v.getInt("juz"),
                sajda = sajda,
                sajdaType = sajdaType
            )
        }
        metadataCache = map
        return map
    }

    private fun getTranslationForSura(suraNumber: Int, localeCode: String): Map<Int, String> {
        val db = getTranslationDatabase(localeCode) ?: return emptyMap()
        val cursor = db.rawQuery(
            "SELECT ayah, text FROM verses WHERE sura = ?",
            arrayOf(suraNumber.toString())
        )
        val map = mutableMapOf<Int, String>()
        while (cursor.moveToNext()) {
            map[cursor.getInt(0)] = cursor.getString(1)
        }
        cursor.close()
        return map
    }

    private fun getQuranDatabase(): SQLiteDatabase {
        if (quranDb?.isOpen == true) return quranDb!!
        databaseCopier.copyDatabaseIfNeeded("quran-uthmani.db")
        val path = context.getDatabasePath("quran-uthmani.db").absolutePath
        quranDb = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
        return quranDb!!
    }

    private fun getTranslationDatabase(localeCode: String): SQLiteDatabase? {
        val fileKey = if (localeCode == "id") "translations/id.indonesian.db" else "translations/en.sahih.db"
        val flatName = fileKey.replace('/', '_')
        if (translationDb?.isOpen == true && currentTranslationDbName == flatName) return translationDb!!
        translationDb?.close()
        databaseCopier.copyDatabaseIfNeeded(fileKey)
        val path = context.getDatabasePath(flatName).absolutePath
        return try {
            val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
            translationDb = db
            currentTranslationDbName = flatName
            db
        } catch (_: Exception) {
            null
        }
    }

    private fun readAssetJson(path: String): JSONObject {
        val stream = context.assets.open(path)
        val reader = BufferedReader(InputStreamReader(stream))
        val text = reader.readText()
        reader.close()
        return JSONObject(text)
    }
}
