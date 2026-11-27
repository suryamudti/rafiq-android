package com.smiledev.rafiq_quran.service

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.smiledev.rafiq_quran.core.DispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioPlayerControllerTest {

    private fun testDispatcherProvider(dispatcher: CoroutineDispatcher) = object : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val unconfined: CoroutineDispatcher = dispatcher
    }

    @Test
    fun `stop when disconnected does not connect or throw and resets state`() = runTest {
        val context = mockk<Context>(relaxed = true)
        val controller = AudioPlayerController(context, testDispatcherProvider(StandardTestDispatcher(testScheduler)))

        controller.stop()

        assertEquals(PlaybackState(), controller.playbackState.value)
        verify(exactly = 1) { context.stopService(any()) }
    }

    @Test
    fun `stop when connected stops and releases controller, cancels polling, and resets state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val context = mockk<Context>(relaxed = true)
        val mediaController = mockk<MediaController>(relaxed = true)
        val listenerSlot = slot<Player.Listener>()
        var currentPos = 1000L

        every { mediaController.addListener(capture(listenerSlot)) } returns Unit
        every { mediaController.isPlaying } returns true
        every { mediaController.duration } returns 10_000L
        every { mediaController.currentPosition } answers { currentPos }

        val audioController = AudioPlayerController(context, testDispatcherProvider(dispatcher))
        audioController.onControllerConnected(mediaController)
        runCurrent()

        assertTrue(audioController.playbackState.value.isPlaying)
        assertEquals(1000L, audioController.playbackState.value.positionMs)

        audioController.stop()

        verify(exactly = 1) { mediaController.stop() }
        verify(exactly = 1) { mediaController.release() }
        verify(exactly = 1) { context.stopService(any()) }
        assertEquals(PlaybackState(), audioController.playbackState.value)

        // Advance time and ensure no further polling reads occur after stop
        currentPos = 2000L
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(PlaybackState(), audioController.playbackState.value)
    }

    @Test
    fun `poller runs only while playing and stops when paused or ended`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val context = mockk<Context>(relaxed = true)
        val mediaController = mockk<MediaController>(relaxed = true)
        val listenerSlot = slot<Player.Listener>()
        var playing = false
        var pos = 0L

        every { mediaController.addListener(capture(listenerSlot)) } returns Unit
        every { mediaController.isPlaying } answers { playing }
        every { mediaController.duration } returns 30_000L
        every { mediaController.currentPosition } answers { pos }

        val audioController = AudioPlayerController(context, testDispatcherProvider(dispatcher))
        audioController.onControllerConnected(mediaController)
        runCurrent()

        // Not playing initially: advancing time should not update position via poller
        pos = 500L
        advanceTimeBy(1500)
        runCurrent()
        assertEquals(0L, audioController.playbackState.value.positionMs)

        // Start playing -> poller should start and update state every 500ms
        playing = true
        listenerSlot.captured.onIsPlayingChanged(true)
        runCurrent()
        assertEquals(500L, audioController.playbackState.value.positionMs)

        pos = 1000L
        advanceTimeBy(500)
        runCurrent()
        assertEquals(1000L, audioController.playbackState.value.positionMs)

        // Pause -> poller should stop
        playing = false
        listenerSlot.captured.onIsPlayingChanged(false)
        runCurrent()
        assertFalse(audioController.playbackState.value.isPlaying)

        pos = 2500L
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(1000L, audioController.playbackState.value.positionMs)

        // Resume then STATE_ENDED -> poller should stop
        playing = true
        listenerSlot.captured.onIsPlayingChanged(true)
        runCurrent()
        assertEquals(2500L, audioController.playbackState.value.positionMs)

        playing = false
        listenerSlot.captured.onPlaybackStateChanged(Player.STATE_ENDED)
        runCurrent()

        pos = 4000L
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(2500L, audioController.playbackState.value.positionMs)

        audioController.release()
    }
}
