package com.smiledev.rafiq_quran.domain.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.IslamicEvent

interface IslamicCalendarRepository {
    val islamicMonthNames: List<String>
    val islamicMonthNamesId: List<String>
    fun getEvents(): Result<List<IslamicEvent>, AppError>
    fun getEventsForMonth(month: Int): Result<List<IslamicEvent>, AppError>
    fun getTodayEvents(): Result<List<IslamicEvent>, AppError>
}
