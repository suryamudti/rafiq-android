package com.smiledev.rafiq_quran.ui.recitation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.smiledev.rafiq_quran.domain.model.Reciter
import com.smiledev.rafiq_quran.domain.model.Surah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

val DEFAULT_RECITERS = listOf(
    Reciter(
        id = 1,
        nameEn = "Mishary Rashid Alafasy",
        nameAr = "مشاري بن راشد العفاسي",
        style = "Murattal",
        country = "Kuwait",
        audioBase = "https://server8.mp3quran.net/afs/"
    ),
    Reciter(
        id = 2,
        nameEn = "Abdul Basit Abdul Samad",
        nameAr = "عبد الباسط عبد الصمد",
        style = "Murattal",
        country = "Egypt",
        audioBase = "https://server7.mp3quran.net/basit/"
    ),
    Reciter(
        id = 3,
        nameEn = "Maher Al-Muaiqly",
        nameAr = "ماهر المعيقلي",
        style = "Murattal",
        country = "Saudi Arabia",
        audioBase = "https://server12.mp3quran.net/maher/"
    ),
    Reciter(
        id = 4,
        nameEn = "Saad Al-Ghamdi",
        nameAr = "سعد الغامدي",
        style = "Murattal",
        country = "Saudi Arabia",
        audioBase = "https://server7.mp3quran.net/s_gmd/"
    ),
    Reciter(
        id = 5,
        nameEn = "Abu Bakr Ash-Shatri",
        nameAr = "أبو بكر الشاطري",
        style = "Murattal",
        country = "Saudi Arabia",
        audioBase = "https://server11.mp3quran.net/shatri/"
    )
)

val DEFAULT_POPULAR_SURAHS = listOf(
    Surah(1, 1, "الفاتحة", "Al-Fatihah", "The Opener", 7, "makkah"),
    Surah(2, 2, "البقرة", "Al-Baqarah", "The Cow", 286, "madinah"),
    Surah(18, 18, "الكهف", "Al-Kahf", "The Cave", 110, "makkah"),
    Surah(36, 36, "يس", "Yasin", "Ya-Sin", 83, "makkah"),
    Surah(55, 55, "الرحمن", "Ar-Rahman", "The Beneficent", 78, "madinah"),
    Surah(56, 56, "الواقعة", "Al-Waqi'ah", "The Inevitable", 96, "makkah"),
    Surah(67, 67, "الملك", "Al-Mulk", "The Sovereignty", 30, "makkah"),
    Surah(112, 112, "الإخلاص", "Al-Ikhlas", "The Sincerity", 4, "makkah")
)

@Immutable
data class SharedRecitationUiState(
    val reciters: List<Reciter> = DEFAULT_RECITERS,
    val surahs: List<Surah> = DEFAULT_POPULAR_SURAHS,
    val selectedReciter: Reciter = DEFAULT_RECITERS.first(),
    val currentSurah: Surah? = DEFAULT_POPULAR_SURAHS.first(),
    val isPlaying: Boolean = false,
    val positionMs: Long = 45000L,
    val durationMs: Long = 180000L,
    val searchQuery: String = "",
    val isLoading: Boolean = false
) {
    val progress: Float
        get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    val formattedPosition: String
        get() = formatTimeMs(positionMs)

    val formattedDuration: String
        get() = formatTimeMs(durationMs)

    val filteredSurahs: List<Surah>
        get() = if (searchQuery.isBlank()) surahs else {
            val q = searchQuery.trim().lowercase()
            surahs.filter {
                it.nameSimple.lowercase().contains(q) ||
                it.translatedName.lowercase().contains(q) ||
                it.chapterNumber.toString().contains(q)
            }
        }
}

fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val minStr = if (minutes < 10) "0$minutes" else "$minutes"
    val secStr = if (seconds < 10) "0$seconds" else "$seconds"
    return "$minStr:$secStr"
}

class SharedRecitationViewModel(
    initialReciters: List<Reciter> = DEFAULT_RECITERS,
    initialSurahs: List<Surah> = DEFAULT_POPULAR_SURAHS
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SharedRecitationUiState(
            reciters = initialReciters,
            surahs = initialSurahs,
            selectedReciter = initialReciters.first(),
            currentSurah = initialSurahs.first()
        )
    )
    val uiState: StateFlow<SharedRecitationUiState> = _uiState.asStateFlow()

    fun selectReciter(reciter: Reciter) {
        _uiState.update { it.copy(selectedReciter = reciter) }
    }

    fun playSurah(surah: Surah) {
        _uiState.update {
            it.copy(
                currentSurah = surah,
                isPlaying = true,
                positionMs = 0L,
                durationMs = (surah.versesCount * 4500L).coerceAtLeast(60000L)
            )
        }
    }

    fun togglePlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun seekTo(positionMs: Long) {
        _uiState.update { it.copy(positionMs = positionMs.coerceIn(0L, it.durationMs)) }
    }

    fun playNext() {
        val state = _uiState.value
        val currentIndex = state.surahs.indexOfFirst { it.id == state.currentSurah?.id }
        if (currentIndex != -1 && currentIndex < state.surahs.size - 1) {
            playSurah(state.surahs[currentIndex + 1])
        }
    }

    fun playPrevious() {
        val state = _uiState.value
        val currentIndex = state.surahs.indexOfFirst { it.id == state.currentSurah?.id }
        if (currentIndex > 0) {
            playSurah(state.surahs[currentIndex - 1])
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}
