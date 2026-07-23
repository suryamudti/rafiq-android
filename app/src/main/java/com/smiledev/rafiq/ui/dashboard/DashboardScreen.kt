package com.smiledev.rafiq.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.navigation3.runtime.NavKey
import com.smiledev.rafiq.AsmaulHusna
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
import com.smiledev.rafiq.BookmarkList
import com.smiledev.rafiq.PrayerLog

private data class FeatureItem(
    val labelResId: Int,
    val navKey: NavKey,
    val icon: ImageVector,
    val color: Color
)

private val features = listOf(
    FeatureItem(R.string.quran, Quran(), Icons.AutoMirrored.Filled.List, Color(0xFF3F51B5)),
    FeatureItem(R.string.prayer_times, PrayerTimes, Icons.Filled.Notifications, Color(0xFF009688)),
    FeatureItem(R.string.qibla, Qibla, Icons.Filled.LocationOn, Color(0xFFFFC107)),
    FeatureItem(R.string.mosques, Mosques, Icons.Filled.Place, Color(0xFF4CAF50)),
    FeatureItem(R.string.prophets, Prophets, Icons.Filled.Person, Color(0xFF795548)),
    FeatureItem(R.string.recitations, Recitation, Icons.Filled.PlayArrow, Color(0xFF2196F3)),
    FeatureItem(R.string.calendar, IslamicCalendar, Icons.Filled.DateRange, Color(0xFF009688)),
    FeatureItem(R.string.zakat, ZakatCalculator, Icons.Filled.ShoppingCart, Color(0xFFFF9800)),
    FeatureItem(R.string.asmaul_husna, AsmaulHusna, Icons.Filled.Star, Color(0xFF3F51B5)),
    FeatureItem(R.string.tasbih, Tasbih, Icons.Filled.Refresh, Color(0xFF009688)),
    FeatureItem(R.string.bookmarks, BookmarkList, Icons.Filled.Favorite, Color(0xFFE91E63)),
    FeatureItem(R.string.prayer_log, PrayerLog, Icons.Filled.Notifications, Color(0xFF795548)),
)

@Composable
fun DashboardScreen(
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onNavigate(Settings) }) {
                Icon(
                    imageVector = Icons.Filled.Face,
                    contentDescription = stringResource(R.string.settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
            Text(
                text = stringResource(R.string.your_islamic_companion),
                style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(features) { feature ->
                FeatureCard(
                    labelResId = feature.labelResId,
                    icon = feature.icon,
                    color = feature.color,
                    onClick = { onNavigate(feature.navKey) }
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    labelResId: Int,
    icon: ImageVector,
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
                imageVector = icon,
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
