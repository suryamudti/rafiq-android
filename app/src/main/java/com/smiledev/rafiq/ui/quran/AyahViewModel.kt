package com.smiledev.rafiq.ui.quran

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.currentLocaleCode
import com.smiledev.rafiq.domain.model.Ayah
import com.smiledev.rafiq.domain.model.Surah
import com.smiledev.rafiq.domain.repository.BookmarkRepository
import com.smiledev.rafiq.domain.repository.QuranRepository
import android.text.Html
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.data.remote.EQuranApiService
import com.smiledev.rafiq.data.remote.IslamicAppApiService
import com.smiledev.rafiq.service.AudioPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NavMarker(val type: String, val label: String, val ayahNumber: Int)

@Immutable
data class AyahUiState(
    val ayahs: List<Ayah> = emptyList(),
    val currentSurah: Surah? = null,
    val suraNumber: Int = -1,
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val bookmarkedAyahs: Set<Int> = emptySet(),
    val translationLanguage: String = "system",
    val ayahFontSize: Int = 22,
    val translationFontSize: Int = 15,
    val searchQuery: String = "",
    val memorizationMode: Boolean = false,
    val memorizationRevealedAyah: Int? = null,
    val memorizationRepeatCount: Int = 0,
    val currentPlayingAyah: Int? = null,
    val isPlaying: Boolean = false,
    val tafsirCache: Map<String, String> = emptyMap(),
    val tafsirLoadingAyah: Int? = null
)

