package com.smiledev.rafiq_quran.ui.prayerlog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.smiledev.rafiq_quran.domain.repository.PrayerLogDay
import com.smiledev.rafiq_quran.domain.util.HijriDateConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private val DAY_OF_WEEK_NAMES = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

@Immutable
data class PrayerItemState(
    val key: String,
    val nameEn: String,
    val nameId: String,
    val nameArabic: String,
    val isCompleted: Boolean,
    val isObligatory: Boolean = true
)

@Immutable
data class SharedDaySummary(
    val date: String,
    val dayName: String,
    val dayNumber: String,
    val isToday: Boolean,
    val isSelected: Boolean,
    val completedCount: Int,
    val totalCount: Int = 5,
    val isAllCompleted: Boolean,
    val log: PrayerLogDay? = null
)

@Immutable
data class SharedPrayerLogUiState(
    val todayDate: String = "2026-10-28",
    val selectedDate: String = "2026-10-28",
    val logs: Map<String, PrayerLogDay> = emptyMap(),
    val sunnahLogs: Map<String, Map<String, Boolean>> = emptyMap(),
    val currentStreak: Int = 0,
    val weeklyCompletedCount: Int = 0,
    val weeklyTotalCount: Int = 35,
    val weeklyPercentage: Int = 0,
    val recentDays: List<SharedDaySummary> = emptyList(),
    val historyLogs: List<PrayerLogDay> = emptyList(),
    val isLoading: Boolean = false
) {
    val selectedLog: PrayerLogDay
        get() = logs[selectedDate] ?: PrayerLogDay(date = selectedDate)

    val selectedSunnah: Map<String, Boolean>
        get() = sunnahLogs[selectedDate] ?: emptyMap()

    val obligatoryPrayers: List<PrayerItemState>
        get() {
            val log = selectedLog
            return listOf(
                PrayerItemState("fajr", "Fajr", "Subuh", "صَلَاةُ الفَجْر", log.fajr),
                PrayerItemState("dhuhr", "Dhuhr", "Dzuhur", "صَلَاةُ الظُّهْر", log.dhuhr),
                PrayerItemState("asr", "Asr", "Ashar", "صَلَاةُ العَصْر", log.asr),
                PrayerItemState("maghrib", "Maghrib", "Maghrib", "صَلَاةُ المَغْرِب", log.maghrib),
                PrayerItemState("isha", "Isha", "Isya", "صَلَاةُ العِشَاء", log.isha)
            )
        }

    val sunnahPrayers: List<PrayerItemState>
        get() {
            val sunnah = selectedSunnah
            return listOf(
                PrayerItemState("tahajjud", "Tahajjud / Qiyam", "Tahajjud / Qiyamul Lail", "صَلَاةُ التَّهَجُّد", sunnah["tahajjud"] ?: false, false),
                PrayerItemState("duha", "Duha Prayer", "Shalat Dhuha", "صَلَاةُ الضُّحَى", sunnah["duha"] ?: false, false),
                PrayerItemState("rawatib", "Rawatib Sunnah", "Sunnah Rawatib", "سُنَنُ الرَّوَاتِب", sunnah["rawatib"] ?: false, false),
                PrayerItemState("witr", "Witr Prayer", "Shalat Witir", "صَلَاةُ الوِتْر", sunnah["witr"] ?: false, false)
            )
        }
}

val DEFAULT_INITIAL_PRAYER_LOGS = listOf(
    PrayerLogDay(date = "2026-10-28", fajr = true, dhuhr = true, asr = true, maghrib = false, isha = false),
    PrayerLogDay(date = "2026-10-27", fajr = true, dhuhr = true, asr = true, maghrib = true, isha = true),
    PrayerLogDay(date = "2026-10-26", fajr = true, dhuhr = true, asr = true, maghrib = true, isha = true),
    PrayerLogDay(date = "2026-10-25", fajr = true, dhuhr = true, asr = true, maghrib = true, isha = true),
    PrayerLogDay(date = "2026-10-24", fajr = true, dhuhr = true, asr = true, maghrib = true, isha = false),
    PrayerLogDay(date = "2026-10-23", fajr = true, dhuhr = true, asr = true, maghrib = true, isha = true),
    PrayerLogDay(date = "2026-10-22", fajr = true, dhuhr = true, asr = true, maghrib = true, isha = true)
)

