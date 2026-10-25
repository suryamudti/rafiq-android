package com.smiledev.rafiq_quran.core

sealed interface AppError {
    data class Network(val message: String, val cause: Throwable? = null) : AppError
    data class Database(val message: String, val cause: Throwable? = null) : AppError
    data object NotFound : AppError
    data class Unknown(val message: String) : AppError
}

val AppError.displayMessage: String get() {
    val isId = currentLocaleCode() == "id"
    return when (this) {
        is AppError.Network -> if (isId) "Tidak dapat terhubung. Periksa koneksi internet Anda dan coba lagi." else "Unable to connect. Please check your internet connection and try again."
        is AppError.Database -> if (isId) "Terjadi kesalahan saat memuat data. Silakan muat ulang aplikasi." else "Something went wrong while loading data. Please restart the app."
        is AppError.NotFound -> if (isId) "Informasi yang diminta tidak ditemukan." else "The requested information was not found."
        is AppError.Unknown -> if (isId) "Terjadi kesalahan tidak terduga. Silakan coba lagi." else "An unexpected error occurred. Please try again."
    }
}

