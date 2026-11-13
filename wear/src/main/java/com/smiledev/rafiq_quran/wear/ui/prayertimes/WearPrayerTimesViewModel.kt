package com.smiledev.rafiq_quran.wear.ui.prayertimes

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.PrayerTimesData
import com.smiledev.rafiq_quran.domain.repository.PrayerTimesRepository
import com.smiledev.rafiq_quran.wear.R
import com.smiledev.rafiq_quran.wear.location.WearLocationSync
import com.smiledev.rafiq_quran.wear.service.WearPrayerNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Immutable
data class WearPrayerTimeEntry(
    val name: String,
    val time: String,
    val prayerKey: String = "",
    @DrawableRes val iconResId: Int = 0,
    val isNext: Boolean = false,
    val isPassed: Boolean = false
)

@Immutable
data class WearPrayerTimesUiState(
    val prayerTimes: List<WearPrayerTimeEntry> = emptyList(),
    val currentDate: Date = Date(),
    val isToday: Boolean = true,
    val hijriDate: String = "",
    val nextPrayerName: String = "",
    val nextPrayerTime: String = "",
    val countdown: String = "",
    val isLoading: Boolean = false,
    val isSyncingLocation: Boolean = false,
    val error: AppError? = null,
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456,
    val calculationMethod: Int = 20,
    val calculationMethodName: String = "KEMENAG",
    val cityName: String = "Jakarta",
    val prayerNotificationsEnabled: Boolean = true
)

