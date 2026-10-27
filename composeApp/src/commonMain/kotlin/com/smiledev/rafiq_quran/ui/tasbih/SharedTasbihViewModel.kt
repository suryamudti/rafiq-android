package com.smiledev.rafiq_quran.ui.tasbih

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.TasbihItem
import com.smiledev.rafiq_quran.domain.usecase.GetTasbihItemsUseCase
import com.smiledev.rafiq_quran.platform.TasbihFeedbackController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val DEFAULT_SHARED_TASBIH_ITEMS = listOf(
    TasbihItem(
        id = 1,
        arabic = "سُبْحَانَ اللهِ",
        transliteration = "SubhanAllah",
        meaningEn = "Glory be to Allah",
        meaningId = "Maha Suci Allah",
        defaultCount = 33
    ),
    TasbihItem(
        id = 2,
        arabic = "الْحَمْدُ لِلَّهِ",
        transliteration = "Alhamdulillah",
        meaningEn = "Praise be to Allah",
        meaningId = "Segala puji bagi Allah",
        defaultCount = 33
    ),
    TasbihItem(
        id = 3,
        arabic = "اللهُ أَكْبَرُ",
        transliteration = "Allahu Akbar",
        meaningEn = "Allah is the Greatest",
        meaningId = "Allah Maha Besar",
        defaultCount = 33
    ),
    TasbihItem(
        id = 4,
        arabic = "أَسْتَغْفِرُ اللَّهَ",
        transliteration = "Astaghfirullah",
        meaningEn = "I seek forgiveness from Allah",
        meaningId = "Aku memohon ampun kepada Allah",
        defaultCount = 33
    ),
    TasbihItem(
        id = 5,
        arabic = "لَا إِلَهَ إِلَّا اللهُ",
        transliteration = "La ilaha illallah",
        meaningEn = "There is no god but Allah",
        meaningId = "Tiada tuhan selain Allah",
        defaultCount = 100
    )
)

@Immutable
data class SharedTasbihUiState(
    val items: List<TasbihItem> = DEFAULT_SHARED_TASBIH_ITEMS,
    val selectedItem: TasbihItem = DEFAULT_SHARED_TASBIH_ITEMS[0],
    val count: Int = 0,
    val lap: Int = 1,
    val totalCount: Int = 0,
    val target: Int = 33,
    val isVibrationEnabled: Boolean = true,
    val isSoundEnabled: Boolean = false,
    val showDhikrPicker: Boolean = false,
    val showResetDialog: Boolean = false,
    val showCustomTargetDialog: Boolean = false,
    val isTargetReachedNotice: Boolean = false
) {
    val progress: Float
        get() = if (target > 0) (count.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
}

class SharedTasbihViewModel(
    private val getTasbihItemsUseCase: GetTasbihItemsUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedTasbihUiState())
    val uiState: StateFlow<SharedTasbihUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        if (getTasbihItemsUseCase == null) return
        viewModelScope.launch {
            when (val result = getTasbihItemsUseCase.invoke()) {
                is Result.Success -> {
                    if (result.data.isNotEmpty()) {
                        _uiState.update { current ->
                            current.copy(
                                items = result.data,
                                selectedItem = result.data.firstOrNull() ?: current.selectedItem,
                                target = result.data.firstOrNull()?.defaultCount ?: current.target
                            )
                        }
                    }
                }
                is Result.Error -> {
                    // Fall back to built-in default items
                }
            }
        }
    }

    fun onIncrement(feedbackController: TasbihFeedbackController? = null) {
        _uiState.update { current ->
            val nextCount = current.count + 1
            val reachedTarget = nextCount >= current.target

            val (newCount, newLap, notice) = if (reachedTarget) {
                Triple(0, current.lap + 1, true)
            } else {
                Triple(nextCount, current.lap, false)
            }

            feedbackController?.let { controller ->
                if (current.isVibrationEnabled) {
                    if (reachedTarget) {
                        controller.vibrateTargetReached()
                    } else {
                        controller.vibrateTick()
                    }
                }
                if (current.isSoundEnabled) {
                    controller.playClickSound()
                }
            }

            current.copy(
                count = newCount,
                lap = newLap,
                totalCount = current.totalCount + 1,
                isTargetReachedNotice = notice
            )
        }
    }

    fun onDecrement() {
        _uiState.update { current ->
            if (current.count > 0) {
                current.copy(
                    count = current.count - 1,
                    totalCount = maxOf(0, current.totalCount - 1),
                    isTargetReachedNotice = false
                )
            } else current
        }
    }

    fun onReset() {
        _uiState.update { current ->
            current.copy(
                count = 0,
                lap = 1,
                showResetDialog = false,
                isTargetReachedNotice = false
            )
        }
    }

    fun onSelectItem(item: TasbihItem) {
        _uiState.update { current ->
            current.copy(
                selectedItem = item,
                count = 0,
                lap = 1,
                target = item.defaultCount,
                showDhikrPicker = false,
                isTargetReachedNotice = false
            )
        }
    }

    fun setTarget(target: Int) {
        if (target <= 0) return
        _uiState.update { current ->
            current.copy(
                target = target,
                showCustomTargetDialog = false
            )
        }
    }

    fun toggleVibration() {
        _uiState.update { it.copy(isVibrationEnabled = !it.isVibrationEnabled) }
    }

    fun toggleSound() {
        _uiState.update { it.copy(isSoundEnabled = !it.isSoundEnabled) }
    }

    fun setDhikrPickerVisible(visible: Boolean) {
        _uiState.update { it.copy(showDhikrPicker = visible) }
    }

    fun setResetDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showResetDialog = visible) }
    }

    fun setCustomTargetDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showCustomTargetDialog = visible) }
    }

    fun dismissTargetReachedNotice() {
        _uiState.update { it.copy(isTargetReachedNotice = false) }
    }
}
