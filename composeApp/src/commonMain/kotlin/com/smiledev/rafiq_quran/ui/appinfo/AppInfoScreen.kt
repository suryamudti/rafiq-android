package com.smiledev.rafiq_quran.ui.appinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBadge
import com.smiledev.rafiq_quran.ui.components.RafiqSharedCard
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeader
import com.smiledev.rafiq_quran.ui.theme.RafiqMultiplatformTheme

@Composable
fun AppInfoScreen(
    appVersion: String = "1.0.0",
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    RafiqMultiplatformTheme {
        Scaffold(
            topBar = {
                RafiqSharedHeader(
                    title = "About Rafiq",
                    subtitle = "Companion for Quran & Daily Worship"
                )
            },
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Branding Card
                RafiqSharedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Rafiq Quran & Worship",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Version $appVersion",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        RafiqSharedBadge(text = "Multiplatform")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "A modern, privacy-respecting Islamic application providing complete Quran reading, audio recitations, accurate prayer times, Hadith library, Qibla compass, and daily dhikr tools.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Architecture Card
                RafiqSharedCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Kotlin Multiplatform Architecture",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Powered by Compose Multiplatform, Koin DI, Ktor Networking, and shared domain & data layers across Android and iOS.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RafiqSharedBadge(text = "Compose Multiplatform")
                        RafiqSharedBadge(text = "Koin DI")
                        RafiqSharedBadge(text = "Ktor")
                    }
                }

                // Features Card
                RafiqSharedCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Core Capabilities",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val features = listOf(
                        "114 Surahs with translations & offline audio",
                        "Accurate prayer times with custom calculation methods",
                        "Comprehensive Hadith collections (Bukhari, Muslim, etc.)",
                        "Digital Tasbih counter with history tracking",
                        "Qibla direction finder and nearby mosques",
                        "Zakat calculator with real-time precious metal rates"
                    )

                    features.forEachIndexed { index, feature ->
                        Text(
                            text = "•  $feature",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        if (index < features.lastIndex) {
                            HorizontalDivider(
                                color = DividerDefaults.color.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
