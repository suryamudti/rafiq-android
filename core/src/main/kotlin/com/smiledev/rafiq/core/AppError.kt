package com.smiledev.rafiq.core

sealed interface AppError {
    data class Network(val message: String, val cause: Throwable? = null) : AppError
    data class Database(val message: String, val cause: Throwable? = null) : AppError
    data object NotFound : AppError
    data class Unknown(val message: String) : AppError
}

val AppError.displayMessage: String get() = when (this) {
    is AppError.Network -> "Unable to connect. Please check your internet connection and try again."
    is AppError.Database -> "Something went wrong while loading data. Please restart the app."
    is AppError.NotFound -> "The requested information was not found."
    is AppError.Unknown -> "An unexpected error occurred. Please try again."
}
