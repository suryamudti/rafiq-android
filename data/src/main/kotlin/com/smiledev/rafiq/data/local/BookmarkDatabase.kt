package com.smiledev.rafiq.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarks", indices = [Index("sura")])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sura: Int,
    val suraName: String,
    val aya: Int,
    val insertTime: String
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY id DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE sura = :sura")
    suspend fun getBookmarksBySura(sura: Int): List<BookmarkEntity>

    @Query("SELECT aya FROM bookmarks WHERE sura = :sura")
    suspend fun getAyasBySura(sura: Int): List<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE sura = :sura AND aya = :aya)")
    suspend fun isBookmarked(sura: Int, aya: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE sura = :sura AND aya = :aya")
    suspend fun delete(sura: Int, aya: Int): Int

    @Delete
    suspend fun delete(bookmark: BookmarkEntity): Int
}

@Database(entities = [BookmarkEntity::class], version = 1, exportSchema = false)
abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: BookmarkDatabase? = null

        fun getInstance(context: Context): BookmarkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookmarkDatabase::class.java,
                    "bookmarks.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
