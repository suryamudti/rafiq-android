package com.smiledev.rafiq_quran.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember

@Composable
actual fun rememberDeviceAzimuth(): State<Float> {
    return remember { mutableFloatStateOf(0f) }
}
