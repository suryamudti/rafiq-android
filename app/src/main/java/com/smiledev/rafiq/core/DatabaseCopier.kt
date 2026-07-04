package com.smiledev.rafiq.core

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseCopier @Inject constructor(private val context: Context) {

    fun copyDatabaseIfNeeded(dbName: String) {
        val flatName = dbName.replace('/', '_')
        val dbFile = context.getDatabasePath(flatName)
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            context.assets.open("quran-data/$dbName").use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
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
