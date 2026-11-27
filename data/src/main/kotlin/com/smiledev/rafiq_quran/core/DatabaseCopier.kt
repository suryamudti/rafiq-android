package com.smiledev.rafiq_quran.core

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

    private fun isValidDbName(dbName: String): Boolean {
        return dbName.isNotBlank() &&
            !dbName.contains("..") &&
            !dbName.startsWith("/") &&
            !dbName.contains("\\")
    }

    @Synchronized
    fun copyDatabaseIfNeeded(dbName: String) {
        if (!isValidDbName(dbName)) {
            android.util.Log.e("DatabaseCopier", "Invalid database name: $dbName")
            return
        }
        val flatName = dbName.replace('/', '_')
        val dbFile = context.translationDbFile(flatName)
        if (dbFile.exists() && dbFile.length() > 0L) return

        val tempFile = File(dbFile.parentFile, "${dbFile.name}.tmp")
        var promoted = false
        try {
            dbFile.parentFile?.mkdirs()
            if (tempFile.exists()) tempFile.delete()
            context.assets.open("quran-data/$dbName").use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            if (dbFile.exists()) {
                dbFile.delete()
            }
            if (tempFile.renameTo(dbFile)) {
                promoted = true
            } else {
                tempFile.inputStream().use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                        output.fd.sync()
                    }
                }
                promoted = true
                tempFile.delete()
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseCopier", "Failed to copy database asset: $dbName", e)
            tempFile.delete()
            if (!promoted && dbFile.exists()) {
                dbFile.delete()
            }
        }
    }

    @Synchronized
    fun copyAndVerifyTranslationDb(dbName: String): Boolean {
        if (!isValidDbName(dbName)) {
            android.util.Log.e("DatabaseCopier", "Invalid database name: $dbName")
            return false
        }
        val flatName = dbName.replace('/', '_')
        val dbFile = context.translationDbFile(flatName)

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

        val hasVersesTable = try {
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='verses'",
                    null
                ).use { cursor ->
                    cursor.moveToFirst()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseCopier", "Verification failed for $flatName", e)
            dbFile.delete()
            return false
        }

        if (!hasVersesTable) {
            android.util.Log.e("DatabaseCopier", "No 'verses' table found in $flatName")
            dbFile.delete()
            return false
        }
        return true
    }
}
