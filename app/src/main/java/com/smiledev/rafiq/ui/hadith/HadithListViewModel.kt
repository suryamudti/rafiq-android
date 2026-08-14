package com.smiledev.rafiq.ui.hadith

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.currentLocaleCode
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.Hadith
import com.smiledev.rafiq.domain.model.HadithBook
import com.smiledev.rafiq.domain.repository.HadithRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class HadithListUiState(
    val book: HadithBook? = null,
    val hadiths: List<Hadith> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val translationLanguage: String = "system"
)

@HiltViewModel
class HadithListViewModel @Inject constructor(
    private val hadithRepository: HadithRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HadithListUiState())
    val uiState: StateFlow<HadithListUiState> = _uiState

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.translationLanguage.collect { lang ->
                _uiState.value = _uiState.value.copy(translationLanguage = lang)
            }
        }
    }

    fun load(bookId: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val booksResult = hadithRepository.getBooks()
            val hadithsResult = hadithRepository.getHadithsByBook(bookId)
            val book = (booksResult as? Result.Success)?.data?.find { it.id == bookId }
            when (hadithsResult) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    book = book, hadiths = hadithsResult.data, isLoading = false
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = hadithsResult.error)
            }
        }
    }

    fun loadById(id: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val booksResult = hadithRepository.getBooks()
            val hadithResult = hadithRepository.getHadithById(id)
            when (hadithResult) {
                is Result.Success -> {
                    val hadith = hadithResult.data
                    val book = hadith?.let { h ->
                        (booksResult as? Result.Success)?.data?.find { it.id == h.bookId }
                    }
                    _uiState.value = _uiState.value.copy(
                        hadiths = hadith?.let { listOf(it) } ?: emptyList(),
                        book = book,
                        isLoading = false
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = hadithResult.error)
            }
        }
    }

    fun resolvedLanguage(): String {
        val lang = _uiState.value.translationLanguage
        return if (lang == "system") currentLocaleCode() else lang
    }
}