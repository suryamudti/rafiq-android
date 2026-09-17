package com.smiledev.rafiq_quran.ui.prayerlog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.repository.PrayerLogDay
import com.smiledev.rafiq_quran.domain.repository.PrayerLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Immutable
data class DaySummary(
    val date: String,
    val dayName: String,
    val dayNumber: String,
    val isToday: Boolean,
    val isSelected: Boolean,
    val completedCount: Int,
    val isAllCompleted: Boolean,
    val log: PrayerLogDay? = null
)

@Immutable
data class PrayerLogUiState(
    val logs: List<PrayerLogDay> = emptyList(),
    val todayDate: String = "",
    val todayLog: PrayerLogDay? = null,
    val selectedDate: String = "",
    val selectedLog: PrayerLogDay? = null,
    val currentStreak: Int = 0,
    val weeklyCompletedCount: Int = 0,
    val weeklyTotalCount: Int = 35,
    val weeklyPercentage: Int = 0,
    val recentDays: List<DaySummary> = emptyList(),
    val historyLogs: List<PrayerLogDay> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class PrayerLogViewModel @Inject constructor(
    private val prayerLogRepository: PrayerLogRepository,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerLogUiState())
    val uiState: StateFlow<PrayerLogUiState> = _uiState

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
        val today = dateFormat.format(Date())
        _uiState.value = _uiState.value.copy(
            todayDate = today,
            selectedDate = today
        )
        observeLogs()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            prayerLogRepository.observeAll().collect { logs ->
                val today = _uiState.value.todayDate.ifEmpty { dateFormat.format(Date()) }
                val currentSelected = _uiState.value.selectedDate.ifEmpty { today }
                val logsByDate = logs.associateBy { it.date }
                val todayLog = logsByDate[today]
                val selectedLog = logsByDate[currentSelected]
                val streak = calculateStreak(today, logsByDate)
                val (weeklyCount, weeklyTotal) = calculateWeeklyStats(today, logsByDate)
                val weeklyPct = if (weeklyTotal > 0) (weeklyCount * 100) / weeklyTotal else 0
                val recentDays = buildRecentDays(today, currentSelected, logsByDate)
                val sortedLogs = logs.sortedByDescending { it.date }

                _uiState.value = _uiState.value.copy(
                    logs = logs,
                    todayDate = today,
                    todayLog = todayLog,
                    selectedDate = currentSelected,
                    selectedLog = selectedLog,
                    currentStreak = streak,
                    weeklyCompletedCount = weeklyCount,
                    weeklyTotalCount = weeklyTotal,
                    weeklyPercentage = weeklyPct,
                    recentDays = recentDays,
                    historyLogs = sortedLogs,
                    isLoading = false
                )
            }
        }
    }

    fun refresh() {
        observeLogs()
    }

    fun selectDate(date: String) {
        val logsByDate = _uiState.value.logs.associateBy { it.date }
        val today = _uiState.value.todayDate
        val selectedLog = logsByDate[date]
        val recentDays = buildRecentDays(today, date, logsByDate)
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            selectedLog = selectedLog,
            recentDays = recentDays
        )
    }

    fun previousDay() {
        val current = _uiState.value.selectedDate.ifEmpty { _uiState.value.todayDate }
        val cal = Calendar.getInstance()
        try {
            val parsed = dateFormat.parse(current)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {
            cal.time = Date()
        }
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val prevDate = dateFormat.format(cal.time)
        selectDate(prevDate)
    }

    fun nextDay() {
        val current = _uiState.value.selectedDate.ifEmpty { _uiState.value.todayDate }
        val today = _uiState.value.todayDate
        if (current >= today) return
        val cal = Calendar.getInstance()
        try {
            val parsed = dateFormat.parse(current)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {
            cal.time = Date()
        }
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val nextDate = dateFormat.format(cal.time)
        if (nextDate <= today) {
            selectDate(nextDate)
        }
    }

    fun jumpToToday() {
        selectDate(_uiState.value.todayDate)
    }

    fun togglePrayer(prayer: String, value: Boolean, targetDate: String? = null) {
        val date = targetDate ?: _uiState.value.selectedDate.ifEmpty { _uiState.value.todayDate }
        val current = _uiState.value.logs.find { it.date == date } ?: PrayerLogDay(date = date)
        viewModelScope.launch(dispatcherProvider.io) {
            val updated = when (prayer.lowercase(Locale.US)) {
                "fajr" -> current.copy(fajr = value)
                "dhuhr" -> current.copy(dhuhr = value)
                "asr" -> current.copy(asr = value)
                "maghrib" -> current.copy(maghrib = value)
                "isha" -> current.copy(isha = value)
                else -> current
            }
            prayerLogRepository.upsert(updated)
        }
    }

    fun markAllPrayers(value: Boolean, targetDate: String? = null) {
        val date = targetDate ?: _uiState.value.selectedDate.ifEmpty { _uiState.value.todayDate }
        val current = _uiState.value.logs.find { it.date == date } ?: PrayerLogDay(date = date)
        viewModelScope.launch(dispatcherProvider.io) {
            val updated = current.copy(
                fajr = value,
                dhuhr = value,
                asr = value,
                maghrib = value,
                isha = value
            )
            prayerLogRepository.upsert(updated)
        }
    }

    internal fun calculateStreak(todayStr: String, logsByDate: Map<String, PrayerLogDay>): Int {
        val cal = Calendar.getInstance()
        try {
            val parsed = dateFormat.parse(todayStr)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {
            cal.time = Date()
        }

        val todayLog = logsByDate[todayStr]
        val isTodayAllCompleted = todayLog?.isAllCompleted == true

        var streak = 0
        if (isTodayAllCompleted) {
            streak = 1
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            // Check consecutive days ending yesterday
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (true) {
            val dateKey = dateFormat.format(cal.time)
            val log = logsByDate[dateKey]
            if (log != null && log.isAllCompleted) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    internal fun calculateWeeklyStats(todayStr: String, logsByDate: Map<String, PrayerLogDay>): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        try {
            val parsed = dateFormat.parse(todayStr)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {
            cal.time = Date()
        }

        var completedCount = 0
        for (i in 0 until 7) {
            val dateKey = dateFormat.format(cal.time)
            val log = logsByDate[dateKey]
            completedCount += log?.completedCount ?: 0
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return Pair(completedCount, 35)
    }

    private fun buildRecentDays(
        todayStr: String,
        selectedDate: String,
        logsByDate: Map<String, PrayerLogDay>
    ): List<DaySummary> {
        val list = mutableListOf<DaySummary>()
        val cal = Calendar.getInstance()
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault())

        try {
            val parsed = dateFormat.parse(todayStr)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {
            cal.time = Date()
        }

        // Start 6 days before today
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0 until 7) {
            val dateKey = dateFormat.format(cal.time)
            val log = logsByDate[dateKey]
            val count = log?.completedCount ?: 0
            list.add(
                DaySummary(
                    date = dateKey,
                    dayName = dayNameFormat.format(cal.time),
                    dayNumber = dayNumberFormat.format(cal.time),
                    isToday = dateKey == todayStr,
                    isSelected = dateKey == selectedDate,
                    completedCount = count,
                    isAllCompleted = count == 5,
                    log = log
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }
}

