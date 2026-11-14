package com.smiledev.rafiq_quran.tv.ui.ambient

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
import com.smiledev.rafiq_quran.tv.R
import com.smiledev.rafiq_quran.tv.service.TvAdhanAlertEvent
import com.smiledev.rafiq_quran.tv.service.TvPrayerNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Immutable
data class TvCityPreset(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val defaultMethod: Int = 20
)

@Immutable
data class TvSpiritualReminder(
    val arabic: String,
    val translation: String,
    val reference: String
)

@Immutable
data class TvPrayerTimeEntry(
    val name: String,
    val arabicName: String,
    val time: String,
    val prayerKey: String = "",
    @DrawableRes val iconResId: Int = 0,
    val isNext: Boolean = false,
    val isPassed: Boolean = false
)

@Immutable
data class TvAmbientUiState(
    val prayerTimes: List<TvPrayerTimeEntry> = emptyList(),
    val currentDate: Date = Date(),
    val currentClockTime: String = "",
    val isToday: Boolean = true,
    val hijriDate: String = "",
    val nextPrayerName: String = "",
    val nextPrayerArabic: String = "",
    val nextPrayerTime: String = "",
    val countdown: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456,
    val calculationMethod: Int = 20,
    val calculationMethodName: String = "KEMENAG",
    val cityName: String = "Jakarta",
    val prayerNotificationsEnabled: Boolean = true,
    val activeAdhanAlert: TvAdhanAlertEvent? = null,
    val reminderIndex: Int = 0,
    val currentReminder: TvSpiritualReminder = DEFAULT_REMINDERS.first()
) {
    companion object {
        val CITY_PRESETS = listOf(
            TvCityPreset("Jakarta", -6.2088, 106.8456, 20),
            TvCityPreset("Makkah", 21.4225, 39.8262, 4),
            TvCityPreset("Madinah", 24.4672, 39.6112, 4),
            TvCityPreset("Surabaya", -7.2575, 112.7521, 20),
            TvCityPreset("Bandung", -6.9175, 107.6191, 20),
            TvCityPreset("Yogyakarta", -7.7956, 110.3695, 20),
            TvCityPreset("Kuala Lumpur", 3.1390, 101.6869, 17),
            TvCityPreset("Singapore", 1.3521, 103.8198, 11)
        )

        val SUPPORTED_METHODS = listOf(20, 4, 3, 11, 17, 1, 5)

        val DEFAULT_REMINDERS = listOf(
            TvSpiritualReminder(
                arabic = "إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَّوْقُوتًا",
                translation = "Indeed, prayer has been decreed upon the believers a decree of specified times.",
                reference = "QS. An-Nisa: 103"
            ),
            TvSpiritualReminder(
                arabic = "حَافِظُوا عَلَى الصَّلَوَاتِ وَالصَّلَاةِ الْوُسْطَىٰ وَقُومُوا لِلَّهِ قَانِتِينَ",
                translation = "Maintain with care the [obligatory] prayers and [in particular] the middle prayer and stand before Allah, devoutly obedient.",
                reference = "QS. Al-Baqarah: 238"
            ),
            TvSpiritualReminder(
                arabic = "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
                translation = "Unquestionably, by the remembrance of Allah hearts are assured.",
                reference = "QS. Ar-Ra'd: 28"
            )
        )
    }
}

