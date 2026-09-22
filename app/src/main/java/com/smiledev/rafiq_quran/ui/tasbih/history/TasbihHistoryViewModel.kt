package com.smiledev.rafiq_quran.ui.tasbih.history

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.domain.model.TasbihDayHistory
import com.smiledev.rafiq_quran.domain.model.TasbihHistoryItem
import com.smiledev.rafiq_quran.domain.repository.TasbihHistoryRepository
import com.smiledev.rafiq_quran.domain.util.SystemTodayProvider
import com.smiledev.rafiq_quran.domain.util.TodayProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@Immutable
data class TasbihHistoryUiState(
    val isLoading: Boolean = true,
    val dailyHistories: List<TasbihDayHistory> = emptyList(),
    val totalAllTimeCount: Int = 0,
    val activeDaysCount: Int = 0,
    val todayCount: Int = 0,
    val showClearAllDialog: Boolean = false,
    val dayToDelete: String? = null
)

@HiltViewModel
class TasbihHistoryViewModel @Inject constructor(
    private val tasbihHistoryRepository: TasbihHistoryRepository,
    private val todayProvider: TodayProvider = SystemTodayProvider,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasbihHistoryUiState())
    val uiState: StateFlow<TasbihHistoryUiState> = _uiState

    init {
        observeHistory()
    }

    private fun getTodayDateString(): String {
        val today = todayProvider.today()
        return String.format(Locale.US, "%04d-%02d-%02d", today.year, today.month, today.day)
    }

    private fun observeHistory() {
        viewModelScope.launch(dispatcherProvider.io) {
            tasbihHistoryRepository.observeAllRecords().collect { allItems ->
                val todayStr = getTodayDateString()
                val grouped = allItems
                    .groupBy { it.date }
                    .map { (date, items) ->
                        TasbihDayHistory(
                            date = date,
                            totalCount = items.sumOf { it.count },
                            items = items.sortedByDescending { it.lastUpdated }
                        )
                    }
                    .sortedByDescending { it.date }

                val totalAll = allItems.sumOf { it.count }
                val activeDays = grouped.count { it.totalCount > 0 }
                val todayTotal = grouped.firstOrNull { it.date == todayStr }?.totalCount ?: 0

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        dailyHistories = grouped,
                        totalAllTimeCount = totalAll,
                        activeDaysCount = activeDays,
                        todayCount = todayTotal
                    )
                }
            }
        }
    }

    fun setShowClearAllDialog(show: Boolean) {
        _uiState.update { it.copy(showClearAllDialog = show) }
    }

    fun setDayToDelete(date: String?) {
        _uiState.update { it.copy(dayToDelete = date) }
    }

    fun confirmDeleteDay() {
        val date = _uiState.value.dayToDelete ?: return
        viewModelScope.launch(dispatcherProvider.io) {
            tasbihHistoryRepository.deleteDay(date)
            _uiState.update { it.copy(dayToDelete = null) }
        }
    }

    fun confirmClearAll() {
        viewModelScope.launch(dispatcherProvider.io) {
            tasbihHistoryRepository.clearAll()
            _uiState.update { it.copy(showClearAllDialog = false) }
        }
    }
}
