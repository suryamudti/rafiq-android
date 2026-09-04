package com.smiledev.rafiq_quran.ui.dashboard

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.PrayerTimeEntry
import com.smiledev.rafiq_quran.domain.repository.PrayerLogRepository
import com.smiledev.rafiq_quran.domain.repository.PrayerTimesRepository
import com.smiledev.rafiq_quran.domain.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
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
    val appVersion: String = "",
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456,
    val calculationMethod: Int = 20,
    val cityName: String = "Jakarta, ID",
    val hijriDate: String = "",
    val gregorianDate: String = "",
    val prayerTimeline: List<PrayerTimeEntry> = emptyList(),
    val activePrayerIndex: Int = 0,
    val lastReadSura: Int = 0,
    val lastReadAya: Int = 0,
    val lastReadSuraName: String = "",
    val dailyAyahArabic: String = "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
    val dailyAyahTranslation: String = "Remember Me; I will remember you. And be grateful to Me and do not deny Me.",
    val dailyAyahSurahRef: String = "QS. Al-Baqarah: 152",
    val dailyAyahSuraNumber: Int = 2,
    val dailyAyahNumber: Int = 152,
    val todayCompletedPrayersCount: Int = 0
)

private data class DailyInspiration(
    val arabic: String,
    val translationEn: String,
    val translationId: String,
    val surahRef: String,
    val suraNumber: Int,
    val ayaNumber: Int
)