@HiltViewModel
class WearPrayerTimesViewModel @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val appContext: Context,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider,
    private val wearLocationSync: WearLocationSync? = null
) : ViewModel() {

    constructor(
        prayerTimesRepository: PrayerTimesRepository,
        preferencesManager: PreferencesManager,
        appContext: Context,
        dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
    ) : this(prayerTimesRepository, preferencesManager, appContext, dispatcherProvider, null)

    private val _uiState = MutableStateFlow(WearPrayerTimesUiState())
    val uiState: StateFlow<WearPrayerTimesUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    var enablePeriodicCountdown: Boolean = true
    private var lastLoadedKey: Triple<Double, Double, Int>? = null

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            kotlinx.coroutines.flow.combine(
                preferencesManager.latitude,
                preferencesManager.longitude,
                preferencesManager.prayerCalculationMethod,
                preferencesManager.cityName,
                preferencesManager.prayerNotificationsEnabled
            ) { latStr, lonStr, method, city, notifEnabled ->
                val lat = latStr.toDoubleOrNull() ?: -6.2088
                val lon = lonStr.toDoubleOrNull() ?: 106.8456
                val calcMethod = if (method == 2) 20 else method
                val methodName = getMethodDisplayName(calcMethod)
                val resolvedCity = if (city.isNotBlank()) city else "Jakarta"
                _uiState.value = _uiState.value.copy(
                    latitude = lat,
                    longitude = lon,
                    calculationMethod = calcMethod,
                    calculationMethodName = methodName,
                    cityName = resolvedCity,
                    prayerNotificationsEnabled = notifEnabled
                )
                Triple(lat, lon, calcMethod)
            }.collect { key ->
                if (key != lastLoadedKey) {
                    lastLoadedKey = key
                    loadPrayerTimes()
                }
            }
        }
    }

    fun refreshLocation(forceGeocode: Boolean = true) {
        val sync = wearLocationSync ?: return
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isSyncingLocation = true)
            sync.syncIfPermitted(forceGeocode = forceGeocode)
            _uiState.value = _uiState.value.copy(isSyncingLocation = false)
        }
    }

    fun loadPrayerTimes(forceRefresh: Boolean = false) {
        val state = _uiState.value
        viewModelScope.launch(dispatcherProvider.io) {
            val dateStr = dateFormat.format(_uiState.value.currentDate)
            if (!forceRefresh) {
                val cached = prayerTimesRepository.getCachedPrayerTimes(
                    state.latitude,
                    state.longitude,
                    dateStr,
                    state.calculationMethod
                )
                if (cached != null) {
                    applyPrayerTimes(cached)
                    return@launch
                }
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = prayerTimesRepository.fetchPrayerTimes(
                state.latitude,
                state.longitude,
                dateStr,
                state.calculationMethod,
                forceRefresh = forceRefresh
            )
            when (result) {
                is Result.Success -> {
                    applyPrayerTimes(result.data)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    private fun applyPrayerTimes(data: PrayerTimesData) {
        val isId = currentLocaleCode() == "id"
        val dhuhaTime = addMinutes(data.timings.sunrise, 20)
        val rawTimes = if (isId) {
            listOf(
                WearPrayerTimeEntry("Imsak", data.timings.imsak, prayerKey = "imsak", iconResId = R.drawable.ic_prayer_imsak),
                WearPrayerTimeEntry("Subuh", data.timings.fajr, prayerKey = "fajr", iconResId = R.drawable.ic_prayer_fajr),
                WearPrayerTimeEntry("Terbit", data.timings.sunrise, prayerKey = "sunrise", iconResId = R.drawable.ic_prayer_sunrise),
                WearPrayerTimeEntry("Dhuha", dhuhaTime, prayerKey = "dhuha", iconResId = R.drawable.ic_prayer_dhuha),
                WearPrayerTimeEntry("Dzuhur", data.timings.dhuhr, prayerKey = "dhuhr", iconResId = R.drawable.ic_prayer_dhuhr),
                WearPrayerTimeEntry("Ashar", data.timings.asr, prayerKey = "asr", iconResId = R.drawable.ic_prayer_asr),
                WearPrayerTimeEntry("Maghrib", data.timings.maghrib, prayerKey = "maghrib", iconResId = R.drawable.ic_prayer_maghrib),
                WearPrayerTimeEntry("Isya", data.timings.isha, prayerKey = "isha", iconResId = R.drawable.ic_prayer_isha)
            )
        } else {
            listOf(
                WearPrayerTimeEntry("Imsak", data.timings.imsak, prayerKey = "imsak", iconResId = R.drawable.ic_prayer_imsak),
                WearPrayerTimeEntry("Fajr", data.timings.fajr, prayerKey = "fajr", iconResId = R.drawable.ic_prayer_fajr),
                WearPrayerTimeEntry("Sunrise", data.timings.sunrise, prayerKey = "sunrise", iconResId = R.drawable.ic_prayer_sunrise),
                WearPrayerTimeEntry("Dhuha", dhuhaTime, prayerKey = "dhuha", iconResId = R.drawable.ic_prayer_dhuha),
                WearPrayerTimeEntry("Dhuhr", data.timings.dhuhr, prayerKey = "dhuhr", iconResId = R.drawable.ic_prayer_dhuhr),
                WearPrayerTimeEntry("Asr", data.timings.asr, prayerKey = "asr", iconResId = R.drawable.ic_prayer_asr),
                WearPrayerTimeEntry("Maghrib", data.timings.maghrib, prayerKey = "maghrib", iconResId = R.drawable.ic_prayer_maghrib),
                WearPrayerTimeEntry("Isha", data.timings.isha, prayerKey = "isha", iconResId = R.drawable.ic_prayer_isha)
            )
        }

        _uiState.value = _uiState.value.copy(
            prayerTimes = rawTimes,
            hijriDate = data.hijriDate ?: "",
            isLoading = false,
            error = null
        )

        if (enablePeriodicCountdown) {
            startCountdown()
        } else {
            updatePrayerStatusAndCountdown()
        }
        if (_uiState.value.prayerNotificationsEnabled) {
            WearPrayerNotificationWorker.scheduleNow(appContext)
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch(dispatcherProvider.io) {
            while (isActive) {
                updatePrayerStatusAndCountdown()
                delay(30_000)
            }
        }
    }

    internal fun updatePrayerStatusAndCountdown(now: Calendar = Calendar.getInstance()) {
        val state = _uiState.value
        val times = state.prayerTimes
        if (times.isEmpty()) return

        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val isToday = checkIfToday(state.currentDate, now)
        val todayStart = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentDayStart = Calendar.getInstance().apply {
            time = state.currentDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val isPastDay = currentDayStart.before(todayStart)

        var nextPrayer: WearPrayerTimeEntry? = null
        if (isToday) {
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
            if (nextPrayer == null) {
                nextPrayer = times.firstOrNull { it.prayerKey == "fajr" } ?: times.firstOrNull()
            }
        }

        var countdownStr = ""
        var nextPrayerName = ""
        var nextPrayerTime = ""

        if (isToday && nextPrayer != null) {
            nextPrayerName = nextPrayer.name
            nextPrayerTime = nextPrayer.time

            val parts = nextPrayer.time.split(":")
            if (parts.size == 2) {
                val targetMinutes = parts[0].toIntOrNull()?.let { it * 60 + (parts[1].toIntOrNull() ?: 0) } ?: 0
                var diff = targetMinutes - nowMinutes
                if (diff < 0) diff += 24 * 60
                val hours = diff / 60
                val mins = diff % 60
                countdownStr = "${hours}h ${mins}m"
            }
        }

        val updatedTimes = times.map { pt ->
            val parts = pt.time.split(":")
            val ptMinutes = if (parts.size == 2) {
                (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
            } else 0

            val passed = when {
                isPastDay -> true
                !isToday -> false
                else -> ptMinutes <= nowMinutes
            }

            val isNext = isToday && (nextPrayer != null && pt.name == nextPrayer.name && pt.time == nextPrayer.time)
            pt.copy(isPassed = passed, isNext = isNext)
        }

        _uiState.value = _uiState.value.copy(
            prayerTimes = updatedTimes,
            nextPrayerName = nextPrayerName,
            nextPrayerTime = nextPrayerTime,
            countdown = countdownStr,
            isToday = isToday
        )
    }

    fun refresh() {
        loadPrayerTimes(forceRefresh = true)
    }

    fun toggleNotifications() {
        viewModelScope.launch(dispatcherProvider.io) {
            val nextEnabled = !_uiState.value.prayerNotificationsEnabled
            preferencesManager.setPrayerNotificationsEnabled(nextEnabled)
            _uiState.value = _uiState.value.copy(prayerNotificationsEnabled = nextEnabled)
            if (nextEnabled) {
                WearPrayerNotificationWorker.scheduleNow(appContext)
            } else {
                WearPrayerNotificationWorker.cancelScheduledAlarms(appContext)
            }
        }
    }

    fun goToToday() {
        val today = Date()
        _uiState.value = _uiState.value.copy(currentDate = today, isToday = true)
        loadPrayerTimes()
    }

    fun goToPreviousDay() {
        val cal = Calendar.getInstance()
        cal.time = _uiState.value.currentDate
        cal.add(Calendar.DAY_OF_MONTH, -1)
        val newDate = cal.time
        _uiState.value = _uiState.value.copy(currentDate = newDate, isToday = checkIfToday(newDate))
        loadPrayerTimes()
    }

    fun goToNextDay() {
        val cal = Calendar.getInstance()
        cal.time = _uiState.value.currentDate
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val newDate = cal.time
        _uiState.value = _uiState.value.copy(currentDate = newDate, isToday = checkIfToday(newDate))
        loadPrayerTimes()
    }

    private fun checkIfToday(date: Date, today: Calendar = Calendar.getInstance()): Boolean {
        val target = Calendar.getInstance().apply { time = date }
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }

    private fun getMethodDisplayName(method: Int): String {
        return when (method) {
            20 -> "KEMENAG"
            1 -> "Univ. Karachi"
            2 -> "ISNA"
            3 -> "MWL"
            4 -> "Umm Al-Qura"
            5 -> "Egyptian"
            11 -> "MUIS"
            17 -> "JAKIM"
            else -> "Method $method"
        }
    }

    private fun addMinutes(time: String, add: Int): String {
        val parts = time.split(":")
        if (parts.size != 2) return time
        val hour = parts[0].toIntOrNull() ?: return time
        val minute = parts[1].toIntOrNull() ?: return time
        val total = hour * 60 + minute + add
        val h = (total / 60) % 24
        val m = total % 60
        return "%02d:%02d".format(Locale.US, h, m)
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
