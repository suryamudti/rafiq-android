package com.smiledev.rafiq_quran.ui.sources

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SourceCategory(val label: String) {
    ALL("All Sources"),
    QURAN("Qur'an & Hadith"),
    PRAYER("Prayer & Geolocation"),
    LIBRARIES("Open Source")
}

@Immutable
data class SharedSourceItem(
    val name: String,
    val category: SourceCategory,
    val description: String,
    val url: String,
    val license: String = "Open Data / Permissive"
)

val DEFAULT_SOURCES_CATALOG = listOf(
    SharedSourceItem(
        name = "Kementerian Agama RI (Kemenag)",
        category = SourceCategory.QURAN,
        description = "Standard Indonesian Mushaf Rasm Uthmani text, Surah names, translations, and Indonesian prayer calculation standards.",
        url = "https://quran.kemenag.go.id"
    ),
    SharedSourceItem(
        name = "Tanzil Project",
        category = SourceCategory.QURAN,
        description = "International verified standard Quran text and multi-language translations archive.",
        url = "https://tanzil.net"
    ),
    SharedSourceItem(
        name = "Kutubut Tis'ah & Sunnah.com",
        category = SourceCategory.QURAN,
        description = "Canonical Hadith collections (Sahih al-Bukhari, Sahih Muslim, Sunan Abi Dawud, Jami` at-Tirmidhi, etc.).",
        url = "https://sunnah.com"
    ),
    SharedSourceItem(
        name = "AlAdhan Prayer Times API",
        category = SourceCategory.PRAYER,
        description = "Global astronomical prayer time calculation service supporting multiple international conventions.",
        url = "https://aladhan.com"
    ),
    SharedSourceItem(
        name = "OpenFreeMap & MapLibre",
        category = SourceCategory.PRAYER,
        description = "Vector map tiles and open-source rendering engine for displaying nearby mosques globally.",
        url = "https://openfreemap.org"
    ),
    SharedSourceItem(
        name = "Compose Multiplatform",
        category = SourceCategory.LIBRARIES,
        description = "JetBrains declarative UI framework sharing code across Android and iOS.",
        url = "https://www.jetbrains.com/compose-multiplatform"
    ),
    SharedSourceItem(
        name = "Ktor HTTP Client",
        category = SourceCategory.LIBRARIES,
        description = "Multiplatform asynchronous HTTP client for Kotlin.",
        url = "https://ktor.io"
    )
)

@Immutable
data class SharedSourcesUiState(
    val selectedCategory: SourceCategory = SourceCategory.ALL,
    val sources: List<SharedSourceItem> = DEFAULT_SOURCES_CATALOG
) {
    val filteredSources: List<SharedSourceItem>
        get() = when (selectedCategory) {
            SourceCategory.ALL -> sources
            else -> sources.filter { it.category == selectedCategory }
        }
}

class SharedSourcesViewModel(
    initialSources: List<SharedSourceItem> = DEFAULT_SOURCES_CATALOG
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SharedSourcesUiState(sources = initialSources)
    )
    val uiState: StateFlow<SharedSourcesUiState> = _uiState.asStateFlow()

    fun selectCategory(category: SourceCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
}
