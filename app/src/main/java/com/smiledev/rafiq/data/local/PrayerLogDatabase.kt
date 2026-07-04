package com.smiledev.rafiq.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Entity(tableName = "prayer_logs")
data class PrayerLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val fajr: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0
)

@Dao
interface PrayerLogDao {
    @Query("SELECT * FROM prayer_logs WHERE date LIKE :monthPrefix ORDER BY date ASC")
    suspend fun getLogsForMonth(monthPrefix: String): List<PrayerLogEntity>

    @Query("SELECT * FROM prayer_logs WHERE date = :date LIMIT 1")
    suspend fun getLogForDate(date: String): PrayerLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(log: PrayerLogEntity): Long

    @Query("SELECT * FROM prayer_logs ORDER BY date DESC")
    suspend fun getAllLogs(): List<PrayerLogEntity>

    @Query("SELECT * FROM prayer_logs ORDER BY date DESC LIMIT 30")
    suspend fun getRecentLogs(): List<PrayerLogEntity>
}

@Database(entities = [PrayerLogEntity::class], version = 1, exportSchema = false)
abstract class PrayerLogDatabase : RoomDatabase() {
    abstract fun prayerLogDao(): PrayerLogDao

    companion object {
        @Volatile
        private var INSTANCE: PrayerLogDatabase? = null

        fun getInstance(context: Context): PrayerLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrayerLogDatabase::class.java,
                    "prayer_logs.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
