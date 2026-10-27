package com.smiledev.rafiq_quran.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberTasbihFeedbackController(): TasbihFeedbackController {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        AndroidTasbihFeedbackController(context)
    }
}
