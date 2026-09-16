package com.smiledev.rafiq_quran.ui.designsystem.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.displayMessage
import com.smiledev.rafiq_quran.theme.RafiqTheme

@Composable
fun RafiqErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    fullScreen: Boolean = true
) {
    val content = @Composable {
        Column(
            modifier = Modifier.padding(RafiqTheme.spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(RafiqTheme.spacing.l))
                Button(onClick = onRetry) {
                    Text(
                        text = stringResource(R.string.retry),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (fullScreen) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun RafiqErrorState(
    error: AppError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    fullScreen: Boolean = true
) {
    RafiqErrorState(
        message = error.displayMessage,
        modifier = modifier,
        onRetry = onRetry,
        fullScreen = fullScreen
    )
}
