package com.smiledev.rafiq_quran.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import com.smiledev.rafiq_quran.AsmaulHusna
import com.smiledev.rafiq_quran.BookmarkList
import com.smiledev.rafiq_quran.HadithBooks
import com.smiledev.rafiq_quran.PrayerLog
import com.smiledev.rafiq_quran.Prophets
import com.smiledev.rafiq_quran.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigate: (NavKey) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    Text(stringResource(R.string.back), modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.theme),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            RadioButtonOption("system", R.string.system_default, state.themeMode, viewModel::setThemeMode)
            RadioButtonOption("light", R.string.light, state.themeMode, viewModel::setThemeMode)
            RadioButtonOption("dark", R.string.dark, state.themeMode, viewModel::setThemeMode)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.quran_translation),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            RadioButtonOption("system", R.string.system_default, state.translationLanguage, viewModel::setTranslationLanguage)
            RadioButtonOption("id", R.string.bahasa_indonesia, state.translationLanguage, viewModel::setTranslationLanguage)
            RadioButtonOption("en", R.string.english, state.translationLanguage, viewModel::setTranslationLanguage)
            RadioButtonOption("both", R.string.both_bahasa_english, state.translationLanguage, viewModel::setTranslationLanguage)

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            var expanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.more_features),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (expanded) "▼" else "▶",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    MoreFeatureItem(R.string.asmaul_husna) { onNavigate(AsmaulHusna) }
                    MoreFeatureItem(R.string.prophets) { onNavigate(Prophets) }
                    MoreFeatureItem(R.string.bookmarks) { onNavigate(BookmarkList) }
                    MoreFeatureItem(R.string.prayer_log) { onNavigate(PrayerLog) }
                    MoreFeatureItem(R.string.hadiths) { onNavigate(HadithBooks) }
                }
            }
        }
    }
}

@Composable
private fun MoreFeatureItem(
    labelResId: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(labelResId),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun RadioButtonOption(
    value: String,
    labelResId: Int,
    current: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(value) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = current == value,
            onClick = { onSelect(value) }
        )
        Text(
            text = stringResource(labelResId),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
