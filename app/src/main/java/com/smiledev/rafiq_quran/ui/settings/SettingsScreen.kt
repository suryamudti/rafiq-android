package com.smiledev.rafiq_quran.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import com.smiledev.rafiq_quran.AppInfo
import com.smiledev.rafiq_quran.AsmaulHusna
import com.smiledev.rafiq_quran.BookmarkList
import com.smiledev.rafiq_quran.HadithBooks
import com.smiledev.rafiq_quran.Hifz
import com.smiledev.rafiq_quran.PrayerLog
import com.smiledev.rafiq_quran.Prophets
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.Sources
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.components.AppUpdateDialog
import com.smiledev.rafiq_quran.ui.designsystem.animation.EaseInOutCubic
import com.smiledev.rafiq_quran.ui.designsystem.animation.EaseOutCubic
import com.smiledev.rafiq_quran.ui.designsystem.animation.RafiqMotion
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqPressable
import com.smiledev.rafiq_quran.ui.designsystem.appbar.RafiqTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigate: (NavKey) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val upToDateMsg = stringResource(R.string.up_to_date_desc, "v${state.appVersion}")
    val updateErrorMsg = stringResource(R.string.update_check_failed)

    LaunchedEffect(state.updateStatus) {
        when (state.updateStatus) {
            UpdateCheckStatus.UP_TO_DATE -> {
                snackbarHostState.showSnackbar(upToDateMsg)
                viewModel.clearUpdateStatus()
            }
            UpdateCheckStatus.ERROR -> {
                snackbarHostState.showSnackbar(updateErrorMsg)
                viewModel.clearUpdateStatus()
            }
            null -> Unit
        }
    }

    if (state.updateInfo != null) {
        AppUpdateDialog(
            updateInfo = state.updateInfo!!,
            onDismiss = viewModel::dismissUpdateDialog
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RafiqTopAppBar(
                title = stringResource(R.string.settings),
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = RafiqTheme.spacing.l)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(RafiqTheme.spacing.s))

            // 1. Theme Section
            SettingsSectionHeader(title = stringResource(R.string.theme))
            SettingsGroupedCard {
                RadioButtonOption(
                    value = "system",
                    labelResId = R.string.system_default,
                    current = state.themeMode,
                    onSelect = viewModel::setThemeMode
                )
                SettingsDivider()
                RadioButtonOption(
                    value = "light",
                    labelResId = R.string.light,
                    current = state.themeMode,
                    onSelect = viewModel::setThemeMode
                )
                SettingsDivider()
                RadioButtonOption(
                    value = "dark",
                    labelResId = R.string.dark,
                    current = state.themeMode,
                    onSelect = viewModel::setThemeMode
                )
            }

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.l))

            // 2. Quran & Translation Section
            SettingsSectionHeader(title = stringResource(R.string.quran_translation))
            SettingsGroupedCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.m),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.show_translation),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.showAyahTranslation,
                        onCheckedChange = viewModel::setShowAyahTranslation
                    )
                }
                SettingsDivider()
                val onLanguageSelected: (String) -> Unit = { lang ->
                    viewModel.setTranslationLanguage(lang)
                }
                RadioButtonOption("system", R.string.system_default, state.translationLanguage, onLanguageSelected)
                SettingsDivider()
                RadioButtonOption("id", R.string.bahasa_indonesia, state.translationLanguage, onLanguageSelected)
                SettingsDivider()
                RadioButtonOption("en", R.string.english, state.translationLanguage, onLanguageSelected)
                SettingsDivider()
                RadioButtonOption("both", R.string.both_bahasa_english, state.translationLanguage, onLanguageSelected)
            }

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.l))

            // 3. Dashboard Customization
            SettingsSectionHeader(title = stringResource(R.string.dashboard_customize_title))
            SettingsGroupedCard {
                dashboardToggleItems.forEachIndexed { index, (key, labelResId) ->
                    DashboardToggleRow(
                        labelResId = labelResId,
                        checked = key !in state.hiddenKeys,
                        onCheckedChange = { viewModel.toggleDashboardKey(key) }
                    )
                    if (index < dashboardToggleItems.lastIndex) {
                        SettingsDivider()
                    }
                }
                SettingsDivider()
                val resetInteraction = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .rafiqPressable(
                            interactionSource = resetInteraction,
                            targetScale = RafiqMotion.CardPressScale
                        )
                        .clickable(
                            interactionSource = resetInteraction,
                            indication = null,
                            onClick = { viewModel.resetDashboard() }
                        )
                        .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.m),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.reset_defaults),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.l))

            // 4. More Features
            var expanded by remember { mutableStateOf(false) }
            val expandInteraction = remember { MutableInteractionSource() }
            SettingsGroupedCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .rafiqPressable(
                            interactionSource = expandInteraction,
                            targetScale = RafiqMotion.CardPressScale
                        )
                        .clickable(
                            interactionSource = expandInteraction,
                            indication = null,
                            onClick = { expanded = !expanded }
                        )
                        .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.m),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.more_features),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (expanded) "▲" else "▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(animationSpec = tween(RafiqMotion.DurationNormal, easing = EaseOutCubic)) +
                            expandVertically(animationSpec = tween(RafiqMotion.DurationNormal, easing = EaseOutCubic)),
                    exit = fadeOut(animationSpec = tween(RafiqMotion.DurationFast, easing = EaseInOutCubic)) +
                            shrinkVertically(animationSpec = tween(RafiqMotion.DurationFast, easing = EaseInOutCubic))
                ) {
                    Column {
                        SettingsDivider()
                        MoreFeatureItem(R.string.asmaul_husna) { onNavigate(AsmaulHusna) }
                        SettingsDivider()
                        MoreFeatureItem(R.string.prophets) { onNavigate(Prophets) }
                        SettingsDivider()
                        MoreFeatureItem(R.string.bookmarks) { onNavigate(BookmarkList) }
                        SettingsDivider()
                        MoreFeatureItem(R.string.prayer_log) { onNavigate(PrayerLog) }
                        SettingsDivider()
                        MoreFeatureItem(R.string.hadiths) { onNavigate(HadithBooks) }
                        SettingsDivider()
                        MoreFeatureItem(R.string.hifz_memorization) { onNavigate(Hifz) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.l))

            // 5. About & Updates
            SettingsSectionHeader(title = stringResource(R.string.about_app))
            SettingsGroupedCard {
                MoreFeatureItem(R.string.sources_and_authenticity) { onNavigate(Sources) }
                SettingsDivider()
                MoreFeatureItem(R.string.about_app) { onNavigate(AppInfo) }
                SettingsDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.m),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.app_version),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "v${state.appVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SettingsDivider()
                val updateInteraction = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .rafiqPressable(
                            interactionSource = updateInteraction,
                            targetScale = RafiqMotion.CardPressScale,
                            enabled = !state.isCheckingUpdate
                        )
                        .clickable(
                            interactionSource = updateInteraction,
                            indication = null,
                            enabled = !state.isCheckingUpdate,
                            onClick = { viewModel.checkForUpdate() }
                        )
                        .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.m),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.check_for_updates),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (state.isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.xxl))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.1).sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = RafiqTheme.spacing.xs,
            bottom = RafiqTheme.spacing.xs
        )
    )
}

