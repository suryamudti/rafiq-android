package com.smiledev.rafiq_quran.data.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.local.TasbihDao
import com.smiledev.rafiq_quran.data.local.TasbihRecordEntity
import com.smiledev.rafiq_quran.domain.model.TasbihHistoryItem
import com.smiledev.rafiq_quran.domain.repository.TasbihHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasbihHistoryRepositoryImpl @Inject constructor(
    private val tasbihDao: TasbihDao
) : TasbihHistoryRepository {

    override fun observeAllRecords(): Flow<List<TasbihHistoryItem>> {
        return tasbihDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeDayRecords(date: String): Flow<List<TasbihHistoryItem>> {
        return tasbihDao.observeByDate(date).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addOrUpdateCount(
        date: String,
        dhikrId: Int,
        dhikrName: String,
        arabic: String,
        delta: Int
    ): Result<Unit, AppError> {
        return try {
            val existing = tasbihDao.getRecord(date, dhikrId, dhikrName)
            if (existing != null) {
                val newCount = (existing.count + delta).coerceAtLeast(0)
                tasbihDao.upsert(
                    existing.copy(
                        arabic = if (arabic.isNotEmpty()) arabic else existing.arabic,
                        count = newCount,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            } else {
                val newCount = delta.coerceAtLeast(0)
                tasbihDao.upsert(
                    TasbihRecordEntity(
                        date = date,
                        dhikrId = dhikrId,
                        dhikrName = dhikrName,
                        arabic = arabic,
                        count = newCount,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Error(AppError.Database("Failed to update tasbih record", e))
        }
    }

    override suspend fun deleteDay(date: String): Result<Unit, AppError> {
        return try {
            tasbihDao.deleteByDate(date)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Error(AppError.Database("Failed to delete tasbih day records", e))
        }
    }

    override suspend fun clearAll(): Result<Unit, AppError> {
        return try {
            tasbihDao.clearAll()
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Error(AppError.Database("Failed to clear tasbih records", e))
        }
    }

    private fun TasbihRecordEntity.toDomain(): TasbihHistoryItem {
        return TasbihHistoryItem(
            id = id,
            date = date,
            dhikrId = dhikrId,
            dhikrName = dhikrName,
            arabic = arabic,
            count = count,
            lastUpdated = lastUpdated
        )
    }
}
