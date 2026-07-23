package com.smiledev.rafiq.data.repository

import android.content.Context
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.local.BookmarkDatabase
import com.smiledev.rafiq.data.local.BookmarkEntity
import com.smiledev.rafiq.domain.repository.BookmarkItem
import com.smiledev.rafiq.domain.repository.BookmarkRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BookmarkRepository {
    private val dao = BookmarkDatabase.getInstance(context).bookmarkDao()

    override fun observeAll(): Flow<List<BookmarkItem>> {
        return dao.getAllBookmarks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun isBookmarked(sura: Int, aya: Int): Result<Boolean, AppError> {
        return try {
            Result.Success(dao.isBookmarked(sura, aya))
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to check bookmark", e))
        }
    }

    override suspend fun toggle(sura: Int, aya: Int, suraName: String): Result<Unit, AppError> {
        return try {
            val already = dao.isBookmarked(sura, aya)
            if (already) {
                dao.delete(sura, aya)
            } else {
                dao.insert(BookmarkEntity(sura = sura, aya = aya, suraName = suraName, insertTime = ""))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to toggle bookmark", e))
        }
    }

    override suspend fun delete(sura: Int, aya: Int): Result<Unit, AppError> {
        return try {
            dao.delete(sura, aya)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to delete bookmark", e))
        }
    }
}

private fun BookmarkEntity.toDomain() = BookmarkItem(id = id, sura = sura, suraName = suraName, aya = aya, insertTime = insertTime)
