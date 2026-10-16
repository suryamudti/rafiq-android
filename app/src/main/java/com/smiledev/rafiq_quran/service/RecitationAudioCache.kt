package com.smiledev.rafiq_quran.service

import android.content.Context
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Singleton disk cache manager for ExoPlayer audio recitations.
 *
 * Caches downloaded audio files (e.g. EveryAyah / Quran.com recitations) up to 100 MB using
 * a Least-Recently-Used (LRU) eviction strategy.
 * Subsequent playbacks of the same ayah load directly from disk without network calls,
 * saving mobile data and enabling instant offline repetition (e.g. for Hifz memorization).
 */
@OptIn(UnstableApi::class)
object RecitationAudioCache {

    const val CACHE_DIR_NAME = "recitation_audio_cache"
    const val DEFAULT_MAX_CACHE_BYTES = 100L * 1024L * 1024L // 100 MB

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context, maxSizeBytes: Long = DEFAULT_MAX_CACHE_BYTES): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: run {
                val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                val evictor = LeastRecentlyUsedCacheEvictor(maxSizeBytes)
                val databaseProvider = StandaloneDatabaseProvider(context)
                SimpleCache(cacheDir, evictor, databaseProvider).also {
                    simpleCache = it
                }
            }
        }
    }

    /**
     * Builds a [DataSource.Factory] wrapped with [CacheDataSource.Factory] that automatically
     * reads from and writes to the LRU disk cache for audio recitations.
     */
    fun createDataSourceFactory(context: Context): DataSource.Factory {
        val cache = getCache(context)
        val upstreamFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Rafiq-Quran/1.0")
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(10_000)
            .setAllowCrossProtocolRedirects(true)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Returns total bytes currently stored in the audio recitation cache.
     */
    fun getCacheSize(context: Context): Long {
        return simpleCache?.cacheSpace ?: 0L
    }

    /**
     * Releases the active cache instance (useful during teardown or tests).
     */
    @Synchronized
    fun releaseCache() {
        try {
            simpleCache?.release()
        } catch (_: Exception) {
            // Ignore release errors during shutdown
        } finally {
            simpleCache = null
        }
    }

    @VisibleForTesting
    @Synchronized
    fun setTestCache(cache: SimpleCache?) {
        simpleCache = cache
    }
}
