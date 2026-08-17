package com.smiledev.rafiq.data.repository

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DatabaseCopier
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.asSuccess
import com.smiledev.rafiq.domain.model.Hadith
import com.smiledev.rafiq.domain.model.HadithBook
import com.smiledev.rafiq.domain.repository.HadithRepository
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
}