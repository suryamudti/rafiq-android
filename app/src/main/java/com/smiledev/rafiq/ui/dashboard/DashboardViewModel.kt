package com.smiledev.rafiq.ui.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.PrayerTimeEntry
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
data class DashboardUiState(
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val nextPrayerName: String = "",
    val nextPrayerTime: String = "",
    val countdown: String = "",
    val greeting: String = "",
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456,
    val calculationMethod: Int = 20
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)

    init {
        _uiState.value = _uiState.value.copy(greeting = computeGreeting())
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
                dateFormat.format(Date()), state.calculationMethod
            )
            when (result) {
                is Result.Success -> {
                    val data = result.data
                    val times = listOf(
                        PrayerTimeEntry("Imsak", data.timings.imsak),
                        PrayerTimeEntry("Fajr (Subuh)", data.timings.fajr),
                        PrayerTimeEntry("Sunrise", data.timings.sunrise),
                        PrayerTimeEntry("Dzuhur", data.timings.dhuhr),
                        PrayerTimeEntry("Asr", data.timings.asr),
                        PrayerTimeEntry("Maghrib", data.timings.maghrib),
                        PrayerTimeEntry("Isya", data.timings.isha)
                    )
                    _uiState.value = _uiState.value.copy(isLoading = false)
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
                for (pt in times) {
                    val parts = pt.time.split(":")
                    if (parts.size == 2) {
                        val ptMinutes = parts[0].toIntOrNull()?.let {
                            it * 60 + (parts[1].toIntOrNull() ?: 0)
                        }
                        if (ptMinutes != null && ptMinutes > nowMinutes) {
                            nextPrayer = pt
                            break
                        }
                    }
                }
                if (nextPrayer == null) nextPrayer = times.first()

                val parts = nextPrayer.time.split(":")
                if (parts.size == 2) {
                    val targetMinutes = parts[0].toIntOrNull()
                        ?.let { it * 60 + (parts[1].toIntOrNull() ?: 0) } ?: 0
                    var diff = targetMinutes - nowMinutes
                    if (diff < 0) diff += 24 * 60
                    val hours = diff / 60
                    val mins = diff % 60
                    _uiState.value = _uiState.value.copy(
                        nextPrayerName = nextPrayer.name,
                        nextPrayerTime = nextPrayer.time,
                        countdown = "${hours}h ${mins}m"
                    )
                }
                delay(30_000)
            }
        }
    }

    fun refresh() {
        loadPrayerTimes()
    }

    private fun computeGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isIndonesian = Locale.getDefault().language == "id"
        return when (hour) {
            in 5..11 -> if (isIndonesian) "Selamat pagi" else "Good morning"
            in 12..16 -> if (isIndonesian) "Selamat siang" else "Good afternoon"
            in 17..20 -> if (isIndonesian) "Selamat sore" else "Good evening"
            else -> if (isIndonesian) "Selamat malam" else "Good night"
        }
    }
}
