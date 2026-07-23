package com.smiledev.rafiq.data.repository

import android.content.Context
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.local.PrayerLogDatabase
import com.smiledev.rafiq.data.local.PrayerLogEntity
import com.smiledev.rafiq.domain.repository.PrayerLogDay
import com.smiledev.rafiq.domain.repository.PrayerLogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerLogRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PrayerLogRepository {
    private val dao = PrayerLogDatabase.getInstance(context).prayerLogDao()

    override fun observeAll(): Flow<List<PrayerLogDay>> {
        return dao.getAllLogs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getForDate(date: String): Result<PrayerLogDay?, AppError> {
        return try {
            val entity = dao.getLogForDate(date)
            Result.Success(entity?.toDomain())
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to get prayer log for date", e))
        }
    }

    override suspend fun upsert(log: PrayerLogDay): Result<Unit, AppError> {
        return try {
            dao.upsert(log.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to upsert prayer log", e))
        }
    }
}

private fun PrayerLogEntity.toDomain() = PrayerLogDay(date = date, fajr = fajr, dhuhr = dhuhr, asr = asr, maghrib = maghrib, isha = isha)
private fun PrayerLogDay.toEntity() = PrayerLogEntity(date = date, fajr = fajr, dhuhr = dhuhr, asr = asr, maghrib = maghrib, isha = isha)
