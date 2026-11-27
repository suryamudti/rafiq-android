package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DatabaseCopier
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.asSuccess
import com.smiledev.rafiq_quran.data.models.AyahData
import com.smiledev.rafiq_quran.data.toDomain
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.domain.model.Surah
import com.smiledev.rafiq_quran.domain.repository.QuranRepository
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val dbLock = Any()

    @Volatile
    private var quranDb: SQLiteDatabase? = null

    @Volatile
    private var translationIdDb: SQLiteDatabase? = null

    @Volatile
    private var translationEnDb: SQLiteDatabase? = null

    @Volatile
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
            val rawList = mutableListOf<AyahData>()
            db.rawQuery(
                "SELECT sura, aya, text, bismillah FROM quran WHERE sura = ? ORDER BY CAST(aya AS INTEGER) ASC",
                arrayOf(suraNumber.toString())
            ).use { cursor ->
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
            }

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

            val missingArabicKeys = keys.filter { it !in arabicMatches }
            val fetchedArabic = fetchArabicBatch(missingArabicKeys)

            val missingTranslationKeys = keys.filter { it !in translationMatches }
            val fetchedTranslations = fetchTranslationsBatch(missingTranslationKeys, localeCode)

            val results = keys.map { key ->
                val (sura, aya) = key
                val arabicData = arabicMatches[key] ?: fetchedArabic[key]
                val meta = metadata["$sura:$aya"]
                AyahData(
                    sura = sura,
                    aya = aya,
                    text = arabicData?.text ?: "",
                    bismillah = arabicData?.bismillah,
                    translation = translationMatches[key] ?: fetchedTranslations[key],
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
        metadataCache?.let { return it }
        return synchronized(dbLock) {
            metadataCache?.let { return it }
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
            map
        }
    }

    private fun getTranslationForSura(suraNumber: Int, localeCode: String): Map<Int, String> {
        val db = getTranslationDatabase(localeCode) ?: return emptyMap()
        val map = mutableMapOf<Int, String>()
        db.rawQuery(
            "SELECT ayah, text FROM verses WHERE CAST(sura AS INTEGER) = ?",
            arrayOf(suraNumber.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                map[cursor.getInt(0)] = cursor.getString(1)
            }
        }
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
        val existing = quranDb
        if (existing?.isOpen == true) return existing
        return synchronized(dbLock) {
            val current = quranDb
            if (current?.isOpen == true) return current
            databaseCopier.copyDatabaseIfNeeded("quran-uthmani.db")
            val dbFile = File(context.filesDir, "databases/quran-uthmani.db")
            val opened = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            quranDb = opened
            opened
        }
    }

    private fun getTranslationDatabase(localeCode: String): SQLiteDatabase? {
        val isId = localeCode == "id"
        val existing = if (isId) translationIdDb else translationEnDb
        if (existing?.isOpen == true) return existing
        return synchronized(dbLock) {
            val current = if (isId) translationIdDb else translationEnDb
            if (current?.isOpen == true) return current

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

            try {
                val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                if (isId) translationIdDb = db else translationEnDb = db
                db
            } catch (e: Exception) {
                android.util.Log.e("QuranRepository", "Error opening translation database: ${dbFile.absolutePath}", e)
                dbFile.delete()
                null
            }
        }
    }

    private fun readAssetJson(path: String): JSONObject {
        val text = BufferedReader(InputStreamReader(context.assets.open(path))).use { it.readText() }
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

    private fun fetchArabicBatch(keys: List<Pair<Int, Int>>): Map<Pair<Int, Int>, AyahData> {
        if (keys.isEmpty()) return emptyMap()
        val db = getQuranDatabase()
        val result = mutableMapOf<Pair<Int, Int>, AyahData>()
        for ((sura, pairs) in keys.groupBy { it.first }) {
            val distinctAyas = pairs.map { it.second }.distinct()
            for (chunk in distinctAyas.chunked(SQLITE_IN_CLAUSE_CHUNK_SIZE)) {
                val placeholders = chunk.joinToString(",") { "?" }
                val args = Array(chunk.size + 1) { index ->
                    if (index == 0) sura.toString() else chunk[index - 1].toString()
                }
                db.rawQuery(
                    "SELECT aya, text, bismillah FROM quran WHERE sura = ? AND aya IN ($placeholders)",
                    args
                ).use { c ->
                    while (c.moveToNext()) {
                        val aya = c.getString(0).toIntOrNull() ?: 0
                        val bismillahStr = if (c.isNull(2)) null else c.getString(2)
                        result[sura to aya] = AyahData(
                            sura = sura,
                            aya = aya,
                            text = c.getString(1),
                            bismillah = if (bismillahStr.isNullOrEmpty()) null else bismillahStr
                        )
                    }
                }
            }
        }
        return result
    }

    private fun fetchTranslationsBatch(
        keys: List<Pair<Int, Int>>,
        localeCode: String
    ): Map<Pair<Int, Int>, String> {
        if (keys.isEmpty()) return emptyMap()
        if (localeCode == "both") {
            val idTranslations = fetchTranslationsBatchSingle(keys, "id")
            val remaining = keys.filter { it !in idTranslations }
            if (remaining.isEmpty()) return idTranslations
            val enTranslations = fetchTranslationsBatchSingle(remaining, "en")
            return enTranslations + idTranslations
        }
        return fetchTranslationsBatchSingle(keys, localeCode)
    }

    private fun fetchTranslationsBatchSingle(
        keys: List<Pair<Int, Int>>,
        localeCode: String
    ): Map<Pair<Int, Int>, String> {
        if (keys.isEmpty()) return emptyMap()
        val db = getTranslationDatabase(localeCode) ?: return emptyMap()
        val result = mutableMapOf<Pair<Int, Int>, String>()
        for ((sura, pairs) in keys.groupBy { it.first }) {
            val distinctAyas = pairs.map { it.second }.distinct()
            for (chunk in distinctAyas.chunked(SQLITE_IN_CLAUSE_CHUNK_SIZE)) {
                val placeholders = chunk.joinToString(",") { "?" }
                val args = Array(chunk.size + 1) { index ->
                    if (index == 0) sura.toString() else chunk[index - 1].toString()
                }
                db.rawQuery(
                    "SELECT ayah, text FROM verses WHERE CAST(sura AS INTEGER) = ? AND CAST(ayah AS INTEGER) IN ($placeholders)",
                    args
                ).use { c ->
                    while (c.moveToNext()) {
                        val aya = c.getInt(0)
                        result[sura to aya] = c.getString(1)
                    }
                }
            }
        }
        return result
    }

    private fun escapeLike(input: String): String =
        input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    companion object {
        private const val SQLITE_IN_CLAUSE_CHUNK_SIZE = 500
    }
}
