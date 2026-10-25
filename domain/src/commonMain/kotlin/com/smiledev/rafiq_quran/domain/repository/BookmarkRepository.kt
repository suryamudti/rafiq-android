package com.smiledev.rafiq_quran.domain.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import kotlinx.coroutines.flow.Flow

data class BookmarkItem(
    val id: Int,
    val sura: Int,
    val suraName: String,
    val aya: Int,
    val insertTime: String
)

interface BookmarkRepository {
    fun observeAll(): Flow<List<BookmarkItem>>
    suspend fun isBookmarked(sura: Int, aya: Int): Result<Boolean, AppError>
    suspend fun toggle(sura: Int, aya: Int, suraName: String): Result<Unit, AppError>
    suspend fun delete(sura: Int, aya: Int): Result<Unit, AppError>
}
