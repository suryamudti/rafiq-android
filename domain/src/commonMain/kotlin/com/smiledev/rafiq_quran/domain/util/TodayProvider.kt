package com.smiledev.rafiq_quran.domain.util

import com.smiledev.rafiq_quran.domain.model.GregorianDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

fun interface TodayProvider {
    fun today(): GregorianDate
}

object SystemTodayProvider : TodayProvider {
    override fun today(): GregorianDate {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return GregorianDate(today.year, today.monthNumber, today.dayOfMonth)
    }
}
