package com.smiledev.rafiq_quran

import androidx.compose.ui.window.ComposeUIViewController
import com.smiledev.rafiq_quran.ui.RafiqAppRoot
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    RafiqAppRoot()
}
