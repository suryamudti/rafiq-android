package com.smiledev.rafiq.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import com.smiledev.rafiq.IslamicCalendar
import com.smiledev.rafiq.Mosques
import com.smiledev.rafiq.PrayerTimes
import com.smiledev.rafiq.Prophets
import com.smiledev.rafiq.Qibla
import com.smiledev.rafiq.Quran
import com.smiledev.rafiq.R
import com.smiledev.rafiq.Recitation
import com.smiledev.rafiq.Settings
import com.smiledev.rafiq.Tasbih
import com.smiledev.rafiq.ZakatCalculator
import com.smiledev.rafiq.HadithBooks
import com.smiledev.rafiq.core.displayMessage

private data class FeatureItem(
    val labelResId: Int,
    val navKey: NavKey,
    val iconResId: Int,
    val color: Color
)

private val features = listOf(
    FeatureItem(R.string.quran, Quran(), R.drawable.ic_quran, Color(0xFF3F51B5)),
    FeatureItem(R.string.qibla, Qibla, R.drawable.ic_qibla, Color(0xFFFFC107)),
    FeatureItem(R.string.mosques, Mosques, R.drawable.ic_mosque, Color(0xFF4CAF50)),
    FeatureItem(R.string.recitations, Recitation, R.drawable.ic_play, Color(0xFF2196F3)),
    FeatureItem(R.string.calendar, IslamicCalendar, R.drawable.ic_calendar, Color(0xFF009688)),
    FeatureItem(R.string.zakat, ZakatCalculator, R.drawable.ic_zakat, Color(0xFFFF9800)),
    FeatureItem(R.string.tasbih, Tasbih, R.drawable.ic_tasbih, Color(0xFF009688)),
    FeatureItem(R.string.hadiths, HadithBooks, R.drawable.ic_hadith, Color(0xFF8D6E63)),
    FeatureItem(R.string.prophets, Prophets, R.drawable.ic_prophet, Color(0xFFFFA000)),
)

@Composable
fun DashboardScreen(
    onNavigate: (NavKey) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.greeting_assalamualaikum),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.greeting,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onNavigate(Settings) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = stringResource(R.string.your_islamic_companion),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(PrayerTimes) },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF009688))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(color = Color.White)
                    }
                    state.error != null -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error?.displayMessage ?: "",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.refresh() }) {
                                Text(
                                    text = stringResource(R.string.retry),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.nextPrayerName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = state.nextPrayerTime,
                                fontSize = 35.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.next_prayer, state.countdown),
                                fontSize = 20.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        features.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { feature ->
                    FeatureCard(
                        labelResId = feature.labelResId,
                        iconResId = feature.iconResId,
                        color = feature.color,
                        onClick = { onNavigate(feature.navKey) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size < 2) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "v${state.appVersion}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeatureCard(
    labelResId: Int,
    iconResId: Int,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = stringResource(labelResId),
                tint = color,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = stringResource(labelResId),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
