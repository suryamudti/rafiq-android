package com.smiledev.rafiq.data.repository

import android.content.Context
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
                list.add(
                    Hadith(
                        id = cursor.getInt(0),
                        bookId = cursor.getString(1),
                        inBookNumber = cursor.getInt(2),
                        narratorAr = cursor.getString(3).ifBlank { null },
                        narratorEn = cursor.getString(4).ifBlank { null },
                        textAr = cursor.getString(5),
                        textEn = cursor.getString(6),
                        textId = cursor.getString(7)
                    )
                )
            }
            cursor.close()
            list.asSuccess()
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to load hadiths for book $bookId", e))
        }
    }

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