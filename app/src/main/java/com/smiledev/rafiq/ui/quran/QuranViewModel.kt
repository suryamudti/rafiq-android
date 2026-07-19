package com.smiledev.rafiq.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.data.local.BookmarkDao
import com.smiledev.rafiq.data.local.BookmarkEntity
import com.smiledev.rafiq.data.models.AyahData
import com.smiledev.rafiq.data.models.Surah
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.data.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class QuranUiState(
    val surahs: List<Surah> = emptyList(),
    val ayahs: List<AyahData> = emptyList(),
    val currentSurah: Surah? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val bookmarkedAyahs: Set<Int> = emptySet(),
    val translationLanguage: String = "system",
    val ayahFontSize: Int = 22,
    val translationFontSize: Int = 15
)

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val bookmarkDao: BookmarkDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuranUiState())
    val uiState: StateFlow<QuranUiState> = _uiState

    private val localeCode = if (Locale.getDefault().language == "in" || Locale.getDefault().language == "id") "id" else "en"

    init {
        loadSurahs()
        viewModelScope.launch(Dispatchers.IO) {
            preferencesManager.translationLanguage.collect { lang ->
                _uiState.value = _uiState.value.copy(translationLanguage = lang)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            preferencesManager.ayahFontSize.collect { size ->
                _uiState.value = _uiState.value.copy(ayahFontSize = size)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            preferencesManager.translationFontSize.collect { size ->
                _uiState.value = _uiState.value.copy(translationFontSize = size)
            }
        }
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
                val savedAyas = bookmarkDao.getAyasBySura(surahNumber).toSet()
                _uiState.value = _uiState.value.copy(
                    ayahs = ayahs,
                    currentSurah = surah,
                    isLoading = false,
                    bookmarkedAyahs = savedAyas
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleBookmark(sura: Int, aya: Int, suraName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isBookmarked = bookmarkDao.isBookmarked(sura, aya)
            if (isBookmarked) {
                bookmarkDao.delete(sura, aya)
                _uiState.value = _uiState.value.copy(
                    bookmarkedAyahs = _uiState.value.bookmarkedAyahs - aya
                )
            } else {
                val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                bookmarkDao.insert(
                    BookmarkEntity(sura = sura, suraName = suraName, aya = aya, insertTime = timeStamp)
                )
                _uiState.value = _uiState.value.copy(
                    bookmarkedAyahs = _uiState.value.bookmarkedAyahs + aya
                )
            }
        }
    }

    fun clearAyahs() {
        _uiState.value = _uiState.value.copy(ayahs = emptyList(), currentSurah = null)
    }
}
