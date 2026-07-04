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
    val isLoading: Boolean = false
)

@HiltViewModel
class PrayerLogViewModel @Inject constructor(
    private val prayerLogDao: PrayerLogDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerLogUiState())
    val uiState: StateFlow<PrayerLogUiState> = _uiState

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init { load() }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val logs = prayerLogDao.getRecentLogs()
            _uiState.value = _uiState.value.copy(logs = logs, isLoading = false)
        }
    }

    fun togglePrayer(date: String, prayer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = prayerLogDao.getLogForDate(date)
            if (existing != null) {
                val updated = when (prayer) {
                    "Fajr" -> existing.copy(fajr = if (existing.fajr == 1) 0 else 1)
                    "Dhuhr" -> existing.copy(dhuhr = if (existing.dhuhr == 1) 0 else 1)
                    "Asr" -> existing.copy(asr = if (existing.asr == 1) 0 else 1)
                    "Maghrib" -> existing.copy(maghrib = if (existing.maghrib == 1) 0 else 1)
                    "Isha" -> existing.copy(isha = if (existing.isha == 1) 0 else 1)
                    else -> existing
                }
                prayerLogDao.insertOrUpdate(updated)
            } else {
                val newLog = PrayerLogEntity(
                    date = date,
                    fajr = if (prayer == "Fajr") 1 else 0,
                    dhuhr = if (prayer == "Dhuhr") 1 else 0,
                    asr = if (prayer == "Asr") 1 else 0,
                    maghrib = if (prayer == "Maghrib") 1 else 0,
                    isha = if (prayer == "Isha") 1 else 0
                )
                prayerLogDao.insertOrUpdate(newLog)
            }
            load()
        }
    }

    val today: String get() = dateFormat.format(Date())
}