@HiltViewModel
class AyahViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val preferencesManager: PreferencesManager,
    private val audioPlayer: AudioPlayerController,
    private val islamicAppApi: IslamicAppApiService,
    private val equranApi: EQuranApiService,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AyahUiState())
    val uiState: StateFlow<AyahUiState> = _uiState

    private val localeCode = currentLocaleCode()
    private var cachedSurahs: List<Surah> = emptyList()
    private var lastReadSura: Int = 0
    private var lastReadAya: Int = 0
    private var tafsirSuraLoaded: Int = -1

    init {
        loadSurahs()
        viewModelScope.launch(dispatcherProvider.io) {
            combine(
                preferencesManager.translationLanguage,
                preferencesManager.ayahFontSize,
                preferencesManager.translationFontSize,
                preferencesManager.lastReadSura,
                preferencesManager.lastReadAya
            ) { lang, ayahSize, transSize, readSura, readAya ->
                lastReadSura = readSura
                lastReadAya = readAya
                _uiState.value = _uiState.value.copy(
                    translationLanguage = lang,
                    ayahFontSize = ayahSize,
                    translationFontSize = transSize
                )
            }.collect()
        }
    }

    private fun loadSurahs() {
        viewModelScope.launch(dispatcherProvider.io) {
            val result = quranRepository.getChapters(localeCode)
            if (result is Result.Success) {
                cachedSurahs = result.data
            }
        }
    }

    fun loadAyahs(surahNumber: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = quranRepository.getAyahsWithTranslation(surahNumber, localeCode)
            when (result) {
                is Result.Success -> {
                    val surah = cachedSurahs.find { it.chapterNumber == surahNumber }
                    _uiState.value = _uiState.value.copy(
                        ayahs = result.data,
                        currentSurah = surah,
                        suraNumber = surahNumber,
                        isLoading = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun toggleBookmark(sura: Int, aya: Int, suraName: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            val result = bookmarkRepository.toggle(sura, aya, suraName)
            if (result is Result.Success) {
                val current = _uiState.value.bookmarkedAyahs
                _uiState.value = _uiState.value.copy(
                    bookmarkedAyahs = if (aya in current) current - aya else current + aya
                )
            }
        }
    }

    fun clearAyahs() {
        _uiState.value = _uiState.value.copy(ayahs = emptyList(), currentSurah = null)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun getFilteredAyahs(): List<Ayah> {
        val q = _uiState.value.searchQuery.trim().lowercase()
        if (q.isEmpty()) return _uiState.value.ayahs
        return _uiState.value.ayahs.filter { ayah ->
            ayah.text.lowercase().contains(q) ||
            (ayah.translationId?.lowercase()?.contains(q) == true) ||
            (ayah.translationEn?.lowercase()?.contains(q) == true)
        }
    }

    fun saveLastReadPosition(sura: Int, aya: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.setLastReadPosition(sura, aya)
        }
    }

    fun getLastReadAyahForSura(suraNumber: Int): Int {
        return if (lastReadSura == suraNumber) lastReadAya else 0
    }

    private fun getAyahAudioUrl(suraNumber: Int, ayaNumber: Int): String {
        return "https://everyayah.com/data/Alafasy_128kbps/${String.format("%03d", suraNumber)}${String.format("%03d", ayaNumber)}.mp3"
    }

    fun toggleAyahAudio(ayahNumber: Int) {
        val state = _uiState.value
        if (state.currentPlayingAyah == ayahNumber && state.isPlaying) {
            audioPlayer.stop()
            _uiState.value = state.copy(isPlaying = false, currentPlayingAyah = null)
        } else {
            val url = getAyahAudioUrl(state.suraNumber, ayahNumber)
            audioPlayer.playAyah(url) {
                val nextAyah = ayahNumber + 1
                val maxAyah = state.ayahs.size
                if (nextAyah <= maxAyah) {
                    playAyahAudio(nextAyah)
                } else {
                    _uiState.value = _uiState.value.copy(isPlaying = false, currentPlayingAyah = null)
                }
            }
            _uiState.value = state.copy(isPlaying = true, currentPlayingAyah = ayahNumber)
        }
    }

    private fun playAyahAudio(ayahNumber: Int) {
        val url = getAyahAudioUrl(_uiState.value.suraNumber, ayahNumber)
        audioPlayer.playAyah(url) {
            val state = _uiState.value
            val nextAyah = ayahNumber + 1
            val maxAyah = state.ayahs.size
            if (nextAyah <= maxAyah) {
                playAyahAudio(nextAyah)
            } else {
                _uiState.value = state.copy(isPlaying = false, currentPlayingAyah = null)
            }
        }
        _uiState.value = _uiState.value.copy(isPlaying = true, currentPlayingAyah = ayahNumber)
    }

    fun toggleMemorizationMode() {
        val current = _uiState.value.memorizationMode
        _uiState.value = _uiState.value.copy(
            memorizationMode = !current,
            memorizationRevealedAyah = null,
            memorizationRepeatCount = if (!current) 0 else 0
        )
    }

    fun revealTranslation(ayahNumber: Int) {
        _uiState.value = _uiState.value.copy(memorizationRevealedAyah = ayahNumber)
    }

    fun hideTranslation() {
        _uiState.value = _uiState.value.copy(memorizationRevealedAyah = null)
    }

    private fun shouldUseIndonesianTafsir(): Boolean {
        val lang = _uiState.value.translationLanguage
        return lang == "id" || (lang == "system" && currentLocaleCode() == "id")
    }

    fun loadTafsir(ayahNumber: Int) {
        val suraNumber = _uiState.value.suraNumber
        val key = "$suraNumber:$ayahNumber"
        if (_uiState.value.tafsirCache.containsKey(key)) return
        _uiState.value = _uiState.value.copy(tafsirLoadingAyah = ayahNumber)
        viewModelScope.launch(dispatcherProvider.io) {
            try {
                if (shouldUseIndonesianTafsir() && suraNumber != tafsirSuraLoaded) {
                    val resp = equranApi.getTafsir(suraNumber)
                    val entries = resp.data.tafsir
                    val newCache = mutableMapOf<String, String>()
                    for (entry in entries) {
                        val k = "$suraNumber:${entry.ayat}"
                        newCache[k] = entry.teks
                    }
                    tafsirSuraLoaded = suraNumber
                    _uiState.value = _uiState.value.copy(
                        tafsirCache = _uiState.value.tafsirCache + newCache,
                        tafsirLoadingAyah = null
                    )
                } else if (!shouldUseIndonesianTafsir()) {
                    val response = islamicAppApi.getVerseWithTafsir(suraNumber, ayahNumber, "en-tafisr-ibn-kathir")
                    val tafsirs = response.data.verse.tafsirs
                    val tafsirText = if (tafsirs != null) {
                        val match = tafsirs.find { it.language_name.lowercase() == "english" }
                        (match ?: tafsirs.firstOrNull())?.text
                    } else null
                    val plainText = if (tafsirText != null) {
                        Html.fromHtml(tafsirText, Html.FROM_HTML_MODE_COMPACT).toString()
                    } else "Tafsir not available"
                    _uiState.value = _uiState.value.copy(
                        tafsirCache = _uiState.value.tafsirCache + (key to plainText),
                        tafsirLoadingAyah = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        tafsirCache = _uiState.value.tafsirCache + (key to "Tafsir not available"),
                        tafsirLoadingAyah = null
                    )
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    tafsirCache = _uiState.value.tafsirCache + (key to "Failed to load tafsir"),
                    tafsirLoadingAyah = null
                )
            }
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

    fun getNavMarkers(): List<NavMarker> {
        val markers = mutableListOf<NavMarker>()
        for (ayah in _uiState.value.ayahs) {
            if (ayah.isFirstAyaOfJuz) {
                markers.add(NavMarker("juz", "Juz ${ayah.juz}", ayah.aya))
            }
            if (ayah.isFirstAyaOfPage) {
                markers.add(NavMarker("page", "Page ${ayah.page}", ayah.aya))
            }
        }
        return markers
    }

    fun getTranslationText(ayah: Ayah, translationLanguage: String): String? {
        val resolvedLang = if (translationLanguage == "system") currentLocaleCode() else translationLanguage
        val hasId = !ayah.translationId.isNullOrBlank()
        val hasEn = !ayah.translationEn.isNullOrBlank()
        return when (resolvedLang) {
            "id" -> if (hasId) ayah.translationId else if (hasEn) ayah.translationEn else null
            else -> if (hasEn) ayah.translationEn else if (hasId) ayah.translationId else null
        }
    }
}
