package com.smiledev.rafiq_quran.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "tasbih_records",
    indices = [Index(value = ["date", "dhikrId", "dhikrName"], unique = true)]
)
data class TasbihRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val dhikrId: Int,
    val dhikrName: String,
    val arabic: String = "",
    val count: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Dao
interface TasbihDao {
    @Query("SELECT * FROM tasbih_records ORDER BY date DESC, lastUpdated DESC")
    fun observeAll(): Flow<List<TasbihRecordEntity>>

    @Query("SELECT * FROM tasbih_records WHERE date = :date ORDER BY lastUpdated DESC")
    fun observeByDate(date: String): Flow<List<TasbihRecordEntity>>

    @Query("SELECT * FROM tasbih_records WHERE date = :date AND dhikrId = :dhikrId AND dhikrName = :dhikrName LIMIT 1")
    suspend fun getRecord(date: String, dhikrId: Int, dhikrName: String): TasbihRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: TasbihRecordEntity): Long

    @Query("DELETE FROM tasbih_records WHERE date = :date")
    suspend fun deleteByDate(date: String): Int

    @Query("DELETE FROM tasbih_records")
    suspend fun clearAll(): Int
}

@Database(entities = [TasbihRecordEntity::class], version = 1, exportSchema = false)
abstract class TasbihDatabase : RoomDatabase() {
    abstract fun tasbihDao(): TasbihDao

    companion object {
        @Volatile
        private var INSTANCE: TasbihDatabase? = null

        fun getInstance(context: Context): TasbihDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TasbihDatabase::class.java,
                    "tasbih.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
