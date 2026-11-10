package com.smiledev.rafiq_quran.wear.ui.quran

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.domain.model.Surah
import com.smiledev.rafiq_quran.domain.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WearRevelationFilter {
    ALL,
    MECCAN,
    MEDINAN
}

@Immutable
data class WearQuranUiState(
    val surahs: List<Surah> = emptyList(),
    val revelationFilter: WearRevelationFilter = WearRevelationFilter.ALL,
    val isLoadingSurahs: Boolean = false,
    val surahsError: AppError? = null,
    val currentSurah: Surah? = null,
    val suraNumber: Int = 1,
    val ayahs: List<Ayah> = emptyList(),
    val isLoadingAyahs: Boolean = false,
    val ayahsError: AppError? = null,
    val showTranslation: Boolean = true,
    val translationLanguage: String = "system",
    val lastReadSura: Int = 0,
    val lastReadAya: Int = 0
) {
    val filteredSurahs: List<Surah>
        get() = when (revelationFilter) {
            WearRevelationFilter.ALL -> surahs
            WearRevelationFilter.MECCAN -> surahs.filter {
                it.revelationPlace.equals("makkah", ignoreCase = true) ||
                    it.revelationPlace.equals("meccan", ignoreCase = true)
            }
            WearRevelationFilter.MEDINAN -> surahs.filter {
                it.revelationPlace.equals("madinah", ignoreCase = true) ||
                    it.revelationPlace.equals("medinan", ignoreCase = true)
            }
        }

    val lastReadSurah: Surah?
        get() = if (lastReadSura > 0) surahs.find { it.chapterNumber == lastReadSura } else null
}

@HiltViewModel
class WearQuranViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(WearQuranUiState())
    val uiState: StateFlow<WearQuranUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.translationLanguage.collect { lang ->
                _uiState.update { it.copy(translationLanguage = lang) }
            }
        }
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.lastReadSura.collect { sura ->
                _uiState.update { it.copy(lastReadSura = sura) }
            }
        }
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.lastReadAya.collect { aya ->
                _uiState.update { it.copy(lastReadAya = aya) }
            }
        }
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.showAyahTranslation.collect { show ->
                _uiState.update { it.copy(showTranslation = show) }
            }
        }
        loadSurahs()
    }

    fun loadSurahs() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.update { it.copy(isLoadingSurahs = true, surahsError = null) }
            val lang = when (_uiState.value.translationLanguage) {
                "id" -> "id"
                "en" -> "en"
                else -> currentLocaleCode()
            }
            when (val result = quranRepository.getChapters(lang)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        val resolvedCurrentSurah = current.currentSurah
                            ?: result.data.find { it.chapterNumber == current.suraNumber }
                        current.copy(
                            surahs = result.data,
                            isLoadingSurahs = false,
                            surahsError = null,
                            currentSurah = resolvedCurrentSurah
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingSurahs = false,
                            surahsError = result.error
                        )
                    }
                }
            }
        }
    }

    fun setRevelationFilter(filter: WearRevelationFilter) {
        _uiState.update { it.copy(revelationFilter = filter) }
    }

    fun loadAyahs(suraNumber: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            val existingSurah = _uiState.value.surahs.find { it.chapterNumber == suraNumber }
            _uiState.update {
                it.copy(
                    suraNumber = suraNumber,
                    currentSurah = existingSurah ?: it.currentSurah,
                    isLoadingAyahs = true,
                    ayahsError = null
                )
            }
            val lang = when (_uiState.value.translationLanguage) {
                "id" -> "id"
                "en" -> "en"
                else -> currentLocaleCode()
            }
            when (val result = quranRepository.getAyahsWithTranslation(suraNumber, lang)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        val resolvedSurah = current.currentSurah
                            ?: current.surahs.find { it.chapterNumber == suraNumber }
                        current.copy(
                            ayahs = result.data,
                            isLoadingAyahs = false,
                            ayahsError = null,
                            currentSurah = resolvedSurah
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingAyahs = false,
                            ayahsError = result.error
                        )
                    }
                }
            }
        }
    }

    fun toggleShowTranslation() {
        viewModelScope.launch(dispatcherProvider.io) {
            val next = !_uiState.value.showTranslation
            preferencesManager.setShowAyahTranslation(next)
        }
    }

    fun saveLastRead(sura: Int, aya: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.setLastReadPosition(sura, aya)
        }
    }

    fun getTranslationText(ayah: Ayah): String? {
        val idTrans = ayah.translationId
        val enTrans = ayah.translationEn
        val fallbackTrans = ayah.translation
        val resolvedLang = when (_uiState.value.translationLanguage) {
            "id" -> "id"
            "en" -> "en"
            else -> currentLocaleCode()
        }
        return when (resolvedLang) {
            "id" -> {
                if (!idTrans.isNullOrBlank()) idTrans
                else if (!enTrans.isNullOrBlank()) enTrans
                else fallbackTrans
            }
            else -> {
                if (!enTrans.isNullOrBlank()) enTrans
                else if (!idTrans.isNullOrBlank()) idTrans
                else fallbackTrans
            }
        }
    }
}
