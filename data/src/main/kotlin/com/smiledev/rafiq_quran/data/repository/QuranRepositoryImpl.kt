package com.smiledev.rafiq.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DatabaseCopier
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.asSuccess
import com.smiledev.rafiq.data.models.AyahData
import com.smiledev.rafiq.data.toDomain
import com.smiledev.rafiq.domain.model.Ayah
import com.smiledev.rafiq.domain.model.Surah
import com.smiledev.rafiq.domain.repository.QuranRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
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
class QuranRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseCopier: DatabaseCopier
) : QuranRepository {
    private var quranDb: SQLiteDatabase? = null
    private var translationIdDb: SQLiteDatabase? = null
    private var translationEnDb: SQLiteDatabase? = null
    private var metadataCache: Map<String, VerseMetadata>? = null

    override fun getChapters(localeCode: String): Result<List<Surah>, AppError> {
        return try {
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
            list.asSuccess()
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to load chapters", e))
        }
    }

    override fun getAyahsWithTranslation(suraNumber: Int, localeCode: String): Result<List<Ayah>, AppError> {
        return try {
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
            val translationMapId = getTranslationForSuraSafe(suraNumber, "id")
            val translationMapEn = getTranslationForSuraSafe(suraNumber, "en")

            val enrichedList = rawList.map { ayah ->
                val key = "${ayah.sura}:${ayah.aya}"
                val meta = metadata[key]
                val resolvedTranslation = if (localeCode == "id") translationMapId[ayah.aya] else translationMapEn[ayah.aya]
                ayah.copy(
                    translation = resolvedTranslation,
                    translationId = translationMapId[ayah.aya],
                    translationEn = translationMapEn[ayah.aya],
                    page = meta?.page ?: 0,
                    juz = meta?.juz ?: 0,
                    sajda = meta?.sajda ?: false,
                    sajdaType = meta?.sajdaType
                )
            }

            val result = enrichedList.mapIndexed { index, ayah ->
                val prevJuz = if (index > 0) enrichedList[index - 1].juz else ayah.juz
                val prevPage = if (index > 0) enrichedList[index - 1].page else ayah.page
                ayah.copy(
                    isFirstAyaOfJuz = (ayah.juz != 0 && ayah.juz != prevJuz),
                    isFirstAyaOfPage = (ayah.page != 0 && ayah.page != prevPage)
                ).toDomain()
            }
            result.asSuccess()
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to load ayahs", e))
        }
    }

    override fun searchAyahs(query: String, localeCode: String, limit: Int): Result<List<Ayah>, AppError> {
        return try {
            val term = query.trim()
            if (term.isEmpty()) return emptyList<Ayah>().asSuccess()
            val pattern = "%${escapeLike(term)}%"

            val arabicMatches = searchArabic(pattern, limit)
            val translationMatches = searchTranslation(pattern, localeCode, limit)

            val keys = (arabicMatches.keys + translationMatches.keys)
                .sortedWith(compareBy({ it.first }, { it.second }))
                .take(limit)
            val metadata = getMetadataMap()

            val results = keys.map { (sura, aya) ->
                val arabicData = arabicMatches[sura to aya] ?: fetchArabic(sura, aya)
                val meta = metadata["$sura:$aya"]
                AyahData(
                    sura = sura,
                    aya = aya,
                    text = arabicData?.text ?: "",
                    bismillah = arabicData?.bismillah,
                    translation = translationMatches[sura to aya]
                        ?: getTranslationForAya(sura, aya, localeCode),
                    page = meta?.page ?: 0,
                    juz = meta?.juz ?: 0,
                    sajda = meta?.sajda ?: false,
                    sajdaType = meta?.sajdaType
                ).toDomain()
            }
            results.asSuccess()
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to search ayahs", e))
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
            "SELECT ayah, text FROM verses WHERE CAST(sura AS INTEGER) = ?",
            arrayOf(suraNumber.toString())
        )
        val map = mutableMapOf<Int, String>()
        while (cursor.moveToNext()) {
            map[cursor.getInt(0)] = cursor.getString(1)
        }
        cursor.close()
        return map
    }

    private fun getTranslationForSuraSafe(suraNumber: Int, localeCode: String): Map<Int, String> {
        return try {
            getTranslationForSura(suraNumber, localeCode)
        } catch (e: Exception) {
            android.util.Log.e("QuranRepository", "Failed to load $localeCode translation for sura $suraNumber", e)
            emptyMap()
        }
    }

    private fun getQuranDatabase(): SQLiteDatabase {
        if (quranDb?.isOpen == true) return quranDb!!
        databaseCopier.copyDatabaseIfNeeded("quran-uthmani.db")
        val dbFile = File(context.filesDir, "databases/quran-uthmani.db")
        quranDb = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        return quranDb!!
    }

    private fun getTranslationDatabase(localeCode: String): SQLiteDatabase? {
        val isId = localeCode == "id"
        val cachedDb = if (isId) translationIdDb else translationEnDb
        if (cachedDb?.isOpen == true) return cachedDb

        val fileKey = if (isId) "translations/id.indonesian.db" else "translations/en.sahih.db"
        val flatName = fileKey.replace('/', '_')
        val dbFile = File(context.filesDir, "databases/$flatName")

        if (!databaseCopier.copyAndVerifyTranslationDb(fileKey)) {
            android.util.Log.w("QuranRepository", "Retrying copy for $fileKey after failed verification")
            dbFile.delete()
            if (!databaseCopier.copyAndVerifyTranslationDb(fileKey)) {
                android.util.Log.e("QuranRepository", "Failed to copy and verify translation database: $fileKey")
                return null
            }
        }

        return try {
            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            if (isId) translationIdDb = db else translationEnDb = db
            android.util.Log.i("QuranRepository", "Opened translation DB: $fileKey")
            db
        } catch (e: Exception) {
            android.util.Log.e("QuranRepository", "Error opening translation database: ${dbFile.absolutePath}", e)
            dbFile.delete()
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

    private fun searchArabic(pattern: String, limit: Int): Map<Pair<Int, Int>, AyahData> {
        val db = getQuranDatabase()
        val result = mutableMapOf<Pair<Int, Int>, AyahData>()
        db.rawQuery(
            "SELECT sura, aya, text, bismillah FROM quran WHERE text LIKE ? ESCAPE '\\' " +
                "ORDER BY CAST(sura AS INTEGER), CAST(aya AS INTEGER) LIMIT ?",
            arrayOf(pattern, limit.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val sura = c.getString(0).toIntOrNull() ?: 0
                val aya = c.getString(1).toIntOrNull() ?: 0
                val bismillahStr = if (c.isNull(3)) null else c.getString(3)
                result[sura to aya] = AyahData(
                    sura = sura,
                    aya = aya,
                    text = c.getString(2),
                    bismillah = if (bismillahStr.isNullOrEmpty()) null else bismillahStr
                )
            }
        }
        return result
    }

    private fun searchTranslation(pattern: String, localeCode: String, limit: Int): Map<Pair<Int, Int>, String> {
        if (localeCode == "both") {
            val merged = searchTranslationSingle(pattern, "en", limit).toMutableMap()
            merged.putAll(searchTranslationSingle(pattern, "id", limit))
            return merged
        }
        return searchTranslationSingle(pattern, localeCode, limit)
    }

    private fun searchTranslationSingle(pattern: String, localeCode: String, limit: Int): Map<Pair<Int, Int>, String> {
        val db = getTranslationDatabase(localeCode) ?: return emptyMap()
        val result = mutableMapOf<Pair<Int, Int>, String>()
        db.rawQuery(
            "SELECT sura, ayah, text FROM verses WHERE text LIKE ? ESCAPE '\\' " +
                "ORDER BY CAST(sura AS INTEGER), ayah LIMIT ?",
            arrayOf(pattern, limit.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val sura = c.getString(0).toIntOrNull() ?: 0
                val aya = c.getInt(1)
                result[sura to aya] = c.getString(2)
            }
        }
        return result
    }

    private fun fetchArabic(sura: Int, aya: Int): AyahData? {
        val db = getQuranDatabase()
        db.rawQuery(
            "SELECT text, bismillah FROM quran WHERE sura = ? AND aya = ?",
            arrayOf(sura.toString(), aya.toString())
        ).use { c ->
            if (!c.moveToFirst()) return null
            val bismillahStr = if (c.isNull(1)) null else c.getString(1)
            return AyahData(
                sura = sura,
                aya = aya,
                text = c.getString(0),
                bismillah = if (bismillahStr.isNullOrEmpty()) null else bismillahStr
            )
        }
    }

    private fun getTranslationForAya(sura: Int, aya: Int, localeCode: String): String? {
        if (localeCode == "both") {
            return getTranslationForAyaSingle(sura, aya, "id") ?: getTranslationForAyaSingle(sura, aya, "en")
        }
        return getTranslationForAyaSingle(sura, aya, localeCode)
    }

    private fun getTranslationForAyaSingle(sura: Int, aya: Int, localeCode: String): String? {
        val db = getTranslationDatabase(localeCode) ?: return null
        db.rawQuery(
            "SELECT text FROM verses WHERE CAST(sura AS INTEGER) = ? AND ayah = ?",
            arrayOf(sura.toString(), aya.toString())
        ).use { c ->
            return if (c.moveToFirst()) c.getString(0) else null
        }
    }

    private fun escapeLike(input: String): String =
        input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
