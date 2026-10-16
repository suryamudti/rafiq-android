package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.local.HifzDatabase
import com.smiledev.rafiq_quran.data.local.HifzEntity
import com.smiledev.rafiq_quran.domain.repository.HifzGoal
import com.smiledev.rafiq_quran.domain.repository.HifzRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HifzRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : HifzRepository {
    private val dao = HifzDatabase.getInstance(context).hifzDao()

    override fun observeAll(): Flow<List<HifzGoal>> {
        return dao.getAllGoals().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun upsert(goal: HifzGoal): Result<Unit, AppError> {
        return try {
            dao.upsert(goal.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to upsert hifz goal", e))
        }
    }

    override suspend fun delete(id: Long): Result<Unit, AppError> {
        return try {
            dao.deleteById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to delete hifz goal", e))
        }
    }
}

internal fun HifzEntity.toDomain() = HifzGoal(
    id = id,
    suraNumber = suraNumber,
    suraName = suraName,
    ayaStart = ayaStart,
    ayaEnd = ayaEnd,
    memorizedAyahs = memorizedAyahsCsv.split(",").mapNotNull { it.toIntOrNull() }.toSet()
)

internal fun HifzGoal.toEntity() = HifzEntity(
    id = id,
    suraNumber = suraNumber,
    suraName = suraName,
    ayaStart = ayaStart,
    ayaEnd = ayaEnd,
    memorizedAyahsCsv = memorizedAyahs.sorted().joinToString(",")
)
