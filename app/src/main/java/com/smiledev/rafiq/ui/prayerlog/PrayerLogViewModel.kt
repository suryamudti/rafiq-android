package com.smiledev.rafiq.ui.prayerlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.data.local.PrayerLogDao
import com.smiledev.rafiq.data.local.PrayerLogEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class PrayerLogUiState(
    val logs: List<PrayerLogEntity> = emptyList(),
    val todayDate: String = "",
    val todayLog: PrayerLogEntity? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class PrayerLogViewModel @Inject constructor(
    private val prayerLogDao: PrayerLogDao
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
            prayerLogDao.getAllLogs().collect { logs ->
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

    fun togglePrayer(prayer: String, value: Boolean) {
        val date = _uiState.value.todayDate
        val current = _uiState.value.todayLog ?: PrayerLogEntity(date = date)
        viewModelScope.launch(Dispatchers.IO) {
            val updated = when (prayer) {
                "fajr" -> current.copy(fajr = value)
                "dhuhr" -> current.copy(dhuhr = value)
                "asr" -> current.copy(asr = value)
                "maghrib" -> current.copy(maghrib = value)
                "isha" -> current.copy(isha = value)
                else -> current
            }
            prayerLogDao.upsert(updated)
        }
    }
}
