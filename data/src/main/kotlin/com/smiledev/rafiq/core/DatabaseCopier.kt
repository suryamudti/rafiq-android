package com.smiledev.rafiq.core

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private fun Context.translationDbFile(flatName: String): File {
    return File(filesDir, "databases/$flatName")
}

@Singleton
class DatabaseCopier @Inject constructor(private val context: Context) {

    fun copyDatabaseIfNeeded(dbName: String) {
        val flatName = dbName.replace('/', '_')
        val dbFile = context.translationDbFile(flatName)
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
        val assetPath = "quran-data/$dbName"
        val dbFile = context.translationDbFile(flatName)

        android.util.Log.i("DatabaseCopier", "Copying: asset=$assetPath -> $flatName")
        copyDatabaseIfNeeded(dbName)

        if (!dbFile.exists()) {
            android.util.Log.e("DatabaseCopier", "File does not exist after copy: $flatName")
            return false
        }
        if (dbFile.length() == 0L) {
            android.util.Log.e("DatabaseCopier", "File is empty after copy: $flatName (${dbFile.length()} bytes)")
            dbFile.delete()
            return false
        }
        android.util.Log.i("DatabaseCopier", "File size: ${dbFile.length()} bytes for $flatName")

        return try {
            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            android.util.Log.i("DatabaseCopier", "Successfully opened DB: $flatName")
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='verses'", null)
            val hasVersesTable = cursor.moveToFirst()
            cursor.close()
            if (!hasVersesTable) {
                android.util.Log.e("DatabaseCopier", "No 'verses' table found in $flatName")
                db.close()
                dbFile.delete()
                return false
            }
            android.util.Log.i("DatabaseCopier", "Verification passed for $flatName")
            db.close()
            true
        } catch (e: Exception) {
            android.util.Log.e("DatabaseCopier", "Verification failed for $flatName", e)
            dbFile.delete()
            false
        }
    }

}
