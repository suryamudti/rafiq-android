package com.smiledev.rafiq.core

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseCopier @Inject constructor(private val context: Context) {

    fun copyDatabaseIfNeeded(dbName: String) {
        val flatName = dbName.replace('/', '_')
        val dbFile = context.getDatabasePath(flatName)
        if (!dbFile.exists() || dbFile.length() == 0L) {
            try {
                dbFile.parentFile?.mkdirs()
                context.assets.open("quran-data/$dbName").use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DatabaseCopier", "Failed to copy database asset: $dbName", e)
            }
        }
    }

    fun copyAndVerifyTranslationDb(dbName: String): Boolean {
        val flatName = dbName.replace('/', '_')
        val dbFile = context.getDatabasePath(flatName)
        copyDatabaseIfNeeded(dbName)
        if (!dbFile.exists() || dbFile.length() == 0L) return false
        return try {
            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val hasTable = db.rawQuery("SELECT count(*) FROM verses WHERE sura = 1", null).use { cursor ->
                cursor.moveToFirst() && cursor.getInt(0) > 0
            }
            db.close()
            hasTable
        } catch (e: Exception) {
            android.util.Log.e("DatabaseCopier", "Verification failed for $dbName, deleting corrupt file", e)
            dbFile.delete()
            false
        }
    }

    fun copyAllQuranDatabases() {
        val databases = listOf(
            "quran-uthmani.db",
            "translations.db",
            "translations/en.sahih.db",
            "translations/id.indonesian.db"
        )
        databases.forEach { copyDatabaseIfNeeded(it) }
    }
}