private val inspirations = listOf(
    DailyInspiration(
        arabic = "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
        translationEn = "Remember Me; I will remember you. And be grateful to Me and do not deny Me.",
        translationId = "Ingatlah kepada-Ku, niscaya Aku ingat kepadamu. Bersyukurlah kepada-Ku dan janganlah ingkar kepada-Ku.",
        surahRef = "QS. Al-Baqarah: 152",
        suraNumber = 2,
        ayaNumber = 152
    ),
    DailyInspiration(
        arabic = "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا • إِنَّ مَعَ الْعُسْرِ يُسْرًا",
        translationEn = "Indeed, with hardship comes ease. Indeed, with hardship comes ease.",
        translationId = "Maka sesungguhnya bersama kesulitan ada kemudahan, sesungguhnya bersama kesulitan ada kemudahan.",
        surahRef = "QS. Al-Insyirah: 5-6",
        suraNumber = 94,
        ayaNumber = 5
    ),
    DailyInspiration(
        arabic = "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
        translationEn = "Unquestionably, by the remembrance of Allah hearts are assured.",
        translationId = "Ingatlah, hanya dengan mengingat Allah hati menjadi tenteram.",
        surahRef = "QS. Ar-Ra'd: 28",
        suraNumber = 13,
        ayaNumber = 28
    ),
    DailyInspiration(
        arabic = "وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ",
        translationEn = "And whoever relies upon Allah - then He is sufficient for him.",
        translationId = "Dan barangsiapa bertawakal kepada Allah, niscaya Allah akan mencukupkan (keperluan)nya.",
        surahRef = "QS. At-Talaq: 3",
        suraNumber = 65,
        ayaNumber = 3
    ),
    DailyInspiration(
        arabic = "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا",
        translationEn = "Allah does not burden a soul beyond that it can bear.",
        translationId = "Allah tidak membebani seseorang melainkan sesuai dengan kesanggupannya.",
        surahRef = "QS. Al-Baqarah: 286",
        suraNumber = 2,
        ayaNumber = 286
    ),
    DailyInspiration(
        arabic = "وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ",
        translationEn = "And when My servants ask you concerning Me, indeed I am near.",
        translationId = "Dan apabila hamba-hamba-Ku bertanya kepadamu tentang Aku, maka sesungguhnya Aku dekat.",
        surahRef = "QS. Al-Baqarah: 186",
        suraNumber = 2,
        ayaNumber = 186
    ),
    DailyInspiration(
        arabic = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
        translationEn = "Our Lord, give us in this world that which is good and in the Hereafter that which is good and protect us from the punishment of the Fire.",
        translationId = "Ya Tuhan kami, berilah kami kebaikan di dunia dan kebaikan di akhirat dan peliharalah kami dari siksa neraka.",
        surahRef = "QS. Al-Baqarah: 201",
        suraNumber = 2,
        ayaNumber = 201
    )
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider,
    private val quranRepository: QuranRepository? = null,
    private val prayerLogRepository: PrayerLogRepository? = null
) : ViewModel() {

    var enablePeriodicCountdown: Boolean = true

    // Secondary constructor for unit testing with configurable periodic countdown
    constructor(
        prayerTimesRepository: PrayerTimesRepository,
        preferencesManager: PreferencesManager,
        context: Context,
        dispatcherProvider: DispatcherProvider,
        quranRepository: QuranRepository?,
        prayerLogRepository: PrayerLogRepository?,
        enablePeriodicCountdown: Boolean
    ) : this(
        prayerTimesRepository,
        preferencesManager,
        context,
        dispatcherProvider,
        quranRepository,
        prayerLogRepository
    ) {
        this.enablePeriodicCountdown = enablePeriodicCountdown
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    private var countdownJob: Job? = null

    init {
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        }.getOrDefault("")

        val isIndonesian = Locale.getDefault().language == "id"
        val gregorianFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val insp = inspirations[dayOfYear % inspirations.size]

        _uiState.value = _uiState.value.copy(
            greeting = computeGreeting(),
            appVersion = versionName,
            gregorianDate = gregorianFormat.format(Date()),
            dailyAyahArabic = insp.arabic,
            dailyAyahTranslation = if (isIndonesian) insp.translationId else insp.translationEn,
            dailyAyahSurahRef = insp.surahRef,
            dailyAyahSuraNumber = insp.suraNumber,
            dailyAyahNumber = insp.ayaNumber
        )

        // Observe Coordinates & Method
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

        // Observe City Name
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.cityName.collect { city ->
                val displayCity = if (city.isNotBlank()) city else "Jakarta, ID"
                _uiState.value = _uiState.value.copy(cityName = displayCity)
            }
        }

        // Observe Last Read
        viewModelScope.launch(dispatcherProvider.io) {
            combine(
                preferencesManager.lastReadSura,
                preferencesManager.lastReadAya
            ) { sura, aya ->
                Pair(sura, aya)
            }.collect { (sura, aya) ->
                val suraName = if (sura > 0 && quranRepository != null) {
                    val lang = if (Locale.getDefault().language == "id") "id" else "en"
                    val chaptersResult = quranRepository.getChapters(lang)
                    if (chaptersResult is Result.Success) {
                        chaptersResult.data.find { it.chapterNumber == sura }?.nameSimple ?: "Surah $sura"
                    } else {
                        "Surah $sura"
                    }
                } else if (sura > 0) {
                    "Surah $sura"
                } else {
                    ""
                }
                _uiState.value = _uiState.value.copy(
                    lastReadSura = sura,
                    lastReadAya = aya,
                    lastReadSuraName = suraName
                )
            }
        }

        // Observe Today's Prayer Log
        viewModelScope.launch(dispatcherProvider.io) {
            prayerLogRepository?.observeAll()?.collect { logs ->
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val todayLog = logs.find { it.date == todayStr }
                val count = if (todayLog != null) {
                    listOf(todayLog.fajr, todayLog.dhuhr, todayLog.asr, todayLog.maghrib, todayLog.isha).count { it }
                } else {
                    0
                }
                _uiState.value = _uiState.value.copy(todayCompletedPrayersCount = count)
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
                    val isIndonesian = Locale.getDefault().language == "id"

                    val times = listOf(
                        PrayerTimeEntry("Imsak", data.timings.imsak),
                        PrayerTimeEntry("Fajr (Subuh)", data.timings.fajr),
                        PrayerTimeEntry("Sunrise", data.timings.sunrise),
                        PrayerTimeEntry("Dzuhur", data.timings.dhuhr),
                        PrayerTimeEntry("Asr", data.timings.asr),
                        PrayerTimeEntry("Maghrib", data.timings.maghrib),
                        PrayerTimeEntry("Isya", data.timings.isha)
                    )

                    val timeline = listOf(
                        PrayerTimeEntry(if (isIndonesian) "Subuh" else "Fajr", data.timings.fajr),
                        PrayerTimeEntry(if (isIndonesian) "Dzuhur" else "Dhuhr", data.timings.dhuhr),
                        PrayerTimeEntry(if (isIndonesian) "Ashar" else "Asr", data.timings.asr),
                        PrayerTimeEntry("Maghrib", data.timings.maghrib),
                        PrayerTimeEntry(if (isIndonesian) "Isya" else "Isha", data.timings.isha)
                    )

                    val hijri = data.hijriDate ?: ""
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hijriDate = hijri,
                        prayerTimeline = timeline
                    )
                    updateCountdown(times, timeline)
                    if (enablePeriodicCountdown) {
                        startCountdown(times, timeline)
                    }
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    private fun updateCountdown(allTimes: List<PrayerTimeEntry>, timeline: List<PrayerTimeEntry>) {
        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        var nextPrayer: PrayerTimeEntry? = null
        for (pt in allTimes) {
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
        if (nextPrayer == null) nextPrayer = allTimes.first()

        val parts = nextPrayer.time.split(":")
        if (parts.size == 2) {
            val targetMinutes = parts[0].toIntOrNull()
                ?.let { it * 60 + (parts[1].toIntOrNull() ?: 0) } ?: 0
            var diff = targetMinutes - nowMinutes
            if (diff < 0) diff += 24 * 60
            val hours = diff / 60
            val mins = diff % 60

            val activeIdx = when {
                nextPrayer.name.contains("Fajr", ignoreCase = true) || nextPrayer.name.contains("Imsak", ignoreCase = true) -> 0
                nextPrayer.name.contains("Dzuhur", ignoreCase = true) || nextPrayer.name.contains("Sunrise", ignoreCase = true) -> 1
                nextPrayer.name.contains("Asr", ignoreCase = true) -> 2
                nextPrayer.name.contains("Maghrib", ignoreCase = true) -> 3
                nextPrayer.name.contains("Isya", ignoreCase = true) -> 4
                else -> 0
            }

            _uiState.value = _uiState.value.copy(
                nextPrayerName = nextPrayer.name,
                nextPrayerTime = nextPrayer.time,
                countdown = "${hours}h ${mins}m",
                prayerTimeline = timeline,
                activePrayerIndex = activeIdx
            )
        }
    }

    private fun startCountdown(allTimes: List<PrayerTimeEntry>, timeline: List<PrayerTimeEntry>) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch(dispatcherProvider.io) {
            while (isActive) {
                delay(30_000)
                updateCountdown(allTimes, timeline)
            }
        }
    }

    fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    public override fun onCleared() {
        super.onCleared()
        stopCountdown()
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
