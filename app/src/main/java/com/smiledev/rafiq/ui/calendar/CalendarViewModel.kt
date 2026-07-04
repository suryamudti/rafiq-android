package com.smiledev.rafiq.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.data.models.IslamicEvent
import com.smiledev.rafiq.data.repository.IslamicCalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class CalendarUiState(
    val events: List<IslamicEvent> = emptyList(),
    val todayEvents: List<IslamicEvent> = emptyList(),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: IslamicCalendarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState

    val localeCode = if (Locale.getDefault().language == "id") "id" else "en"

    val monthNames: List<String>
        get() = if (localeCode == "id") repository.islamicMonthNamesId else repository.islamicMonthNames

    init { load() }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val todayEvents = repository.getTodayEvents()
                val events = repository.getEventsForMonth(_uiState.value.selectedMonth)
                _uiState.value = _uiState.value.copy(
                    events = events,
                    todayEvents = todayEvents,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun selectMonth(month: Int) {
        _uiState.value = _uiState.value.copy(selectedMonth = month)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val events = repository.getEventsForMonth(month)
                _uiState.value = _uiState.value.copy(events = events, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun getMonthName(month: Int, indonesian: Boolean = localeCode == "id"): String =
        if (indonesian) repository.islamicMonthNamesId.getOrElse(month - 1) { "" }
        else repository.islamicMonthNames.getOrElse(month - 1) { "" }
}
