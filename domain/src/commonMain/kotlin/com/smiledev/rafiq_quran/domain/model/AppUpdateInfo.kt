package com.smiledev.rafiq_quran.domain.model

data class AppUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val isUpdateAvailable: Boolean,
    val releaseName: String,
    val releaseNotes: String? = null,
    val releaseUrl: String,
    val downloadUrl: String? = null,
    val publishedAt: String? = null
)
