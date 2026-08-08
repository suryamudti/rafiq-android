package com.smiledev.rafiq.domain.util

import com.smiledev.rafiq.domain.model.GregorianDate
import java.util.Calendar

fun interface TodayProvider {
    fun today(): GregorianDate
}

object SystemTodayProvider : TodayProvider {
    override fun today(): GregorianDate {
        val cal = Calendar.getInstance()
        return GregorianDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }
}
