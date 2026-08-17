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

@Immutable
data class QuranUiState(
    val surahs: List<Surah> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val searchQuery: String = "",
    val searchResults: List<Ayah> = emptyList(),
    val searchLoading: Boolean = false,
    val searchError: AppError? = null,
    val translationLanguage: String = "system"
)

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuranUiState())
    val uiState: StateFlow<QuranUiState> = _uiState

    private val localeCode = currentLocaleCode()
    private var searchJob: Job? = null

    init {
        loadSurahs()
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.translationLanguage.collect { lang ->
                _uiState.value = _uiState.value.copy(translationLanguage = lang)
            }
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