@HiltViewModel
class TvAmbientViewModel @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val appContext: Context,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TvAmbientUiState(
            currentClockTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        )
    )
    val uiState: StateFlow<TvAmbientUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    var enablePeriodicCountdown: Boolean = true

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    private val clockFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            TvPrayerNotificationWorker.activeAdhanAlert.collect { alert ->
                _uiState.value = _uiState.value.copy(activeAdhanAlert = alert)
            }
        }

        viewModelScope.launch(dispatcherProvider.io) {
            combine(
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
            }.collect {
                loadPrayerTimes()
            }
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
                TvPrayerTimeEntry("Imsak", "الإمساك", data.timings.imsak, prayerKey = "imsak", iconResId = R.drawable.ic_prayer_imsak),
                TvPrayerTimeEntry("Subuh", "الفجر", data.timings.fajr, prayerKey = "fajr", iconResId = R.drawable.ic_prayer_fajr),
                TvPrayerTimeEntry("Terbit", "الشروق", data.timings.sunrise, prayerKey = "sunrise", iconResId = R.drawable.ic_prayer_sunrise),
                TvPrayerTimeEntry("Dhuha", "الضحى", dhuhaTime, prayerKey = "dhuha", iconResId = R.drawable.ic_prayer_dhuha),
                TvPrayerTimeEntry("Dzuhur", "الظهر", data.timings.dhuhr, prayerKey = "dhuhr", iconResId = R.drawable.ic_prayer_dhuhr),
                TvPrayerTimeEntry("Ashar", "العصر", data.timings.asr, prayerKey = "asr", iconResId = R.drawable.ic_prayer_asr),
                TvPrayerTimeEntry("Maghrib", "المغرب", data.timings.maghrib, prayerKey = "maghrib", iconResId = R.drawable.ic_prayer_maghrib),
                TvPrayerTimeEntry("Isya", "العشاء", data.timings.isha, prayerKey = "isha", iconResId = R.drawable.ic_prayer_isha)
            )
        } else {
            listOf(
                TvPrayerTimeEntry("Imsak", "الإمساك", data.timings.imsak, prayerKey = "imsak", iconResId = R.drawable.ic_prayer_imsak),
                TvPrayerTimeEntry("Fajr", "الفجر", data.timings.fajr, prayerKey = "fajr", iconResId = R.drawable.ic_prayer_fajr),
                TvPrayerTimeEntry("Sunrise", "الشروق", data.timings.sunrise, prayerKey = "sunrise", iconResId = R.drawable.ic_prayer_sunrise),
                TvPrayerTimeEntry("Dhuha", "الضحى", dhuhaTime, prayerKey = "dhuha", iconResId = R.drawable.ic_prayer_dhuha),
                TvPrayerTimeEntry("Dhuhr", "الظهر", data.timings.dhuhr, prayerKey = "dhuhr", iconResId = R.drawable.ic_prayer_dhuhr),
                TvPrayerTimeEntry("Asr", "العصر", data.timings.asr, prayerKey = "asr", iconResId = R.drawable.ic_prayer_asr),
                TvPrayerTimeEntry("Maghrib", "المغرب", data.timings.maghrib, prayerKey = "maghrib", iconResId = R.drawable.ic_prayer_maghrib),
                TvPrayerTimeEntry("Isha", "العشاء", data.timings.isha, prayerKey = "isha", iconResId = R.drawable.ic_prayer_isha)
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
            TvPrayerNotificationWorker.scheduleNow(appContext)
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch(dispatcherProvider.io) {
            while (isActive) {
                updatePrayerStatusAndCountdown()
                delay(15_000)
            }
        }
    }

    private fun updatePrayerStatusAndCountdown() {
        val state = _uiState.value
        val times = state.prayerTimes
        val now = Calendar.getInstance()
        val clockStr = clockFormat.format(now.time)
        if (times.isEmpty()) {
            _uiState.value = state.copy(currentClockTime = clockStr)
            return
        }

        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val isToday = checkIfToday(state.currentDate)
        val todayStart = Calendar.getInstance().apply {
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

        var nextPrayer: TvPrayerTimeEntry? = null
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
                nextPrayer = times.firstOrNull()
            }
        }

        var countdownStr = ""
        var nextPrayerName = ""
        var nextPrayerArabic = ""
        var nextPrayerTime = ""

        if (isToday && nextPrayer != null) {
            nextPrayerName = nextPrayer.name
            nextPrayerArabic = nextPrayer.arabicName
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
            currentClockTime = clockStr,
            nextPrayerName = nextPrayerName,
            nextPrayerArabic = nextPrayerArabic,
            nextPrayerTime = nextPrayerTime,
            countdown = countdownStr,
            isToday = isToday
        )
    }

    fun selectNextCity() {
        val presets = TvAmbientUiState.CITY_PRESETS
        val currentIdx = presets.indexOfFirst { it.name.equals(_uiState.value.cityName, ignoreCase = true) }
        val nextPreset = presets[(currentIdx + 1) % presets.size]
        selectCity(nextPreset)
    }

    fun selectCity(preset: TvCityPreset) {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.setCityName(preset.name)
            preferencesManager.setLatitude(preset.latitude.toString())
            preferencesManager.setLongitude(preset.longitude.toString())
            preferencesManager.setPrayerCalculationMethod(preset.defaultMethod)
            _uiState.value = _uiState.value.copy(
                cityName = preset.name,
                latitude = preset.latitude,
                longitude = preset.longitude,
                calculationMethod = preset.defaultMethod,
                calculationMethodName = getMethodDisplayName(preset.defaultMethod)
            )
            loadPrayerTimes()
        }
    }

    fun cycleCalculationMethod() {
        viewModelScope.launch(dispatcherProvider.io) {
            val methods = TvAmbientUiState.SUPPORTED_METHODS
            val currentIdx = methods.indexOf(_uiState.value.calculationMethod)
            val nextMethod = methods[(currentIdx + 1) % methods.size]
            preferencesManager.setPrayerCalculationMethod(nextMethod)
            _uiState.value = _uiState.value.copy(
                calculationMethod = nextMethod,
                calculationMethodName = getMethodDisplayName(nextMethod)
            )
            loadPrayerTimes()
        }
    }

    fun nextSpiritualReminder() {
        val reminders = TvAmbientUiState.DEFAULT_REMINDERS
        val nextIndex = (_uiState.value.reminderIndex + 1) % reminders.size
        _uiState.value = _uiState.value.copy(
            reminderIndex = nextIndex,
            currentReminder = reminders[nextIndex]
        )
    }

    fun triggerAdhanPreview() {
        val state = _uiState.value
        val name = state.nextPrayerName.ifBlank { "Maghrib" }
        val time = state.nextPrayerTime.ifBlank { "17:56" }
        TvPrayerNotificationWorker.triggerInAppAdhanAlert(name, time)
    }

    fun dismissAdhanAlert() {
        TvPrayerNotificationWorker.dismissInAppAdhanAlert()
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
                TvPrayerNotificationWorker.scheduleNow(appContext)
            } else {
                TvPrayerNotificationWorker.cancelScheduledAlarms(appContext)
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

    private fun checkIfToday(date: Date): Boolean {
        val today = Calendar.getInstance()
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
        return "%02d:%02d".format(h, m)
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
