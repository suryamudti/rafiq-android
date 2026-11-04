package com.smiledev.rafiq_quran.ui.hifz

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.smiledev.rafiq_quran.domain.repository.HifzGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class HifzFilter(val label: String) {
    ALL("All Goals"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed")
}

@Immutable
data class SharedHifzUiState(
    val goals: List<HifzGoal> = emptyList(),
    val filter: HifzFilter = HifzFilter.ALL,
    val isLoading: Boolean = false,
    val formError: String? = null
) {
    val totalAyahsTarget: Int
        get() = goals.sumOf { it.totalAyahs }

    val totalAyahsMemorized: Int
        get() = goals.sumOf { it.memorizedCount }

    val overallPercentage: Int
        get() = if (totalAyahsTarget == 0) 0 else (totalAyahsMemorized * 100) / totalAyahsTarget

    val completedGoalsCount: Int
        get() = goals.count { it.isComplete }

    val filteredGoals: List<HifzGoal>
        get() = when (filter) {
            HifzFilter.ALL -> goals
            HifzFilter.IN_PROGRESS -> goals.filter { !it.isComplete }
            HifzFilter.COMPLETED -> goals.filter { it.isComplete }
        }
}

val DEFAULT_INITIAL_HIFZ_GOALS = listOf(
    HifzGoal(
        id = 1L,
        suraNumber = 67,
        suraName = "Al-Mulk",
        ayaStart = 1,
        ayaEnd = 30,
        memorizedAyahs = (1..15).toSet()
    ),
    HifzGoal(
        id = 2L,
        suraNumber = 78,
        suraName = "An-Naba",
        ayaStart = 1,
        ayaEnd = 40,
        memorizedAyahs = (1..20).toSet()
    ),
    HifzGoal(
        id = 3L,
        suraNumber = 18,
        suraName = "Al-Kahf",
        ayaStart = 1,
        ayaEnd = 10,
        memorizedAyahs = (1..10).toSet()
    ),
    HifzGoal(
        id = 4L,
        suraNumber = 55,
        suraName = "Ar-Rahman",
        ayaStart = 1,
        ayaEnd = 25,
        memorizedAyahs = (1..8).toSet()
    )
)

class SharedHifzViewModel(
    initialGoals: List<HifzGoal> = DEFAULT_INITIAL_HIFZ_GOALS
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SharedHifzUiState(goals = initialGoals)
    )
    val uiState: StateFlow<SharedHifzUiState> = _uiState.asStateFlow()

    private var nextGoalId: Long = 100L

    fun setFilter(filter: HifzFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun toggleAyah(goalId: Long, aya: Int) {
        _uiState.update { state ->
            val updated = state.goals.map { goal ->
                if (goal.id == goalId) {
                    val updatedSet = if (aya in goal.memorizedAyahs) {
                        goal.memorizedAyahs - aya
                    } else {
                        goal.memorizedAyahs + aya
                    }
                    goal.copy(memorizedAyahs = updatedSet)
                } else {
                    goal
                }
            }
            state.copy(goals = updated)
        }
    }

    fun markAllAyahs(goalId: Long, memorized: Boolean) {
        _uiState.update { state ->
            val updated = state.goals.map { goal ->
                if (goal.id == goalId) {
                    val updatedSet = if (memorized) {
                        (goal.ayaStart..goal.ayaEnd).toSet()
                    } else {
                        emptySet()
                    }
                    goal.copy(memorizedAyahs = updatedSet)
                } else {
                    goal
                }
            }
            state.copy(goals = updated)
        }
    }

    fun addGoal(suraNumber: Int, suraName: String, ayaStart: Int, ayaEnd: Int): Boolean {
        if (suraNumber < 1 || suraNumber > 114 || ayaStart < 1 || ayaStart > ayaEnd) {
            _uiState.update { it.copy(formError = "Invalid Surah or Ayah range") }
            return false
        }

        val name = suraName.trim().ifEmpty { "Surah $suraNumber" }
        val newGoal = HifzGoal(
            id = nextGoalId++,
            suraNumber = suraNumber,
            suraName = name,
            ayaStart = ayaStart,
            ayaEnd = ayaEnd,
            memorizedAyahs = emptySet()
        )

        _uiState.update { state ->
            state.copy(
                goals = state.goals + newGoal,
                formError = null
            )
        }
        return true
    }

    fun deleteGoal(goalId: Long) {
        _uiState.update { state ->
            state.copy(goals = state.goals.filter { it.id != goalId })
        }
    }

    fun clearFormError() {
        _uiState.update { it.copy(formError = null) }
    }
}
