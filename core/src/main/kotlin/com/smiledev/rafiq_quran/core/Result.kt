package com.smiledev.rafiq.core

sealed class Result<out T, out E : AppError> {
    data class Success<T>(val data: T) : Result<T, Nothing>()
    data class Error<E : AppError>(val error: E) : Result<Nothing, E>()
}

inline fun <T, E : AppError> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T, E : AppError> Result<T, E>.onError(action: (E) -> Unit): Result<T, E> {
    if (this is Result.Error) action(error)
    return this
}

fun <T, E : AppError> Result<T, E>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    is Result.Error -> null
}