@Composable
private fun SettingsGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RafiqTheme.customShapes.large,
        border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = RafiqTheme.elevation.low)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        thickness = 1.dp
    )
}

@Composable
private fun MoreFeatureItem(
    labelResId: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .rafiqPressable(
                interactionSource = interactionSource,
                targetScale = RafiqMotion.CardPressScale
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(labelResId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "›",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .rafiqPressable(
                interactionSource = interactionSource,
                targetScale = RafiqMotion.CardPressScale
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onSelect(value) }
            )
            .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = current == value,
            onClick = { onSelect(value) }
        )
        Text(
            text = stringResource(labelResId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = RafiqTheme.spacing.s)
        )
    }
}

private val dashboardToggleItems: List<Pair<String, Int>> = listOf(
    "quran" to R.string.quran,
    "prayer_times" to R.string.prayer_times,
    "qibla" to R.string.qibla,
    "tasbih" to R.string.tasbih,
    "hadith" to R.string.hadiths,
    "prophets" to R.string.prophets,
    "asmaul_husna" to R.string.asmaul_husna,
    "recitation" to R.string.recitations,
    "mosques" to R.string.mosques,
    "calendar" to R.string.calendar,
    "zakat" to R.string.zakat,
    "prayer_log" to R.string.prayer_log,
    "prayer_guidance" to R.string.prayer_guidance,
    "sunnah_guidance" to R.string.sunnah_guidance,
    "hifz" to R.string.hifz_memorization
)

@Composable
private fun DashboardToggleRow(
    labelResId: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(labelResId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
