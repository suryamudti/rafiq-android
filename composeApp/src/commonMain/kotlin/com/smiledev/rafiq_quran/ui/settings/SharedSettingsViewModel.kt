package com.smiledev.rafiq_quran.ui.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ThemePreference(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

enum class LanguagePreference(val code: String, val label: String) {
    INDONESIAN("id", "Bahasa Indonesia"),
    ENGLISH("en", "English")
}

enum class QuranFontSize(val label: String, val scaleFactor: Float) {
    SMALL("Small", 0.85f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.25f)
}

@Immutable
data class SharedSettingsUiState(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val language: LanguagePreference = LanguagePreference.INDONESIAN,
    val calculationMethod: String = "KEMENAG (Ministry of Religious Affairs ID)",
    val prayerNotifications: Boolean = true,
    val tasbihVibration: Boolean = true,
    val quranFontSize: QuranFontSize = QuranFontSize.MEDIUM,
    val appVersion: String = "1.0.0-kmp",
    val buildNumber: String = "20261028"
)

val CALCULATION_METHODS = listOf(
    "KEMENAG (Ministry of Religious Affairs ID)",
    "Muslim World League (MWL)",
    "Islamic Society of North America (ISNA)",
    "Umm al-Qura University, Makkah",
    "Egyptian General Authority of Survey"
)

class SharedSettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SharedSettingsUiState())
    val uiState: StateFlow<SharedSettingsUiState> = _uiState.asStateFlow()

    fun setTheme(theme: ThemePreference) {
        _uiState.update { it.copy(theme = theme) }
    }

    fun setLanguage(language: LanguagePreference) {
        _uiState.update { it.copy(language = language) }
    }

    fun setCalculationMethod(method: String) {
        _uiState.update { it.copy(calculationMethod = method) }
    }

    fun togglePrayerNotifications() {
        _uiState.update { it.copy(prayerNotifications = !it.prayerNotifications) }
    }

    fun toggleTasbihVibration() {
        _uiState.update { it.copy(tasbihVibration = !it.tasbihVibration) }
    }

    fun setQuranFontSize(size: QuranFontSize) {
        _uiState.update { it.copy(quranFontSize = size) }
    }
}
