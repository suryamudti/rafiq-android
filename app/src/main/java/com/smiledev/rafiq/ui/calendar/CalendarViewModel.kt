package com.smiledev.rafiq.ui.calendar

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.currentLocaleCode
import com.smiledev.rafiq.core.getOrNull
import com.smiledev.rafiq.domain.model.GregorianDate
import com.smiledev.rafiq.domain.model.HijriDate
import com.smiledev.rafiq.domain.model.IslamicEvent
import com.smiledev.rafiq.domain.repository.IslamicCalendarRepository
import com.smiledev.rafiq.domain.util.HijriDateConverter
import com.smiledev.rafiq.domain.util.SystemTodayProvider
import com.smiledev.rafiq.domain.util.TodayProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@Immutable
data class CalendarDay(
    val gregorianDay: Int,
    val hijriDate: HijriDate,
    val events: List<IslamicEvent>,
    val isToday: Boolean
)

@Immutable
data class CalendarUiState(
    val todayEvents: List<IslamicEvent> = emptyList(),
    val displayedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val displayedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val grid: List<CalendarDay?> = emptyList(),
    val selectedIndex: Int? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: IslamicCalendarRepository,
    private val todayProvider: TodayProvider = SystemTodayProvider,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState

    val localeCode = currentLocaleCode()

    val monthNames: List<String>
        get() = if (localeCode == "id") repository.islamicMonthNamesId else repository.islamicMonthNames

    init { load() }

    private fun load() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val today = todayProvider.today()
            val todayResult = repository.getTodayEvents()
            val eventsResult = repository.getEvents()
            val todayEvents = if (todayResult is Result.Success) todayResult.data else emptyList()
            val events = if (eventsResult is Result.Success) eventsResult.data else emptyList()
            _uiState.value = _uiState.value.copy(
                todayEvents = todayEvents,
                displayedYear = today.year,
                displayedMonth = today.month,
                grid = buildGrid(today.year, today.month, events, today),
                isLoading = false,
                error = when {
                    todayResult is Result.Error -> todayResult.error
                    eventsResult is Result.Error -> eventsResult.error
                    else -> null
                }
            )
        }
    }

    fun nextMonth() {
        val s = _uiState.value
        val newMonth = if (s.displayedMonth == 12) 1 else s.displayedMonth + 1
        val newYear = if (s.displayedMonth == 12) s.displayedYear + 1 else s.displayedYear
        _uiState.value = s.copy(displayedYear = newYear, displayedMonth = newMonth, selectedIndex = null)
        refreshGrid()
    }

    fun previousMonth() {
        val s = _uiState.value
        val newMonth = if (s.displayedMonth == 1) 12 else s.displayedMonth - 1
        val newYear = if (s.displayedMonth == 1) s.displayedYear - 1 else s.displayedYear
        _uiState.value = s.copy(displayedYear = newYear, displayedMonth = newMonth, selectedIndex = null)
        refreshGrid()
    }

    fun goToToday() {
        val today = todayProvider.today()
        _uiState.value = _uiState.value.copy(
            displayedYear = today.year,
            displayedMonth = today.month,
            selectedIndex = null
        )
        refreshGrid()
    }

    fun onDayClick(index: Int) {
        _uiState.value = _uiState.value.copy(selectedIndex = index)
    }

    fun dismissDaySheet() {
        _uiState.value = _uiState.value.copy(selectedIndex = null)
    }

    fun getMonthName(month: Int, indonesian: Boolean = localeCode == "id"): String =
        if (indonesian) repository.islamicMonthNamesId.getOrElse(month - 1) { "" }
        else repository.islamicMonthNames.getOrElse(month - 1) { "" }

    private fun refreshGrid() {
        viewModelScope.launch(dispatcherProvider.io) {
            val s = _uiState.value
            val events = repository.getEvents().getOrNull() ?: emptyList()
            val today = todayProvider.today()
            _uiState.value = s.copy(grid = buildGrid(s.displayedYear, s.displayedMonth, events, today))
        }
    }

    private fun buildGrid(
        year: Int,
        month: Int,
        events: List<IslamicEvent>,
        today: GregorianDate
    ): List<CalendarDay?> {
        val daysInMonth = HijriDateConverter.daysInGregorianMonth(year, month)
        val leadingBlanks = HijriDateConverter.weekdayOf(year, month, 1)
        val grid = MutableList<CalendarDay?>(42) { null }
        for (day in 1..daysInMonth) {
            val hijri = HijriDateConverter.gregorianToHijri(year, month, day)
            grid[leadingBlanks + day - 1] = CalendarDay(
                gregorianDay = day,
                hijriDate = hijri,
                events = events.filter { it.hijriMonth == hijri.month && it.hijriDay == hijri.day },
                isToday = today.year == year && today.month == month && today.day == day
            )
        }
        return grid
    }
}
