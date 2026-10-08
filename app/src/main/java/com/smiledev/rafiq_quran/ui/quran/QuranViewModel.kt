package com.smiledev.rafiq_quran.ui.quran

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 250L
private const val SEARCH_LIMIT = 100

enum class RevelationFilter {
    ALL,
    MECCAN,
    MEDINAN
}

enum class QuranSearchTab {
    SURAHS,
    AYAHS
}

@Immutable
data class QuranUiState(
    val surahs: List<Surah> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val searchQuery: String = "",
    val searchResults: List<Ayah> = emptyList(),
    val searchLoading: Boolean = false,
    val searchError: AppError? = null,
    val translationLanguage: String = "system",
    val lastReadSura: Int = 0,
    val lastReadAya: Int = 0,
    val revelationFilter: RevelationFilter = RevelationFilter.ALL,
    val searchTab: QuranSearchTab = QuranSearchTab.SURAHS
) {
    val lastReadSurah: Surah?
        get() = if (lastReadSura > 0) surahs.find { it.chapterNumber == lastReadSura } else null

    val meccanCount: Int
        get() = surahs.count {
            it.revelationPlace.equals("makkah", ignoreCase = true) ||
                it.revelationPlace.equals("meccan", ignoreCase = true)
        }

    val medinanCount: Int
        get() = surahs.count {
            it.revelationPlace.equals("madinah", ignoreCase = true) ||
                it.revelationPlace.equals("medinan", ignoreCase = true)
        }

    val filteredSurahs: List<Surah>
        get() {
            val byRevelation = when (revelationFilter) {
                RevelationFilter.ALL -> surahs
                RevelationFilter.MECCAN -> surahs.filter {
                    it.revelationPlace.equals("makkah", ignoreCase = true) ||
                        it.revelationPlace.equals("meccan", ignoreCase = true)
                }
                RevelationFilter.MEDINAN -> surahs.filter {
                    it.revelationPlace.equals("madinah", ignoreCase = true) ||
                        it.revelationPlace.equals("medinan", ignoreCase = true)
                }
            }
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return byRevelation
            return byRevelation.filter { surah ->
                surah.chapterNumber.toString() == query ||
                    surah.nameSimple.lowercase().contains(query) ||
                    surah.nameArabic.contains(query) ||
                    surah.translatedName.lowercase().contains(query)
            }
        }
}

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuranUiState())
    val uiState: StateFlow<QuranUiState> = _uiState

    private var searchJob: Job? = null

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.translationLanguage.collect { lang ->
                _uiState.value = _uiState.value.copy(translationLanguage = lang)
                loadSurahs()
            }
        }
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.lastReadSura.collect { sura ->
                _uiState.value = _uiState.value.copy(lastReadSura = sura)
            }
        }
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.lastReadAya.collect { aya ->
                _uiState.value = _uiState.value.copy(lastReadAya = aya)
            }
        }
    }

    fun setRevelationFilter(filter: RevelationFilter) {
        _uiState.value = _uiState.value.copy(revelationFilter = filter)
    }

    fun setSearchTab(tab: QuranSearchTab) {
        _uiState.value = _uiState.value.copy(searchTab = tab)
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchResults = emptyList(),
            searchLoading = false,
            searchError = null
        )
    }

    fun loadSurahs() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val lang = resolvedLanguage()
            val chapterLang = if (lang == "both") currentLocaleCode() else lang
            val result = quranRepository.getChapters(chapterLang)
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

    fun refresh() { loadSurahs() }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch(dispatcherProvider.io) {
            delay(SEARCH_DEBOUNCE_MS)
            val term = _uiState.value.searchQuery.trim()
            if (term.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    searchResults = emptyList(), searchLoading = false, searchError = null
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(searchLoading = true, searchError = null)
            when (val result = quranRepository.searchAyahs(term, resolvedLanguage(), SEARCH_LIMIT)) {
                is Result.Success -> {
                    if (_uiState.value.searchQuery.trim() == term) {
                        _uiState.value = _uiState.value.copy(searchResults = result.data, searchLoading = false)
                    }
                }
                is Result.Error -> {
                    if (_uiState.value.searchQuery.trim() == term) {
                        _uiState.value = _uiState.value.copy(searchLoading = false, searchError = result.error)
                    }
                }
            }
        }
    }

    fun resolvedLanguage(): String {
        val lang = _uiState.value.translationLanguage
        return if (lang == "system") currentLocaleCode() else lang
    }
}
