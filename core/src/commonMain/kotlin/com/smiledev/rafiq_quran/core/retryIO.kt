package com.smiledev.rafiq_quran.core

import kotlinx.coroutines.delay

suspend fun <T> retryIO(
    times: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> Result<T, AppError>
): Result<T, AppError> {
    require(times >= 1) { "times must be at least 1" }
    var currentDelay = initialDelay
    repeat(times - 1) {
        when (val result = block()) {
            is Result.Success -> return result
            is Result.Error -> {
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
    }
    return block()
}
