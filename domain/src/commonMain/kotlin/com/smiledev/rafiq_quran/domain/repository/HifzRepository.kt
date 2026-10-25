package com.smiledev.rafiq_quran.domain.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import kotlinx.coroutines.flow.Flow

data class HifzGoal(
    val id: Long = 0,
    val suraNumber: Int,
    val suraName: String,
    val ayaStart: Int,
    val ayaEnd: Int,
    val memorizedAyahs: Set<Int> = emptySet()
) {
    val totalAyahs: Int
        get() = (ayaEnd - ayaStart + 1).coerceAtLeast(0)

    val memorizedCount: Int
        get() = memorizedAyahs.count { it in ayaStart..ayaEnd }

    val progressPercent: Int
        get() = if (totalAyahs == 0) 0 else (memorizedCount * 100) / totalAyahs

    val isComplete: Boolean
        get() = totalAyahs > 0 && memorizedCount >= totalAyahs
}

interface HifzRepository {
    fun observeAll(): Flow<List<HifzGoal>>
    suspend fun upsert(goal: HifzGoal): Result<Unit, AppError>
    suspend fun delete(id: Long): Result<Unit, AppError>
}
