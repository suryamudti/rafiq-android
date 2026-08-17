package com.smiledev.rafiq.core

import kotlinx.coroutines.delay

suspend fun <T> retryIO(
    times: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> Result<T, AppError>
): Result<T, AppError> {
    var currentDelay = initialDelay
    repeat(times - 1) { attempt ->
        when (val result = block()) {
            is Result.Success -> return result
            is Result.Error -> {
                if (attempt < times - 2) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
                }
            }
        }
    }
    return block()
}
