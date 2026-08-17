package com.smiledev.rafiq.ui.prayertimes

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.repository.PrayerTimesRepository
import dagger.hilt.android.lifecycle.HiltViewModel

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

@Immutable
data class PrayerTimeEntry(
    val name: String,
    val time: String
)

@Immutable
data class PrayerTimesUiState(
    val prayerTimes: List<PrayerTimeEntry> = emptyList(),
    val currentDate: Date = Date(),
    val hijriDate: String = "",
    val currentPrayer: String = "",
    val currentPrayerTime: String = "",
    val countdown: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456,
    val calculationMethod: Int = 20
)

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerTimesUiState())
    val uiState: StateFlow<PrayerTimesUiState> = _uiState

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val displayDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            combine(
                preferencesManager.latitude,
                preferencesManager.longitude,
                preferencesManager.prayerCalculationMethod
            ) { latStr, lonStr, method ->
                val lat = latStr.toDoubleOrNull() ?: -6.2088
                val lon = lonStr.toDoubleOrNull() ?: 106.8456
                val calcMethod = if (method == 2) 20 else method
                _uiState.value = _uiState.value.copy(
                    latitude = lat,
                    longitude = lon,
                    calculationMethod = calcMethod
                )
            }.collect {
                loadPrayerTimes()
            }
        }
    }

    fun loadPrayerTimes() {
        val state = _uiState.value
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = prayerTimesRepository.fetchPrayerTimes(
                state.latitude, state.longitude,
                dateFormat.format(state.currentDate), state.calculationMethod
            )
            when (result) {
                is Result.Success -> {
                    val data = result.data
                    val times = listOf(
                        PrayerTimeEntry("Imsak", data.timings.imsak),
                        PrayerTimeEntry("Fajr (Subuh)", data.timings.fajr),
                        PrayerTimeEntry("Sunrise", data.timings.sunrise),
                        PrayerTimeEntry("Dhuha", data.timings.sunrise),
                        PrayerTimeEntry("Dzuhur", data.timings.dhuhr),
                        PrayerTimeEntry("Asr", data.timings.asr),
                        PrayerTimeEntry("Maghrib", data.timings.maghrib),
                        PrayerTimeEntry("Isya", data.timings.isha)
                    )
                    _uiState.value = _uiState.value.copy(
                        prayerTimes = times,
                        hijriDate = data.hijriDate ?: "",
                        isLoading = false
                    )
                    startCountdown(times)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    private fun startCountdown(times: List<PrayerTimeEntry>) {
        viewModelScope.launch(dispatcherProvider.io) {
            while (isActive) {
                val now = Calendar.getInstance()
                val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

                var nextPrayer: PrayerTimeEntry? = null
                for (pt in times.drop(1)) {
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

    fun refresh() { loadPrayerTimes() }

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

    val displayDate: String
        get() = displayDateFormat.format(_uiState.value.currentDate)
}
