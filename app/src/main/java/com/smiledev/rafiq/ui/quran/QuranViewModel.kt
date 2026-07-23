package com.smiledev.rafiq.ui.quran

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.currentLocaleCode
import com.smiledev.rafiq.domain.model.Ayah
import com.smiledev.rafiq.domain.model.Surah
import com.smiledev.rafiq.domain.repository.BookmarkRepository
import com.smiledev.rafiq.domain.repository.QuranRepository
import com.smiledev.rafiq.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class QuranUiState(
    val surahs: List<Surah> = emptyList(),
    val ayahs: List<Ayah> = emptyList(),
    val currentSurah: Surah? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val bookmarkedAyahs: Set<Int> = emptySet(),
    val translationLanguage: String = "system",
    val ayahFontSize: Int = 22,
    val translationFontSize: Int = 15
)

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuranUiState())
    val uiState: StateFlow<QuranUiState> = _uiState

    private val localeCode = currentLocaleCode()

    init {
        loadSurahs()
        viewModelScope.launch(dispatcherProvider.io) {
            combine(
                preferencesManager.translationLanguage,
                preferencesManager.ayahFontSize,
                preferencesManager.translationFontSize
            ) { lang, ayahSize, transSize ->
                _uiState.value = _uiState.value.copy(
                    translationLanguage = lang,
                    ayahFontSize = ayahSize,
                    translationFontSize = transSize
                )
            }.collect()
        }
    }

    fun loadSurahs() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = quranRepository.getChapters(localeCode)
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(surahs = result.data, isLoading = false)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun loadAyahs(surahNumber: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = quranRepository.getAyahsWithTranslation(surahNumber, localeCode)
            when (result) {
                is Result.Success -> {
                    val surah = _uiState.value.surahs.find { it.chapterNumber == surahNumber }
                    _uiState.value = _uiState.value.copy(
                        ayahs = result.data,
                        currentSurah = surah,
                        isLoading = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun toggleBookmark(sura: Int, aya: Int, suraName: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            val result = bookmarkRepository.toggle(sura, aya, suraName)
            if (result is Result.Success) {
                val current = _uiState.value.bookmarkedAyahs
                _uiState.value = _uiState.value.copy(
                    bookmarkedAyahs = if (aya in current) current - aya else current + aya
                )
            }
        }
    }

    fun refresh() { loadSurahs() }

    fun clearAyahs() {
        _uiState.value = _uiState.value.copy(ayahs = emptyList(), currentSurah = null)
    }
}
