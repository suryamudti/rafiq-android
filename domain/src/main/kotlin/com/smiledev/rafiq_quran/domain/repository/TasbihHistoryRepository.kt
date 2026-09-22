package com.smiledev.rafiq_quran.domain.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.TasbihHistoryItem
import kotlinx.coroutines.flow.Flow

interface TasbihHistoryRepository {
    fun observeAllRecords(): Flow<List<TasbihHistoryItem>>
    fun observeDayRecords(date: String): Flow<List<TasbihHistoryItem>>
    suspend fun addOrUpdateCount(
        date: String,
        dhikrId: Int,
        dhikrName: String,
        arabic: String,
        delta: Int
    ): Result<Unit, AppError>
    suspend fun deleteDay(date: String): Result<Unit, AppError>
    suspend fun clearAll(): Result<Unit, AppError>
}
