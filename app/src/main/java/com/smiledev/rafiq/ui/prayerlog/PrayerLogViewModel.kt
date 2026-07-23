package com.smiledev.rafiq.ui.prayerlog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.repository.PrayerLogDay
import com.smiledev.rafiq.domain.repository.PrayerLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Immutable
data class PrayerLogUiState(
    val logs: List<PrayerLogDay> = emptyList(),
    val todayDate: String = "",
    val todayLog: PrayerLogDay? = null,
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
        _uiState.value = _uiState.value.copy(todayDate = today)
        observeLogs()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            prayerLogRepository.observeAll().collect { logs ->
                val today = _uiState.value.todayDate
                val todayLog = logs.find { it.date == today }
                _uiState.value = _uiState.value.copy(
                    logs = logs,
                    todayLog = todayLog,
                    isLoading = false
                )
            }
        }
    }

    fun refresh() { observeLogs() }

    fun togglePrayer(prayer: String, value: Boolean) {
        val date = _uiState.value.todayDate
        val current = _uiState.value.todayLog ?: PrayerLogDay(date = date)
        viewModelScope.launch(dispatcherProvider.io) {
            val updated = when (prayer) {
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
}
