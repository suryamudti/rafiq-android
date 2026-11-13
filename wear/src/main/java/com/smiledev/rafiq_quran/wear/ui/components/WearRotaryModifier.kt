package com.smiledev.rafiq_quran.wear.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import kotlinx.coroutines.launch

fun Modifier.wearRotaryScroll(listState: ScalingLazyListState): Modifier = composed {
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    this
        .onRotaryScrollEvent { event ->
            coroutineScope.launch {
                listState.scrollBy(event.verticalScrollPixels)
            }
            true
        }
        .focusRequester(focusRequester)
        .focusable()
}
