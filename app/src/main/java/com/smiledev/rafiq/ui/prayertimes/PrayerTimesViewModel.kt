package com.smiledev.rafiq.ui.prayertimes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.data.repository.PrayerTimesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class PrayerTimeEntry(
    val name: String,
    val time: String
)

data class PrayerTimesUiState(
    val prayerTimes: List<PrayerTimeEntry> = emptyList(),
    val currentDate: Date = Date(),
    val hijriDate: String = "",
    val currentPrayer: String = "",
    val currentPrayerTime: String = "",
    val countdown: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456,
    val calculationMethod: Int = 20
)

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerTimesUiState())
    val uiState: StateFlow<PrayerTimesUiState> = _uiState

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val displayDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                preferencesManager.latitude,
                preferencesManager.longitude,
                preferencesManager.prayerCalculationMethod
            ) { latStr, lonStr, method ->
                val lat = latStr.toDoubleOrNull() ?: -6.2088
                val lon = lonStr.toDoubleOrNull() ?: 106.8456
                val calcMethod = if (method == 2) 20 else method // Map ISNA(2)→Karachi(20) default
                _uiState.value = _uiState.value.copy(
                    latitude = lat,
                    longitude = lon,
                    calculationMethod = calcMethod
                )
            }.collect { }
            loadPrayerTimes()
        }
    }

    fun loadPrayerTimes() {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val date = _uiState.value.currentDate
                val response = prayerTimesRepository.fetchPrayerTimes(
                    state.latitude, state.longitude,
                    dateFormat.format(date), state.calculationMethod
                )
                if (response.code == 200) {
                    val t = response.data.timings
                    val times = listOf(
                        PrayerTimeEntry("Imsak", t.Imsak),
                        PrayerTimeEntry("Fajr (Subuh)", t.Fajr),
                        PrayerTimeEntry("Sunrise", t.Sunrise),
                        PrayerTimeEntry("Dhuha", t.Imsak),
                        PrayerTimeEntry("Dzuhur", t.Dhuhr),
                        PrayerTimeEntry("Asr", t.Asr),
                        PrayerTimeEntry("Maghrib", t.Maghrib),
                        PrayerTimeEntry("Isya", t.Isha)
                    )
                    val hijri = extractHijriDate(response.data.date)
                    _uiState.value = _uiState.value.copy(
                        prayerTimes = times,
                        hijriDate = hijri,
                        isLoading = false
                    )
                    startCountdown(times)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "API error: ${response.status}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun startCountdown(times: List<PrayerTimeEntry>) {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val now = Calendar.getInstance()
                val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

                var nextPrayer: PrayerTimeEntry? = null
                for (pt in times.drop(1)) { // skip Imsak
                    val parts = pt.time.split(":")
                    if (parts.size == 2) {
                        val ptMinutes = parts[0].toIntOrNull()?.let { it * 60 + (parts[1].toIntOrNull() ?: 0) }
                        if (ptMinutes != null && ptMinutes > nowMinutes) {
                            nextPrayer = pt
                            break
                        }
                    }
                }
                if (nextPrayer == null) nextPrayer = times.first()

                val parts = nextPrayer.time.split(":")
                if (parts.size == 2) {
                    val targetMinutes = parts[0].toIntOrNull()?.let { it * 60 + (parts[1].toIntOrNull() ?: 0) } ?: 0
                    var diff = targetMinutes - nowMinutes
                    if (diff < 0) diff += 24 * 60
                    val hours = diff / 60
                    val mins = diff % 60
                    _uiState.value = _uiState.value.copy(
                        currentPrayer = nextPrayer.name,
                        currentPrayerTime = nextPrayer.time,
                        countdown = "${hours}h ${mins}m"
                    )
                }
                delay(30_000)
            }
        }
    }

    fun goToPreviousDay() {
        val cal = Calendar.getInstance()
        cal.time = _uiState.value.currentDate
        cal.add(Calendar.DAY_OF_MONTH, -1)
        _uiState.value = _uiState.value.copy(currentDate = cal.time)
        loadPrayerTimes()
    }

    fun goToNextDay() {
        val cal = Calendar.getInstance()
        cal.time = _uiState.value.currentDate
        cal.add(Calendar.DAY_OF_MONTH, 1)
        _uiState.value = _uiState.value.copy(currentDate = cal.time)
        loadPrayerTimes()
    }

    private fun extractHijriDate(dateMap: Map<String, Any>?): String {
        if (dateMap == null) return ""
        val hijri = dateMap["hijri"] as? Map<*, *> ?: return ""
        val day = hijri["day"] ?: ""
        val month = (hijri["month"] as? Map<*, *>)?.get("en") ?: ""
        val year = hijri["year"] ?: ""
        return "$day $month $year"
    }

    val displayDate: String
        get() = displayDateFormat.format(_uiState.value.currentDate)
}