class SharedPrayerLogViewModel(
    initialTodayDate: String = "2026-10-28",
    initialLogs: List<PrayerLogDay> = DEFAULT_INITIAL_PRAYER_LOGS
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SharedPrayerLogUiState(
            todayDate = initialTodayDate,
            selectedDate = initialTodayDate,
            logs = initialLogs.associateBy { it.date },
            sunnahLogs = mapOf(
                initialTodayDate to mapOf("duha" to true, "rawatib" to true)
            )
        )
    )
    val uiState: StateFlow<SharedPrayerLogUiState> = _uiState.asStateFlow()

    init {
        refreshCalculatedState()
    }

    fun selectDate(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
        refreshCalculatedState()
    }

    fun jumpToToday() {
        val today = _uiState.value.todayDate
        selectDate(today)
    }

    fun previousDay() {
        val current = _uiState.value.selectedDate
        val prevDate = offsetDateString(current, -1)
        selectDate(prevDate)
    }

    fun nextDay() {
        val current = _uiState.value.selectedDate
        val today = _uiState.value.todayDate
        if (current >= today) return
        val nextDate = offsetDateString(current, 1)
        selectDate(nextDate)
    }

    fun togglePrayer(prayerKey: String, isCompleted: Boolean, targetDate: String? = null) {
        val date = targetDate ?: _uiState.value.selectedDate
        _uiState.update { state ->
            val current = state.logs[date] ?: PrayerLogDay(date = date)
            val updated = when (prayerKey.lowercase()) {
                "fajr" -> current.copy(fajr = isCompleted)
                "dhuhr" -> current.copy(dhuhr = isCompleted)
                "asr" -> current.copy(asr = isCompleted)
                "maghrib" -> current.copy(maghrib = isCompleted)
                "isha" -> current.copy(isha = isCompleted)
                else -> current
            }
            val newLogs = state.logs + (date to updated)
            state.copy(logs = newLogs)
        }
        refreshCalculatedState()
    }

    fun toggleSunnah(prayerKey: String, isCompleted: Boolean, targetDate: String? = null) {
        val date = targetDate ?: _uiState.value.selectedDate
        _uiState.update { state ->
            val daySunnah = state.sunnahLogs[date]?.toMutableMap() ?: mutableMapOf()
            daySunnah[prayerKey.lowercase()] = isCompleted
            val newSunnahLogs = state.sunnahLogs + (date to daySunnah.toMap())
            state.copy(sunnahLogs = newSunnahLogs)
        }
    }

    fun markAllObligatory(isCompleted: Boolean, targetDate: String? = null) {
        val date = targetDate ?: _uiState.value.selectedDate
        _uiState.update { state ->
            val current = state.logs[date] ?: PrayerLogDay(date = date)
            val updated = current.copy(
                fajr = isCompleted,
                dhuhr = isCompleted,
                asr = isCompleted,
                maghrib = isCompleted,
                isha = isCompleted
            )
            state.copy(logs = state.logs + (date to updated))
        }
        refreshCalculatedState()
    }

    private fun refreshCalculatedState() {
        _uiState.update { state ->
            val today = state.todayDate
            val currentSelected = state.selectedDate
            val logs = state.logs

            val streak = calculateStreak(today, logs)
            val (weeklyCount, weeklyTotal) = calculateWeeklyStats(today, logs)
            val weeklyPct = if (weeklyTotal > 0) (weeklyCount * 100) / weeklyTotal else 0
            val recentDays = buildRecentDays(today, currentSelected, logs)
            val sortedLogs = logs.values.sortedByDescending { it.date }

            state.copy(
                currentStreak = streak,
                weeklyCompletedCount = weeklyCount,
                weeklyTotalCount = weeklyTotal,
                weeklyPercentage = weeklyPct,
                recentDays = recentDays,
                historyLogs = sortedLogs
            )
        }
    }

    internal fun calculateStreak(todayStr: String, logsByDate: Map<String, PrayerLogDay>): Int {
        val todayLog = logsByDate[todayStr]
        val isTodayAllCompleted = todayLog?.isAllCompleted == true

        var streak = 0
        var checkDate = if (isTodayAllCompleted) {
            streak = 1
            offsetDateString(todayStr, -1)
        } else {
            offsetDateString(todayStr, -1)
        }

        while (true) {
            val log = logsByDate[checkDate]
            if (log != null && log.isAllCompleted) {
                streak++
                checkDate = offsetDateString(checkDate, -1)
            } else {
                break
            }
        }
        return streak
    }

    internal fun calculateWeeklyStats(todayStr: String, logsByDate: Map<String, PrayerLogDay>): Pair<Int, Int> {
        var completedCount = 0
        var curr = todayStr
        for (i in 0 until 7) {
            val log = logsByDate[curr]
            completedCount += log?.completedCount ?: 0
            curr = offsetDateString(curr, -1)
        }
        return Pair(completedCount, 35)
    }

    private fun buildRecentDays(
        todayStr: String,
        selectedDate: String,
        logsByDate: Map<String, PrayerLogDay>
    ): List<SharedDaySummary> {
        val list = mutableListOf<SharedDaySummary>()
        // 6 days before today up to today
        var startDate = offsetDateString(todayStr, -6)
        for (i in 0 until 7) {
            val log = logsByDate[startDate]
            val completed = log?.completedCount ?: 0
            val (year, month, day) = parseDate(startDate)
            val weekday = HijriDateConverter.weekdayOf(year, month, day)
            val dayName = DAY_OF_WEEK_NAMES.getOrElse(weekday) { "" }

            list.add(
                SharedDaySummary(
                    date = startDate,
                    dayName = dayName,
                    dayNumber = day.toString(),
                    isToday = startDate == todayStr,
                    isSelected = startDate == selectedDate,
                    completedCount = completed,
                    isAllCompleted = log?.isAllCompleted == true,
                    log = log
                )
            )
            startDate = offsetDateString(startDate, 1)
        }
        return list
    }

    companion object {
        fun formatDate(year: Int, month: Int, day: Int): String {
            val m = if (month < 10) "0$month" else "$month"
            val d = if (day < 10) "0$day" else "$day"
            return "$year-$m-$d"
        }

        fun parseDate(dateStr: String): Triple<Int, Int, Int> {
            val parts = dateStr.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
            val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
            return Triple(year, month, day)
        }

        fun offsetDateString(dateStr: String, daysOffset: Int): String {
            val (year, month, day) = parseDate(dateStr)
            val (ny, nm, nd) = offsetDate(year, month, day, daysOffset)
            return formatDate(ny, nm, nd)
        }

        fun offsetDate(year: Int, month: Int, day: Int, daysOffset: Int): Triple<Int, Int, Int> {
            var y = year
            var m = month
            var d = day + daysOffset

            if (daysOffset > 0) {
                while (true) {
                    val daysInM = HijriDateConverter.daysInGregorianMonth(y, m)
                    if (d <= daysInM) break
                    d -= daysInM
                    m++
                    if (m > 12) {
                        m = 1
                        y++
                    }
                }
            } else if (daysOffset < 0) {
                while (d < 1) {
                    m--
                    if (m < 1) {
                        m = 12
                        y--
                    }
                    val daysInM = HijriDateConverter.daysInGregorianMonth(y, m)
                    d += daysInM
                }
            }

            return Triple(y, m, d)
        }
    }
}
