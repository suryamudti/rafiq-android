package com.smiledev.rafiq_quran.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberTasbihFeedbackController(): TasbihFeedbackController {
    return remember {
        IosTasbihFeedbackController()
    }
}
