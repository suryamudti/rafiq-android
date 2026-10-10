package com.smiledev.rafiq_quran.ui.settings

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.AppUpdateInfo
import com.smiledev.rafiq_quran.domain.usecase.CheckAppUpdateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UpdateCheckStatus {
    UP_TO_DATE,
    ERROR
}

@Immutable
data class SettingsUiState(
    val themeMode: String = "system",
    val translationLanguage: String = "system",
    val hiddenKeys: Set<String> = emptySet(),
    val appVersion: String = "",
    val isCheckingUpdate: Boolean = false,
    val updateInfo: AppUpdateInfo? = null,
    val updateStatus: UpdateCheckStatus? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase? = null,
    @ApplicationContext private val context: Context? = null,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    // Secondary constructor for existing unit tests
    constructor(
        preferencesManager: PreferencesManager,
        dispatcherProvider: DispatcherProvider
    ) : this(preferencesManager, null, null, dispatcherProvider)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val versionName = context?.let { ctx ->
            runCatching {
                ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0"
            }.getOrDefault("1.0")
        } ?: "1.0"

        _uiState.value = _uiState.value.copy(appVersion = versionName)

        viewModelScope.launch(dispatcherProvider.io) {
            combine(
                preferencesManager.themeMode,
                preferencesManager.translationLanguage,
                preferencesManager.hiddenDashboardKeys
            ) { theme, lang, hidden ->
                _uiState.value = _uiState.value.copy(
                    themeMode = theme,
                    translationLanguage = lang,
                    hiddenKeys = hidden
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

    fun toggleDashboardKey(key: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.toggleDashboardKey(key)
        }
    }

    fun resetDashboard() {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.setDashboardHidden(emptySet())
        }
    }

    fun checkForUpdate() {
        val useCase = checkAppUpdateUseCase ?: return
        if (_uiState.value.isCheckingUpdate) return

        _uiState.value = _uiState.value.copy(
            isCheckingUpdate = true,
            updateStatus = null
        )

        viewModelScope.launch(dispatcherProvider.io) {
            val ver = _uiState.value.appVersion.ifEmpty { "1.0" }
            when (val result = useCase(ver)) {
                is Result.Success -> {
                    val info = result.data
                    _uiState.value = _uiState.value.copy(
                        isCheckingUpdate = false,
                        updateInfo = if (info.isUpdateAvailable) info else null,
                        updateStatus = if (!info.isUpdateAvailable) UpdateCheckStatus.UP_TO_DATE else null
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isCheckingUpdate = false,
                        updateInfo = null,
                        updateStatus = UpdateCheckStatus.ERROR
                    )
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.value = _uiState.value.copy(updateInfo = null)
    }

    fun clearUpdateStatus() {
        _uiState.value = _uiState.value.copy(updateStatus = null)
    }
}
