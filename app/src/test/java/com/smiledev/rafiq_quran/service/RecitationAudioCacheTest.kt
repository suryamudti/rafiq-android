package com.smiledev.rafiq_quran.service

import android.content.Context
import androidx.media3.common.util.UnstableApi
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@androidx.annotation.OptIn(UnstableApi::class)
class RecitationAudioCacheTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("test_cache").toFile()
        RecitationAudioCache.releaseCache()
    }

    @After
    fun tearDown() {
        RecitationAudioCache.releaseCache()
        tempDir.deleteRecursively()
    }

    @Test
    fun constants_haveExpectedValues() {
        assertEquals("recitation_audio_cache", RecitationAudioCache.CACHE_DIR_NAME)
        assertEquals(100L * 1024L * 1024L, RecitationAudioCache.DEFAULT_MAX_CACHE_BYTES)
    }

    @Test
    fun getCacheSize_whenNoCacheInitialized_returnsZero() {
        val context = mockk<Context>(relaxed = true)
        assertEquals(0L, RecitationAudioCache.getCacheSize(context))
    }

    @Test
    fun releaseCache_whenNoActiveCache_completesSafely() {
        RecitationAudioCache.releaseCache()
        // No exception thrown
    }
}
