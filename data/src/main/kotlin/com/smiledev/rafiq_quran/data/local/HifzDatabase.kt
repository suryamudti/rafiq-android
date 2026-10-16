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
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "hifz_goals")
data class HifzEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val suraNumber: Int,
    val suraName: String,
    val ayaStart: Int,
    val ayaEnd: Int,
    val memorizedAyahsCsv: String = ""
)

object HifzConverters {
    @TypeConverter
    fun fromCsv(csv: String): Set<Int> =
        csv.split(",").mapNotNull { it.toIntOrNull() }.toSet()

    @TypeConverter
    fun toCsv(set: Set<Int>): String =
        set.sorted().joinToString(",")
}

@Dao
interface HifzDao {
    @Query("SELECT * FROM hifz_goals ORDER BY id DESC")
    fun getAllGoals(): Flow<List<HifzEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: HifzEntity): Long

    @Query("DELETE FROM hifz_goals WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(entities = [HifzEntity::class], version = 1, exportSchema = false)
@TypeConverters(HifzConverters::class)
abstract class HifzDatabase : RoomDatabase() {
    abstract fun hifzDao(): HifzDao

    companion object {
        @Volatile
        private var INSTANCE: HifzDatabase? = null

        fun getInstance(context: Context): HifzDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HifzDatabase::class.java,
                    "hifz.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
