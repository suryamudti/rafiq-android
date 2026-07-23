package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.IslamicEvent

interface IslamicCalendarRepository {
    val islamicMonthNames: List<String>
    val islamicMonthNamesId: List<String>
    fun getEvents(): Result<List<IslamicEvent>, AppError>
    fun getEventsForMonth(month: Int): Result<List<IslamicEvent>, AppError>
    fun getTodayEvents(): Result<List<IslamicEvent>, AppError>
}
