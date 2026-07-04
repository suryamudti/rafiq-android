package com.smiledev.rafiq.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.data.models.AyahData
import com.smiledev.rafiq.data.models.Surah
import com.smiledev.rafiq.data.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class QuranUiState(
    val surahs: List<Surah> = emptyList(),
    val ayahs: List<AyahData> = emptyList(),
    val currentSurah: Surah? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val quranRepository: QuranRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuranUiState())
    val uiState: StateFlow<QuranUiState> = _uiState

    private val localeCode = if (Locale.getDefault().language == "in" || Locale.getDefault().language == "id") "id" else "en"

    init {
        loadSurahs()
    }

    fun loadSurahs() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val surahs = quranRepository.getChapters(localeCode)
                _uiState.value = _uiState.value.copy(surahs = surahs, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadAyahs(surahNumber: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val surah = _uiState.value.surahs.find { it.chapterNumber == surahNumber }
                val ayahs = quranRepository.getAyahsWithTranslation(surahNumber, localeCode)
                _uiState.value = _uiState.value.copy(
                    ayahs = ayahs,
                    currentSurah = surah,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun clearAyahs() {
        _uiState.value = _uiState.value.copy(ayahs = emptyList(), currentSurah = null)
    }
}
