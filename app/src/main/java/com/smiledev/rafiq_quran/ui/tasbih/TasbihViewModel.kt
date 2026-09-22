package com.smiledev.rafiq_quran.ui.tasbih

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.TasbihItem
import com.smiledev.rafiq_quran.domain.repository.TasbihRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.smiledev.rafiq_quran.domain.model.TasbihHistoryItem
import com.smiledev.rafiq_quran.domain.repository.TasbihHistoryRepository
import com.smiledev.rafiq_quran.domain.util.SystemTodayProvider
import com.smiledev.rafiq_quran.domain.util.TodayProvider
import java.util.Locale
import javax.inject.Inject

@Immutable
data class TasbihUiState(
    val items: List<TasbihItem> = emptyList(),
    val selectedItem: TasbihItem? = null,
    val count: Int = 0,
    val lap: Int = 1,
    val totalCount: Int = 0,
    val target: Int = 33,
    val isVibrationEnabled: Boolean = true,
    val isSoundEnabled: Boolean = false,
    val showDhikrPicker: Boolean = false,
    val showResetDialog: Boolean = false,
    val showCustomTargetDialog: Boolean = false,
    val isTargetReachedNotice: Boolean = false,
    val todayDhikrCount: Int = 0
) {
    val progress: Float
        get() = if (target > 0) (count.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
}

@HiltViewModel
class TasbihViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TasbihRepository,
    private val preferencesManager: PreferencesManager,
    private val tasbihHistoryRepository: TasbihHistoryRepository,
    private val todayProvider: TodayProvider = SystemTodayProvider,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    // Secondary constructor for testing compatibility
    constructor(
        context: Context,
        repository: TasbihRepository = object : TasbihRepository {
            override fun getTasbihItems(): Result<List<TasbihItem>, com.smiledev.rafiq_quran.core.AppError> =
                Result.Success(emptyList())
        },
        preferencesManager: PreferencesManager = PreferencesManager(context),
        tasbihHistoryRepository: TasbihHistoryRepository = object : TasbihHistoryRepository {
            override fun observeAllRecords() = kotlinx.coroutines.flow.flowOf(emptyList<TasbihHistoryItem>())
            override fun observeDayRecords(date: String) = kotlinx.coroutines.flow.flowOf(emptyList<TasbihHistoryItem>())
            override suspend fun addOrUpdateCount(date: String, dhikrId: Int, dhikrName: String, arabic: String, delta: Int) = Result.Success(Unit)
            override suspend fun deleteDay(date: String) = Result.Success(Unit)
            override suspend fun clearAll() = Result.Success(Unit)
        },
        todayProvider: TodayProvider = SystemTodayProvider
    ) : this(context, repository, preferencesManager, tasbihHistoryRepository, todayProvider, DefaultDispatcherProvider)

    private val _uiState = MutableStateFlow(TasbihUiState())
    val uiState: StateFlow<TasbihUiState> = _uiState

    init {
        loadData()
        observeTodayHistory()
    }

    private fun getTodayDateString(): String {
        val today = todayProvider.today()
        return String.format(Locale.US, "%04d-%02d-%02d", today.year, today.month, today.day)
    }

    private fun observeTodayHistory() {
        viewModelScope.launch(dispatcherProvider.io) {
            tasbihHistoryRepository.observeDayRecords(getTodayDateString()).collect { records ->
                val totalToday = records.sumOf { it.count }
                _uiState.update { it.copy(todayDhikrCount = totalToday) }
            }
        }
    }

    private fun updateDailyHistory(delta: Int) {
        val current = _uiState.value
        val todayStr = getTodayDateString()
        val item = current.selectedItem
        val dhikrId = item?.id ?: 0
        val dhikrName = item?.transliteration ?: "Custom"
        val arabic = item?.arabic ?: ""

        viewModelScope.launch(dispatcherProvider.io) {
            tasbihHistoryRepository.addOrUpdateCount(
                date = todayStr,
                dhikrId = dhikrId,
                dhikrName = dhikrName,
                arabic = arabic,
                delta = delta
            )
        }
    }

    private fun loadData() {
        viewModelScope.launch(dispatcherProvider.io) {
            val result = repository.getTasbihItems()
            val items = if (result is Result.Success) result.data else emptyList()

            val savedSelectedId = preferencesManager.tasbihSelectedId.first()
            val savedCount = preferencesManager.tasbihCount.first()
            val savedLap = preferencesManager.tasbihLap.first()
            val savedTotal = preferencesManager.tasbihTotal.first()
            val savedTarget = preferencesManager.tasbihTarget.first()
            val savedVibration = preferencesManager.tasbihVibrationEnabled.first()
            val savedSound = preferencesManager.tasbihSoundEnabled.first()

            val selected = items.firstOrNull { it.id == savedSelectedId } ?: items.firstOrNull()
            val effectiveTarget = if (savedTarget > 0) savedTarget else (selected?.defaultCount ?: 33)

            _uiState.update {
                it.copy(
                    items = items,
                    selectedItem = selected,
                    count = savedCount,
                    lap = savedLap,
                    totalCount = savedTotal,
                    target = effectiveTarget,
                    isVibrationEnabled = savedVibration,
                    isSoundEnabled = savedSound
                )
            }
        }
    }

    fun increment() {
        val current = _uiState.value
        val isMilestone = current.target > 0 && (current.count + 1) == current.target
        val isOverflow = current.target > 0 && current.count >= current.target

        val nextCount = if (isOverflow) 1 else current.count + 1
        val nextLap = if (isOverflow) current.lap + 1 else current.lap
        val nextTotal = current.totalCount + 1

        _uiState.update {
            it.copy(
                count = nextCount,
                lap = nextLap,
                totalCount = nextTotal,
                isTargetReachedNotice = isMilestone
            )
        }

        if (current.isVibrationEnabled) {
            if (isMilestone) {
                vibrateMilestone()
            } else {
                vibrateTap()
            }
        }
        if (current.isSoundEnabled) {
            playClickSound()
        }

        persistSession(nextCount, nextLap, nextTotal)
        updateDailyHistory(delta = 1)
    }

    fun decrement() {
        val current = _uiState.value
        if (current.count <= 0) return

        val nextCount = current.count - 1
        val nextTotal = (current.totalCount - 1).coerceAtLeast(0)

        _uiState.update {
            it.copy(
                count = nextCount,
                totalCount = nextTotal,
                isTargetReachedNotice = false
            )
        }

        if (current.isVibrationEnabled) {
            vibrateTap()
        }

        persistSession(nextCount, current.lap, nextTotal)
        updateDailyHistory(delta = -1)
    }

    fun reset(resetAll: Boolean = false) {
        _uiState.update {
            it.copy(
                count = 0,
                lap = if (resetAll) 1 else it.lap,
                totalCount = if (resetAll) 0 else it.totalCount,
                showResetDialog = false,
                isTargetReachedNotice = false
            )
        }

        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.resetTasbih(resetAll)
        }
    }

    fun selectDhikr(item: TasbihItem?) {
        val newTarget = item?.defaultCount ?: _uiState.value.target
        _uiState.update {
            it.copy(
                selectedItem = item,
                target = newTarget,
                count = 0,
                showDhikrPicker = false,
                isTargetReachedNotice = false
            )
        }

        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.setTasbihSelectedId(item?.id ?: 0)
            preferencesManager.setTasbihTarget(newTarget)
            preferencesManager.setTasbihCount(0)
        }
    }

    fun setTarget(target: Int) {
        _uiState.update {
            it.copy(
                target = target,
                showCustomTargetDialog = false,
                isTargetReachedNotice = target > 0 && it.count >= target
            )
        }

        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.setTasbihTarget(target)
        }
    }

    fun toggleVibration() {
        val next = !_uiState.value.isVibrationEnabled
        _uiState.update { it.copy(isVibrationEnabled = next) }
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.setTasbihVibration(next)
        }
    }

    fun toggleSound() {
        val next = !_uiState.value.isSoundEnabled
        _uiState.update { it.copy(isSoundEnabled = next) }
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.setTasbihSound(next)
        }
    }

    fun setShowDhikrPicker(show: Boolean) {
        _uiState.update { it.copy(showDhikrPicker = show) }
    }

    fun setShowResetDialog(show: Boolean) {
        _uiState.update { it.copy(showResetDialog = show) }
    }

    fun setShowCustomTargetDialog(show: Boolean) {
        _uiState.update { it.copy(showCustomTargetDialog = show) }
    }

    private fun vibrateTap() {
        try {
            val vibrator = getVibrator()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
        } catch (_: Throwable) {}
    }

    private fun vibrateMilestone() {
        try {
            val vibrator = getVibrator()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 80, 150), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 80, 80, 150), -1)
            }
        } catch (_: Throwable) {}
    }

    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun playClickSound() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f)
        } catch (_: Throwable) {}
    }

    private fun persistSession(count: Int, lap: Int, total: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.updateTasbihSession(count, lap, total)
        }
    }
}
