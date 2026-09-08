package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DatabaseCopier
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.asSuccess
import com.smiledev.rafiq_quran.domain.model.Hadith
import com.smiledev.rafiq_quran.domain.model.HadithBook
import com.smiledev.rafiq_quran.domain.model.HadithTopic
import com.smiledev.rafiq_quran.domain.repository.HadithRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HadithRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseCopier: DatabaseCopier
) : HadithRepository {

    private var db: SQLiteDatabase? = null

    override fun getBooks(): Result<List<HadithBook>, AppError> {
        return try {
            val d = getDatabase()
            val cursor = d.rawQuery(
                "SELECT id, collection, number, name_ar, name_en, name_id FROM books ORDER BY collection, number",
                null
            )
            val list = mutableListOf<HadithBook>()
            while (cursor.moveToNext()) {
                list.add(
                    HadithBook(
                        id = cursor.getString(0),
                        collection = cursor.getString(1),
                        number = cursor.getInt(2),
                        nameAr = cursor.getString(3),
                        nameEn = cursor.getString(4),
                        nameId = cursor.getString(5)
                    )
                )
            }
            cursor.close()
            list.asSuccess()
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to load hadith books", e))
        }
    }

    override fun getHadithsByBook(bookId: String): Result<List<Hadith>, AppError> {
        return try {
            val d = getDatabase()
            val cursor = d.rawQuery(
                "SELECT id, book_id, in_book_number, narrator_ar, narrator_en, text_ar, text_en, text_id" +
                    " FROM hadiths WHERE book_id = ? ORDER BY in_book_number",
                arrayOf(bookId)
            )
            val list = mutableListOf<Hadith>()
            while (cursor.moveToNext()) {
                list.add(cursorToHadith(cursor))
            }
            cursor.close()
            list.asSuccess()
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to load hadiths for book $bookId", e))
        }
    }

    override fun searchHadiths(query: String, limit: Int): Result<List<Hadith>, AppError> {
        val term = query.trim()
        if (term.isEmpty()) return emptyList<Hadith>().asSuccess()
        return try {
            val d = getDatabase()
            val pattern = "%${escapeLike(term)}%"
            val args = arrayOf(pattern, pattern, pattern, pattern, pattern, pattern, limit.toString())
            val cursor = d.rawQuery(
                """
                SELECT h.id, h.book_id, h.in_book_number, h.narrator_ar, h.narrator_en,
                       h.text_ar, h.text_en, h.text_id
                FROM hadiths h
                JOIN books b ON b.id = h.book_id
                WHERE h.text_id LIKE ? ESCAPE '\'
                   OR h.text_en LIKE ? ESCAPE '\'
                   OR h.text_ar LIKE ? ESCAPE '\'
                   OR b.name_id LIKE ? ESCAPE '\'
                   OR b.name_en LIKE ? ESCAPE '\'
                   OR b.name_ar LIKE ? ESCAPE '\'
                ORDER BY h.id
                LIMIT ?
                """.trimIndent(),
                args
            )
            val list = mutableListOf<Hadith>()
            while (cursor.moveToNext()) {
                list.add(cursorToHadith(cursor))
            }
            cursor.close()
            list.asSuccess()
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to search hadiths for \"$query\"", e))
        }
    }

    override fun getHadithById(id: Int): Result<Hadith?, AppError> {
        return try {
            val d = getDatabase()
            val cursor = d.rawQuery(
                "SELECT id, book_id, in_book_number, narrator_ar, narrator_en, text_ar, text_en, text_id" +
                    " FROM hadiths WHERE id = ?",
                arrayOf(id.toString())
            )
            val hadith = if (cursor.moveToFirst()) cursorToHadith(cursor) else null
            cursor.close()
            hadith.asSuccess()
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to load hadith $id", e))
        }
    }

    override fun getTopics(): Result<List<HadithTopic>, AppError> {
        return TOPICS.asSuccess()
    }

    private fun escapeLike(term: String): String =
        term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private fun cursorToHadith(c: Cursor): Hadith = Hadith(
        id = c.getInt(0),
        bookId = c.getString(1),
        inBookNumber = c.getInt(2),
        narratorAr = c.getString(3).ifBlank { null },
        narratorEn = c.getString(4).ifBlank { null },
        textAr = c.getString(5),
        textEn = c.getString(6),
        textId = c.getString(7)
    )

    private fun getDatabase(): SQLiteDatabase {
        if (db?.isOpen == true) return db!!
        databaseCopier.copyDatabaseIfNeeded("hadiths/hadith.db")
        val flatName = "hadiths/hadith.db".replace('/', '_')
        val dbFile = File(context.filesDir, "databases/$flatName")
        if (!dbFile.exists() || dbFile.length() == 0L) {
            throw IllegalStateException("hadith.db missing after copy: $flatName")
        }
        val opened = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val cursor = opened.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('books','hadiths')",
            null
        )
        val tables = mutableSetOf<String>()
        while (cursor.moveToNext()) tables.add(cursor.getString(0))
        cursor.close()
        if (tables.size < 2) {
            opened.close()
            dbFile.delete()
            throw IllegalStateException("hadith.db missing required tables: $tables")
        }
        db = opened
        return opened
    }

    companion object {
        val TOPICS = listOf(
            HadithTopic(
                id = "faith",
                nameAr = "الإيمان والعقيدة",
                nameEn = "Faith & Creed",
                nameId = "Iman & Akidah",
                bookIds = listOf("bukhari.1", "bukhari.2", "bukhari.97", "muslim.1", "muslim.57")
            ),
            HadithTopic(
                id = "purification",
                nameAr = "الطهارة والوضوء",
                nameEn = "Purification",
                nameId = "Thaharah & Bersuci",
                bookIds = listOf("bukhari.4", "bukhari.5", "bukhari.6", "bukhari.7", "muslim.2", "muslim.3")
            ),
            HadithTopic(
                id = "prayer",
                nameAr = "الصلاة والمساجد",
                nameEn = "Prayer & Mosques",
                nameId = "Shalat & Masjid",
                bookIds = listOf(
                    "bukhari.8", "bukhari.9", "bukhari.10", "bukhari.11", "bukhari.12",
                    "bukhari.13", "bukhari.14", "bukhari.15", "bukhari.16", "bukhari.17",
                    "bukhari.18", "bukhari.19", "bukhari.20", "bukhari.21", "bukhari.22",
                    "bukhari.23", "muslim.4", "muslim.5", "muslim.6", "muslim.7",
                    "muslim.8", "muslim.9", "muslim.10", "muslim.11"
                )
            ),
            HadithTopic(
                id = "zakat",
                nameAr = "الزكاة والصدقة",
                nameEn = "Zakat & Charity",
                nameId = "Zakat & Sedekah",
                bookIds = listOf("bukhari.24", "muslim.12")
            ),
            HadithTopic(
                id = "fasting",
                nameAr = "الصيام والاعتكاف",
                nameEn = "Fasting & I'tikaf",
                nameId = "Puasa & Itikaf",
                bookIds = listOf("bukhari.30", "bukhari.31", "bukhari.32", "bukhari.33", "muslim.13", "muslim.14")
            ),
            HadithTopic(
                id = "hajj",
                nameAr = "الحج والعمرة",
                nameEn = "Hajj & Umrah",
                nameId = "Haji & Umrah",
                bookIds = listOf("bukhari.25", "bukhari.26", "bukhari.27", "bukhari.28", "bukhari.29", "muslim.15")
            ),
            HadithTopic(
                id = "family",
                nameAr = "النكاح والأسرة",
                nameEn = "Marriage & Family",
                nameId = "Pernikahan & Keluarga",
                bookIds = listOf("bukhari.67", "bukhari.68", "bukhari.69", "muslim.16", "muslim.17", "muslim.18", "muslim.19")
            ),
            HadithTopic(
                id = "transactions",
                nameAr = "المعاملات المالية",
                nameEn = "Commerce & Transactions",
                nameId = "Muamalah & Jual Beli",
                bookIds = listOf(
                    "bukhari.34", "bukhari.35", "bukhari.36", "bukhari.37", "bukhari.38",
                    "bukhari.39", "bukhari.40", "bukhari.41", "bukhari.42", "bukhari.43",
                    "bukhari.44", "bukhari.45", "bukhari.47", "bukhari.48", "bukhari.49",
                    "bukhari.50", "bukhari.51", "bukhari.54", "bukhari.55", "bukhari.85",
                    "muslim.20", "muslim.21", "muslim.22", "muslim.23", "muslim.24",
                    "muslim.25", "muslim.31"
                )
            ),
            HadithTopic(
                id = "manners",
                nameAr = "الأخلاق والآداب",
                nameEn = "Manners & Character",
                nameId = "Akhlak & Adab",
                bookIds = listOf("bukhari.78", "bukhari.79", "bukhari.95", "bukhari.96", "muslim.38", "muslim.39", "muslim.40", "muslim.41")
            ),
            HadithTopic(
                id = "supplication",
                nameAr = "الذكر والدعاء والرقاق",
                nameEn = "Remembrance & Supplication",
                nameId = "Dzikir, Doa & Kelembutan Hati",
                bookIds = listOf("bukhari.80", "bukhari.81", "muslim.48", "muslim.49", "muslim.50", "muslim.55")
            ),
            HadithTopic(
                id = "knowledge",
                nameAr = "العلم",
                nameEn = "Knowledge",
                nameId = "Ilmu Pengetahuan",
                bookIds = listOf("bukhari.3", "muslim.47")
            ),
            HadithTopic(
                id = "prophets",
                nameAr = "الأنبياء وفضائل الصحابة",
                nameEn = "Prophets & Merits",
                nameId = "Kisah Nabi & Keutamaan Sahabat",
                bookIds = listOf(
                    "bukhari.59", "bukhari.60", "bukhari.61", "bukhari.62", "bukhari.63",
                    "bukhari.64", "bukhari.65", "bukhari.66", "muslim.43", "muslim.44",
                    "muslim.45", "muslim.56"
                )
            ),
            HadithTopic(
                id = "governance",
                nameAr = "الجهاد والأحكام والقضاء",
                nameEn = "Jihad & Governance",
                nameId = "Jihad, Hukum & Pemerintahan",
                bookIds = listOf(
                    "bukhari.46", "bukhari.52", "bukhari.53", "bukhari.56", "bukhari.57",
                    "bukhari.58", "bukhari.86", "bukhari.87", "bukhari.88", "bukhari.89",
                    "bukhari.90", "bukhari.93", "muslim.28", "muslim.29", "muslim.30",
                    "muslim.32", "muslim.33"
                )
            ),
            HadithTopic(
                id = "lifestyle",
                nameAr = "الأطعمة والطب واللباس",
                nameEn = "Food, Health & Lifestyle",
                nameId = "Makanan, Kesehatan & Pakaian",
                bookIds = listOf(
                    "bukhari.70", "bukhari.71", "bukhari.72", "bukhari.73", "bukhari.74",
                    "bukhari.75", "bukhari.76", "bukhari.77", "muslim.34", "muslim.35",
                    "muslim.36", "muslim.37"
                )
            ),
            HadithTopic(
                id = "destiny",
                nameAr = "القدر والأيمان والنذور",
                nameEn = "Destiny & Oaths",
                nameId = "Takdir, Sumpah & Nazar",
                bookIds = listOf("bukhari.82", "bukhari.83", "bukhari.84", "bukhari.91", "bukhari.94", "muslim.26", "muslim.27", "muslim.42", "muslim.46")
            ),
            HadithTopic(
                id = "afterlife",
                nameAr = "الفتن وأشراط الساعة واليوم الآخر",
                nameEn = "Tribulations & Day of Judgment",
                nameId = "Fitnah & Hari Kiamat",
                bookIds = listOf("bukhari.92", "muslim.51", "muslim.52", "muslim.53", "muslim.54")
            )
        )
    }
}