package com.smiledev.rafiq.data.local

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
