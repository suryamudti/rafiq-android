package com.smiledev.rafiq_quran.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RetryIOTest {

    @Test
    fun retriesAllAttemptsWithExponentialBackoff() = runTest {
        val attemptTimes = mutableListOf<Long>()

        val result = retryIO<String>(
            times = 3,
            initialDelay = 100,
            maxDelay = 1000,
            factor = 2.0
        ) {
            attemptTimes.add(currentTime)
            Result.Error(AppError.Network("fail"))
        }

        assertTrue(result is Result.Error)
        assertEquals(listOf(0L, 100L, 300L), attemptTimes)
        assertEquals(300L, currentTime)
    }

    @Test
    fun capsBackoffDelayAtMaxDelay() = runTest {
        val attemptTimes = mutableListOf<Long>()

        val result = retryIO<String>(
            times = 4,
            initialDelay = 100,
            maxDelay = 150,
            factor = 2.0
        ) {
            attemptTimes.add(currentTime)
            Result.Error(AppError.Network("fail"))
        }

        assertTrue(result is Result.Error)
        // Delays: 100, 150 (capped from 200), 150
        assertEquals(listOf(0L, 100L, 250L, 400L), attemptTimes)
    }

    @Test
    fun returnsImmediatelyOnSuccessWithoutExtraDelay() = runTest {
        val attemptTimes = mutableListOf<Long>()

        val result = retryIO(
            times = 3,
            initialDelay = 100,
            maxDelay = 1000,
            factor = 2.0
        ) {
            attemptTimes.add(currentTime)
            if (attemptTimes.size == 2) Result.Success("ok") else Result.Error(AppError.Network("retry"))
        }

        assertEquals(Result.Success("ok"), result)
        assertEquals(listOf(0L, 100L), attemptTimes)
        assertEquals(100L, currentTime)
    }
}
