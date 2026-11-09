package com.smiledev.rafiq_quran.ui.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class SharedDashboardUiState(
    val greeting: String = "Assalamu'alaikum",
    val hijriDate: String = "16 Rabi'ul Akhir 1448 H",
    val gregorianDate: String = "Wednesday, 28 Oct 2026",
    val nextPrayerName: String = "Maghrib",
    val nextPrayerTime: String = "17:58",
    val countdown: String = "01h 24m",
    val cityName: String = "Jakarta, Indonesia",
    val dailyVerseArabic: String = "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
    val dailyVerseTranslation: String = "Remember Me; I will remember you. And be grateful to Me and do not deny Me.",
    val dailyVerseSurah: String = "QS. Al-Baqarah: 152",
    val dailyVerseSuraNumber: Int = 2,
    val dailyVerseAyaNumber: Int = 152,
    val dailyHadithArabic: String = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ",
    val dailyHadithTranslation: String = "The reward of deeds depends upon the intentions and every person will get the reward according to what he has intended.",
    val dailyHadithRef: String = "HR. Bukhari No. 1",
    val lastReadSura: Int = 2,
    val lastReadSuraName: String = "Al-Baqarah",
    val lastReadAya: Int = 255,
    val todayCompletedPrayersCount: Int = 3,
    val totalPrayersTarget: Int = 5
)

class SharedDashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SharedDashboardUiState())
    val uiState: StateFlow<SharedDashboardUiState> = _uiState.asStateFlow()

    fun updateCity(city: String) {
        _uiState.update { it.copy(cityName = city) }
    }

    fun updateLastRead(suraNumber: Int, suraName: String, ayaNumber: Int) {
        _uiState.update {
            it.copy(
                lastReadSura = suraNumber,
                lastReadSuraName = suraName,
                lastReadAya = ayaNumber
            )
        }
    }
}
