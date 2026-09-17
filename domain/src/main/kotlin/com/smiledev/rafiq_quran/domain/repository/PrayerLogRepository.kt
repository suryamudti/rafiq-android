package com.smiledev.rafiq_quran.domain.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import kotlinx.coroutines.flow.Flow

data class PrayerLogDay(
    val date: String,
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false
) {
    val completedCount: Int
        get() = listOf(fajr, dhuhr, asr, maghrib, isha).count { it }

    val isAllCompleted: Boolean
        get() = fajr && dhuhr && asr && maghrib && isha
}

interface PrayerLogRepository {
    fun observeAll(): Flow<List<PrayerLogDay>>
    suspend fun getForDate(date: String): Result<PrayerLogDay?, AppError>
    suspend fun upsert(log: PrayerLogDay): Result<Unit, AppError>
}
