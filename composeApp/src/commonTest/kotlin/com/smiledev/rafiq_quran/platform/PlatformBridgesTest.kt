package com.smiledev.rafiq_quran.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformBridgesTest {

    private class FakeAudioPlayerController : AudioPlayerController {
        private val _state = MutableStateFlow(AudioPlaybackState())
        override val playbackState: StateFlow<AudioPlaybackState> = _state

        override fun play(url: String, ayahNumber: Int) {
            _state.value = AudioPlaybackState(isPlaying = true, currentAyah = ayahNumber)
        }

        override fun playAyah(url: String, ayahNumber: Int, onComplete: () -> Unit) {
            _state.value = AudioPlaybackState(isPlaying = true, currentAyah = ayahNumber)
            onComplete()
        }

        override fun toggle() {
            _state.value = _state.value.copy(isPlaying = !_state.value.isPlaying)
        }

        override fun stop() {
            _state.value = _state.value.copy(isPlaying = false)
        }

        override fun seekTo(positionMs: Long) {
            _state.value = _state.value.copy(currentPositionMs = positionMs)
        }

        override fun release() {
            _state.value = AudioPlaybackState()
        }
    }

    private class FakeTasbihFeedbackController : TasbihFeedbackController {
        var tickCount = 0
        var targetReachedCount = 0
        var soundCount = 0

        override fun vibrateTick() { tickCount++ }
        override fun vibrateTargetReached() { targetReachedCount++ }
        override fun playClickSound() { soundCount++ }
    }

    @Test
    fun testAudioPlayerControllerFlow() {
        val controller = FakeAudioPlayerController()
        assertFalse(controller.playbackState.value.isPlaying)

        controller.play("https://example.com/audio.mp3", 1)
        assertTrue(controller.playbackState.value.isPlaying)
        assertEquals(1, controller.playbackState.value.currentAyah)

        controller.toggle()
        assertFalse(controller.playbackState.value.isPlaying)

        controller.seekTo(1500L)
        assertEquals(1500L, controller.playbackState.value.currentPositionMs)

        controller.release()
        assertEquals(0L, controller.playbackState.value.currentPositionMs)
    }

    @Test
    fun testTasbihFeedbackController() {
        val feedback = FakeTasbihFeedbackController()
        feedback.vibrateTick()
        feedback.vibrateTick()
        feedback.vibrateTargetReached()
        feedback.playClickSound()

        assertEquals(2, feedback.tickCount)
        assertEquals(1, feedback.targetReachedCount)
        assertEquals(1, feedback.soundCount)
    }
}
