package com.smiledev.rafiq.ui.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class SettingsUiState(
    val themeMode: String = "system",
    val translationLanguage: String = "system",
    val ayahFontSize: Int = 22,
    val translationFontSize: Int = 15
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            combine(
                preferencesManager.themeMode,
                preferencesManager.translationLanguage,
                preferencesManager.ayahFontSize,
                preferencesManager.translationFontSize
            ) { theme, lang, ayahSize, transSize ->
                _uiState.value = _uiState.value.copy(
                    themeMode = theme,
                    translationLanguage = lang,
                    ayahFontSize = ayahSize,
                    translationFontSize = transSize
                )
            }.collect()
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun setTranslationLanguage(lang: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.setTranslationLanguage(lang)
        }
    }

    fun setAyahFontSize(size: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.setAyahFontSize(size)
        }
    }

    fun setTranslationFontSize(size: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.setTranslationFontSize(size)
        }
    }
}
