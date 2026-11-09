package com.smiledev.rafiq_quran.ui.tasbih.history

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.smiledev.rafiq_quran.domain.model.TasbihDayHistory
import com.smiledev.rafiq_quran.domain.model.TasbihHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

val DEFAULT_INITIAL_TASBIH_HISTORY = listOf(
    TasbihDayHistory(
        date = "2026-10-28",
        totalCount = 100,
        items = listOf(
            TasbihHistoryItem(1L, "2026-10-28", 1, "Subhanallah", "سُبْحَانَ اللَّهِ", 33),
            TasbihHistoryItem(2L, "2026-10-28", 2, "Alhamdulillah", "الْحَمْدُ لِلَّهِ", 33),
            TasbihHistoryItem(3L, "2026-10-28", 3, "Allahu Akbar", "اللَّهُ أَكْبَرُ", 34)
        )
    ),
    TasbihDayHistory(
        date = "2026-10-27",
        totalCount = 200,
        items = listOf(
            TasbihHistoryItem(4L, "2026-10-27", 4, "Astaghfirullah", "أَسْتَغْفِرُ اللَّهَ", 100),
            TasbihHistoryItem(5L, "2026-10-27", 5, "Subhanallahi wa Bihamdihi", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", 100)
        )
    ),
    TasbihDayHistory(
        date = "2026-10-26",
        totalCount = 133,
        items = listOf(
            TasbihHistoryItem(6L, "2026-10-26", 6, "La Ilaha Illallah", "لَا إِلَهَ إِلَّا اللَّهُ", 100),
            TasbihHistoryItem(7L, "2026-10-26", 1, "Subhanallah", "سُبْحَانَ اللَّهِ", 33)
        )
    ),
    TasbihDayHistory(
        date = "2026-10-25",
        totalCount = 99,
        items = listOf(
            TasbihHistoryItem(8L, "2026-10-25", 7, "Shalawat Nabi", "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ", 99)
        )
    )
)

@Immutable
data class SharedTasbihHistoryUiState(
    val dailyHistories: List<TasbihDayHistory> = DEFAULT_INITIAL_TASBIH_HISTORY,
    val totalAllTimeCount: Int = 532,
    val activeDaysCount: Int = 4,
    val todayCount: Int = 100,
    val streakDays: Int = 4,
    val isLoading: Boolean = false
)

class SharedTasbihHistoryViewModel(
    initialHistory: List<TasbihDayHistory> = DEFAULT_INITIAL_TASBIH_HISTORY
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SharedTasbihHistoryUiState(
            dailyHistories = initialHistory,
            totalAllTimeCount = initialHistory.sumOf { it.totalCount },
            activeDaysCount = initialHistory.count { it.totalCount > 0 },
            todayCount = initialHistory.firstOrNull { it.date == "2026-10-28" }?.totalCount ?: 0,
            streakDays = calculateStreak(initialHistory)
        )
    )
    val uiState: StateFlow<SharedTasbihHistoryUiState> = _uiState.asStateFlow()

    fun deleteDay(date: String) {
        _uiState.update { state ->
            val updated = state.dailyHistories.filterNot { it.date == date }
            state.copy(
                dailyHistories = updated,
                totalAllTimeCount = updated.sumOf { it.totalCount },
                activeDaysCount = updated.count { it.totalCount > 0 },
                todayCount = updated.firstOrNull { it.date == "2026-10-28" }?.totalCount ?: 0,
                streakDays = calculateStreak(updated)
            )
        }
    }

    fun clearAll() {
        _uiState.update {
            it.copy(
                dailyHistories = emptyList(),
                totalAllTimeCount = 0,
                activeDaysCount = 0,
                todayCount = 0,
                streakDays = 0
            )
        }
    }

    private fun calculateStreak(histories: List<TasbihDayHistory>): Int {
        return histories.takeWhile { it.totalCount > 0 }.size
    }
}
