package com.smiledev.rafiq_quran.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

@Composable
expect fun rememberDeviceAzimuth(): State<Float>
