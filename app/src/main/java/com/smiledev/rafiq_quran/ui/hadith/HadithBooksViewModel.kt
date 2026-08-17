package com.smiledev.rafiq_quran.ui.hadith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.HadithBook
import com.smiledev.rafiq_quran.domain.repository.HadithRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class HadithBooksUiState(
    val books: List<HadithBook> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val translationLanguage: String = "system"
)

@HiltViewModel
class HadithBooksViewModel @Inject constructor(
    private val hadithRepository: HadithRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HadithBooksUiState())
    val uiState: StateFlow<HadithBooksUiState> = _uiState

    val localeCode = currentLocaleCode()

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.translationLanguage.collect { lang ->
                _uiState.value = _uiState.value.copy(translationLanguage = lang)
            }
        }
        loadBooks()
    }

    fun loadBooks() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = hadithRepository.getBooks()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(books = result.data, isLoading = false)
                is Result.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
            }
        }
    }

    fun resolvedLanguage(): String {
        val lang = _uiState.value.translationLanguage
        return if (lang == "system") currentLocaleCode() else lang
    }
}
