package com.smiledev.rafiq.core

sealed interface AppError {
    data class Network(val message: String, val cause: Throwable? = null) : AppError
    data class Database(val message: String, val cause: Throwable? = null) : AppError
    data object NotFound : AppError
    data class Unknown(val message: String) : AppError
}
