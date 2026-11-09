package com.smiledev.rafiq_quran.ui.prayertimes

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class SharedPrayerEntry(
    val key: String,
    val nameEn: String,
    val nameId: String,
    val nameArabic: String,
    val time: String,
    val isNext: Boolean = false,
    val isPassed: Boolean = false,
    val isCurrent: Boolean = false
)

@Immutable
data class SharedPrayerTimesUiState(
    val selectedDate: String = "2026-10-28",
    val displayDate: String = "Wednesday, October 28, 2026",
    val hijriDate: String = "17 Rabi' al-Thani 1448 H",
    val cityName: String = "Jakarta, Indonesia",
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456,
    val calculationMethodName: String = "KEMENAG (Ministry of Religious Affairs ID)",
    val prayerNotificationsEnabled: Boolean = true,
    val prayers: List<SharedPrayerEntry> = emptyList(),
    val currentPrayerName: String = "Dhuhr",
    val nextPrayerName: String = "Asr",
    val nextPrayerTime: String = "15:02",
    val countdown: String = "01:45:20",
    val isToday: Boolean = true,
    val isLoading: Boolean = false
)

val DEFAULT_PRAYER_ENTRIES = listOf(
    SharedPrayerEntry(
        key = "imsak",
        nameEn = "Imsak",
        nameId = "Imsak",
        nameArabic = "الإمساك",
        time = "04:05",
        isPassed = true
    ),
    SharedPrayerEntry(
        key = "fajr",
        nameEn = "Fajr",
        nameId = "Subuh",
        nameArabic = "صلاة الفجر",
        time = "04:18",
        isPassed = true
    ),
    SharedPrayerEntry(
        key = "sunrise",
        nameEn = "Sunrise",
        nameId = "Terbit",
        nameArabic = "الشروق",
        time = "05:32",
        isPassed = true
    ),
    SharedPrayerEntry(
        key = "dhuhr",
        nameEn = "Dhuhr",
        nameId = "Dzuhur",
        nameArabic = "صلاة الظهر",
        time = "11:46",
        isPassed = false,
        isCurrent = true
    ),
    SharedPrayerEntry(
        key = "asr",
        nameEn = "Asr",
        nameId = "Ashar",
        nameArabic = "صلاة العصر",
        time = "15:02",
        isNext = true,
        isPassed = false
    ),
    SharedPrayerEntry(
        key = "maghrib",
        nameEn = "Maghrib",
        nameId = "Maghrib",
        nameArabic = "صلاة المغرب",
        time = "17:54",
        isPassed = false
    ),
    SharedPrayerEntry(
        key = "isha",
        nameEn = "Isha",
        nameId = "Isya",
        nameArabic = "صلاة العشاء",
        time = "19:05",
        isPassed = false
    )
)

class SharedPrayerTimesViewModel(
    initialDate: String = "2026-10-28",
    initialCity: String = "Jakarta, Indonesia",
    initialLatitude: Double = -6.2088,
    initialLongitude: Double = 106.8456,
    initialPrayers: List<SharedPrayerEntry> = DEFAULT_PRAYER_ENTRIES
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SharedPrayerTimesUiState(
            selectedDate = initialDate,
            cityName = initialCity,
            latitude = initialLatitude,
            longitude = initialLongitude,
            prayers = initialPrayers
        )
    )
    val uiState: StateFlow<SharedPrayerTimesUiState> = _uiState.asStateFlow()

    fun toggleNotifications() {
        _uiState.update { it.copy(prayerNotificationsEnabled = !it.prayerNotificationsEnabled) }
    }

    fun goToNextDay() {
        _uiState.update { state ->
            val nextDate = shiftDateString(state.selectedDate, days = 1)
            state.copy(
                selectedDate = nextDate,
                displayDate = formatDisplayDate(nextDate),
                isToday = false
            )
        }
    }

    fun goToPreviousDay() {
        _uiState.update { state ->
            val prevDate = shiftDateString(state.selectedDate, days = -1)
            state.copy(
                selectedDate = prevDate,
                displayDate = formatDisplayDate(prevDate),
                isToday = false
            )
        }
    }

    fun goToToday() {
        _uiState.update { state ->
            state.copy(
                selectedDate = "2026-10-28",
                displayDate = "Wednesday, October 28, 2026",
                isToday = true
            )
        }
    }

    fun setCalculationMethod(methodName: String) {
        _uiState.update { it.copy(calculationMethodName = methodName) }
    }

    fun updateLocation(cityName: String, latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(
                cityName = cityName,
                latitude = latitude,
                longitude = longitude
            )
        }
    }

    private fun shiftDateString(dateStr: String, days: Int): String {
        val parts = dateStr.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return dateStr
        var (year, month, day) = parts
        day += days
        if (day < 1) {
            day = 28
            month -= 1
            if (month < 1) {
                month = 12
                year -= 1
            }
        } else if (day > 28) {
            day = 1
            month += 1
            if (month > 12) {
                month = 1
                year += 1
            }
        }
        val monthStr = if (month < 10) "0$month" else "$month"
        val dayStr = if (day < 10) "0$day" else "$day"
        return "$year-$monthStr-$dayStr"
    }

    private fun formatDisplayDate(dateStr: String): String {
        val parts = dateStr.split("-")
        if (parts.size != 3) return dateStr
        val year = parts[0]
        val monthNum = parts[1].toIntOrNull() ?: 10
        val day = parts[2].toIntOrNull() ?: 28
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val monthName = months.getOrElse(monthNum - 1) { "October" }
        return "$day $monthName $year"
    }
}
