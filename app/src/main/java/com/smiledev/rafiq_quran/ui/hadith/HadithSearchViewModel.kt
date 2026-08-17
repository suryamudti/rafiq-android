package com.smiledev.rafiq_quran.ui.hadith

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.Hadith
import com.smiledev.rafiq_quran.domain.model.HadithBook
import com.smiledev.rafiq_quran.domain.repository.HadithRepository
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
data class HadithSearchUiState(
    val query: String = "",
    val results: List<Hadith> = emptyList(),
    val books: List<HadithBook> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val translationLanguage: String = "system"
)

@HiltViewModel
class HadithSearchViewModel @Inject constructor(
    private val hadithRepository: HadithRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HadithSearchUiState())
    val uiState: StateFlow<HadithSearchUiState> = _uiState

    private var searchJob: Job? = null

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.translationLanguage.collect { lang ->
                _uiState.value = _uiState.value.copy(translationLanguage = lang)
            }
        }
        loadBooks()
    }

    private fun loadBooks() {
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = hadithRepository.getBooks()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(books = result.data)
                is Result.Error -> _uiState.value = _uiState.value.copy(error = result.error)
            }
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch(dispatcherProvider.io) {
            delay(SEARCH_DEBOUNCE_MS)
            val term = _uiState.value.query.trim()
            if (term.isEmpty()) {
                _uiState.value = _uiState.value.copy(results = emptyList(), isLoading = false, error = null)
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = hadithRepository.searchHadiths(term, SEARCH_LIMIT)) {
                is Result.Success -> {
                    if (_uiState.value.query.trim() == term) {
                        _uiState.value = _uiState.value.copy(results = result.data, isLoading = false)
                    }
                }
                is Result.Error -> {
                    if (_uiState.value.query.trim() == term) {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
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
