package com.smiledev.rafiq.ui.recitation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.Reciter
import com.smiledev.rafiq.domain.model.Surah
import com.smiledev.rafiq.domain.repository.QuranRepository
import com.smiledev.rafiq.domain.repository.ReciterRepository
import com.smiledev.rafiq.service.AudioPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class RecitationUiState(
    val reciters: List<Reciter> = emptyList(),
    val surahs: List<Surah> = emptyList(),
    val selectedReciter: Reciter? = null,
    val currentSurah: Surah? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val error: AppError? = null
)

@HiltViewModel
class RecitationViewModel @Inject constructor(
    private val reciterRepository: ReciterRepository,
    private val quranRepository: QuranRepository,
    private val audioPlayer: AudioPlayerController,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecitationUiState())
    val uiState: StateFlow<RecitationUiState> = _uiState

    init { loadReciters() }

    private fun loadReciters() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = reciterRepository.getReciters()
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(reciters = result.data, isLoading = false)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun selectReciter(reciter: Reciter) {
        _uiState.value = _uiState.value.copy(selectedReciter = reciter, currentSurah = null)
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = quranRepository.getChapters()
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(surahs = result.data, isLoading = false)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun playSurah(surah: Surah) {
        val reciter = _uiState.value.selectedReciter ?: return
        val url = "https://everyayah.com/data/${reciter.identifier}/${String.format("%03d", surah.chapterNumber)}.mp3"
        _uiState.value = _uiState.value.copy(currentSurah = surah, isPlaying = true)
        audioPlayer.play(url)
    }

    fun togglePlayback() {
        audioPlayer.toggle()
        _uiState.value = _uiState.value.copy(isPlaying = audioPlayer.isPlaying)
    }

    fun stop() {
        audioPlayer.stop()
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    fun backToReciters() {
        stop()
        _uiState.value = _uiState.value.copy(selectedReciter = null, surahs = emptyList(), currentSurah = null)
    }

    override fun onCleared() {
        audioPlayer.release()
        super.onCleared()
    }
}
