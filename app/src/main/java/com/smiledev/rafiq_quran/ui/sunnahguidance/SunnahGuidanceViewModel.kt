package com.smiledev.rafiq_quran.ui.sunnahguidance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.domain.model.SunnahCategory
import com.smiledev.rafiq_quran.domain.model.SunnahGuidanceItem
import com.smiledev.rafiq_quran.domain.usecase.GetSunnahGuidanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SunnahGuidanceUiState(
    val items: List<SunnahGuidanceItem> = emptyList(),
    val selectedCategory: SunnahCategory? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: AppError? = null
)

@HiltViewModel
class SunnahGuidanceViewModel @Inject constructor(
    private val getSunnahGuidanceUseCase: GetSunnahGuidanceUseCase,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SunnahGuidanceUiState())
    val uiState: StateFlow<SunnahGuidanceUiState> = _uiState.asStateFlow()

    val localeCode: String
        get() = currentLocaleCode()

    init {
        loadSunnahGuidance()
    }

    fun loadSunnahGuidance() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = getSunnahGuidanceUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            items = result.data,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.error
                        )
                    }
                }
            }
        }
    }

    fun selectCategory(category: SunnahCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun filterItems(state: SunnahGuidanceUiState): List<SunnahGuidanceItem> {
        var list = state.items
        if (state.selectedCategory != null) {
            list = list.filter { it.category == state.selectedCategory }
        }
        val query = state.searchQuery.trim().lowercase()
        if (query.isNotEmpty()) {
            list = list.filter { item ->
                item.titleEn.lowercase().contains(query) ||
                    item.titleId.lowercase().contains(query) ||
                    item.titleArabic.contains(query) ||
                    item.summaryEn.lowercase().contains(query) ||
                    item.summaryId.lowercase().contains(query) ||
                    item.descriptionEn.lowercase().contains(query) ||
                    item.descriptionId.lowercase().contains(query) ||
                    (item.hadithReference?.lowercase()?.contains(query) == true) ||
                    (item.surahReference?.lowercase()?.contains(query) == true)
            }
        }
        return list
    }
}
