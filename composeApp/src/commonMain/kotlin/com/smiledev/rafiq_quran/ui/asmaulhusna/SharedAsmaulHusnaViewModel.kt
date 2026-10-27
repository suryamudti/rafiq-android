package com.smiledev.rafiq_quran.ui.asmaulhusna

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.AsmaulHusna
import com.smiledev.rafiq_quran.domain.usecase.GetAsmaulHusnaUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val DEFAULT_SHARED_ASMAUL_HUSNA = listOf(
    AsmaulHusna(1, "الرَّحْمَنُ", "Ar-Rahman", "The Beneficent", "Yang Maha Pengasih", "Encourages broad mercy and gratitude", "Mengingatkan akan rahmat Allah yang luas"),
    AsmaulHusna(2, "الرَّحِيمُ", "Ar-Rahim", "The Merciful", "Yang Maha Penyayang", "Spurs constant devotion and hope", "Mendorong ketakwaan dan harapan"),
    AsmaulHusna(3, "الْمَلِكُ", "Al-Malik", "The King", "Yang Maha Merajai", "Reminds that true sovereignty belongs to Allah", "Mengingatkan bahwa kerajaan sejati adalah milik Allah"),
    AsmaulHusna(4, "الْقُدُّوسُ", "Al-Quddus", "The Most Holy", "Yang Maha Suci", "Instills purity of heart and purpose", "Menyucikan hati dari segala cela"),
    AsmaulHusna(5, "السَّلَامُ", "As-Salam", "The Source of Peace", "Yang Maha Memberi Kesejahteraan", "Spreads peaceful conduct and inner calmness", "Menebarkan kedamaian dan ketenangan jiwa"),
    AsmaulHusna(6, "الْمُؤْمِنُ", "Al-Mu'min", "The Giver of Security", "Yang Maha Memberi Keamanan", "Fosters reassurance and steadfast faith", "Memberi rasa aman dan meneguhkan keimanan"),
    AsmaulHusna(7, "الْمُهَيْمِنُ", "Al-Muhaymin", "The Guardian", "Yang Maha Memelihara", "Assures comprehensive protection", "Menjaga dan memelihara seluruh ciptaan"),
    AsmaulHusna(8, "الْعَزِيزُ", "Al-Aziz", "The All-Mighty", "Yang Maha Perkasa", "Provides strength in facing hardships", "Memberi kekuatan dalam menghadapi kesulitan"),
    AsmaulHusna(9, "الْجَبَّارُ", "Al-Jabbar", "The Compeller", "Yang Maha Memaksa", "Mends broken affairs and upholds truth", "Memperbaiki segala urusan yang retak"),
    AsmaulHusna(10, "الْمُتَكَبِّرُ", "Al-Mutakabbir", "The Supreme", "Yang Maha Memiliki Kebesaran", "Cultivates humility before the Creator", "Menumbuhkan kerendahan hati di hadapan-Nya")
)

@Immutable
data class SharedAsmaulHusnaUiState(
    val names: List<AsmaulHusna> = DEFAULT_SHARED_ASMAUL_HUSNA,
    val searchQuery: String = "",
    val favoriteIds: Set<Int> = emptySet(),
    val showFavoritesOnly: Boolean = false,
    val isGridView: Boolean = true,
    val selectedName: AsmaulHusna? = null,
    val dhikrCount: Int = 0,
    val isLoading: Boolean = false,
    val error: AppError? = null
)

class SharedAsmaulHusnaViewModel(
    private val getAsmaulHusnaUseCase: GetAsmaulHusnaUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedAsmaulHusnaUiState())
    val uiState: StateFlow<SharedAsmaulHusnaUiState> = _uiState.asStateFlow()

    init {
        loadNames()
    }

    fun loadNames() {
        if (getAsmaulHusnaUseCase == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getAsmaulHusnaUseCase.invoke()) {
                is Result.Success -> {
                    val list = if (result.data.isNotEmpty()) result.data else DEFAULT_SHARED_ASMAUL_HUSNA
                    _uiState.update { it.copy(names = list, isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error) }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFavoritesOnly() {
        _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
    }

    fun toggleGridView() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun selectName(name: AsmaulHusna?) {
        _uiState.update { it.copy(selectedName = name, dhikrCount = 0) }
    }

    fun incrementDhikr() {
        _uiState.update { it.copy(dhikrCount = it.dhikrCount + 1) }
    }

    fun resetDhikr() {
        _uiState.update { it.copy(dhikrCount = 0) }
    }

    fun toggleFavorite(id: Int) {
        _uiState.update { state ->
            val updated = if (id in state.favoriteIds) {
                state.favoriteIds - id
            } else {
                state.favoriteIds + id
            }
            state.copy(favoriteIds = updated)
        }
    }

    fun selectRandomName() {
        val names = _uiState.value.names
        if (names.isNotEmpty()) {
            selectName(names.random())
        }
    }

    fun getFilteredNames(state: SharedAsmaulHusnaUiState = _uiState.value): List<AsmaulHusna> {
        val query = state.searchQuery.trim().lowercase()
        return state.names.filter { item ->
            val matchesFavorites = !state.showFavoritesOnly || item.id in state.favoriteIds
            val matchesSearch = query.isEmpty() ||
                item.transliteration.lowercase().contains(query) ||
                item.meaningEn.lowercase().contains(query) ||
                item.meaningId.lowercase().contains(query) ||
                item.arabic.contains(query) ||
                item.id.toString() == query

            matchesFavorites && matchesSearch
        }
    }
}
