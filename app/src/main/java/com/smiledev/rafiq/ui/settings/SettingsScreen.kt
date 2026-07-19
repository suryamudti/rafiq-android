package com.smiledev.rafiq.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    Text("Back", modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
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
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            RadioButtonOption("system", "System default", state.themeMode, viewModel::setThemeMode)
            RadioButtonOption("light", "Light", state.themeMode, viewModel::setThemeMode)
            RadioButtonOption("dark", "Dark", state.themeMode, viewModel::setThemeMode)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Quran Translation",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            RadioButtonOption("system", "System default", state.translationLanguage, viewModel::setTranslationLanguage)
            RadioButtonOption("id", "Bahasa Indonesia", state.translationLanguage, viewModel::setTranslationLanguage)
            RadioButtonOption("en", "English", state.translationLanguage, viewModel::setTranslationLanguage)
            RadioButtonOption("both", "Both (Bahasa & English)", state.translationLanguage, viewModel::setTranslationLanguage)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Ayah Font Size: ${state.ayahFontSize}sp",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("12", fontSize = 12.sp, fontWeight = FontWeight.Light)
                Slider(
                    value = state.ayahFontSize.toFloat(),
                    onValueChange = { viewModel.setAyahFontSize(it.toInt()) },
                    valueRange = 12f..40f,
                    steps = 27,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text("40", fontSize = 12.sp, fontWeight = FontWeight.Light)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Translation Font Size: ${state.translationFontSize}sp",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("10", fontSize = 12.sp, fontWeight = FontWeight.Light)
                Slider(
                    value = state.translationFontSize.toFloat(),
                    onValueChange = { viewModel.setTranslationFontSize(it.toInt()) },
                    valueRange = 10f..30f,
                    steps = 19,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text("30", fontSize = 12.sp, fontWeight = FontWeight.Light)
            }
        }
    }
}

@Composable
private fun RadioButtonOption(
    value: String,
    label: String,
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
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
