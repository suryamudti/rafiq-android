package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import kotlinx.coroutines.flow.Flow

data class PrayerLogDay(
    val date: String,
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false
)

interface PrayerLogRepository {
    fun observeAll(): Flow<List<PrayerLogDay>>
    suspend fun getForDate(date: String): Result<PrayerLogDay?, AppError>
    suspend fun upsert(log: PrayerLogDay): Result<Unit, AppError>
}
