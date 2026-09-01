package com.smiledev.rafiq_quran.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "prayer_logs")
data class PrayerLogEntity(
    @PrimaryKey val date: String,
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false
)

@Dao
interface PrayerLogDao {
    @Query("SELECT * FROM prayer_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<PrayerLogEntity>>

    @Query("SELECT * FROM prayer_logs WHERE date = :date")
    suspend fun getLogForDate(date: String): PrayerLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: PrayerLogEntity)
}

@Database(entities = [PrayerLogEntity::class], version = 2, exportSchema = false)
abstract class PrayerLogDatabase : RoomDatabase() {
    abstract fun prayerLogDao(): PrayerLogDao

    companion object {
        @Volatile
        private var INSTANCE: PrayerLogDatabase? = null

        private val MIGRATION_1_2 = Room.Migration(
            fromVersion = 1,
            toVersion = 2) { database ->
            // Migration v1→v2: no-op since schema is backward-compatible
        }

        fun getInstance(context: Context): PrayerLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrayerLogDatabase::class.java,
                    "prayer_logs.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
