package com.smiledev.rafiq.ui.calendar

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.IslamicEvent
import com.smiledev.rafiq.domain.repository.IslamicCalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import com.smiledev.rafiq.core.currentLocaleCode
import javax.inject.Inject

@Immutable
data class CalendarUiState(
    val events: List<IslamicEvent> = emptyList(),
    val todayEvents: List<IslamicEvent> = emptyList(),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val isLoading: Boolean = false,
    val error: AppError? = null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: IslamicCalendarRepository,
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
            val todayResult = repository.getTodayEvents()
            val eventsResult = repository.getEventsForMonth(_uiState.value.selectedMonth)
            val todayEvents = if (todayResult is Result.Success) todayResult.data else emptyList()
            val events = if (eventsResult is Result.Success) eventsResult.data else emptyList()
            _uiState.value = _uiState.value.copy(
                events = events,
                todayEvents = todayEvents,
                isLoading = false,
                error = when {
                    todayResult is Result.Error -> todayResult.error
                    eventsResult is Result.Error -> eventsResult.error
                    else -> null
                }
            )
        }
    }

    fun selectMonth(month: Int) {
        _uiState.value = _uiState.value.copy(selectedMonth = month)
        viewModelScope.launch(dispatcherProvider.io) {
            val result = repository.getEventsForMonth(month)
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(events = result.data, error = null)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.error)
                }
            }
        }
    }

    fun getMonthName(month: Int, indonesian: Boolean = localeCode == "id"): String =
        if (indonesian) repository.islamicMonthNamesId.getOrElse(month - 1) { "" }
        else repository.islamicMonthNames.getOrElse(month - 1) { "" }
}
