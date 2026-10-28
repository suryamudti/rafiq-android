package com.smiledev.rafiq_quran.ui.zakat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.appbar.RafiqTopAppBar
import com.smiledev.rafiq_quran.ui.designsystem.button.RafiqButton
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqCard
import com.smiledev.rafiq_quran.ui.designsystem.input.RafiqTextField
import com.smiledev.rafiq_quran.ui.designsystem.tab.RafiqSegmentedControl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatCalculatorScreen(
    onBack: () -> Unit,
    viewModel: ZakatCalculatorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            RafiqTopAppBar(
                title = stringResource(R.string.zakat_calculator),
                onBack = onBack
            )
        }
    ) { padding ->
        fun formatVal(value: Double): String {
            return if (state.selectedCurrency == "IDR") {
                "Rp%,.2f".format(value)
            } else {
                "$%,.2f".format(value)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = RafiqTheme.spacing.l)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(RafiqTheme.spacing.m))

            Text(
                text = stringResource(R.string.calculate_your_zakat),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(RafiqTheme.spacing.m))

            // Currency Selector using Emil Kowalski Segmented Control
            val currencies = listOf("USD", "IDR")
            val selectedIdx = currencies.indexOf(state.selectedCurrency).coerceAtLeast(0)
            RafiqSegmentedControl(
                tabs = currencies,
                selectedIndex = selectedIdx,
                onTabSelected = { viewModel.updateCurrency(currencies[it]) },
                modifier = Modifier.padding(bottom = RafiqTheme.spacing.m)
            )

            // Input Fields
            RafiqTextField(
                value = state.goldWeight,
                onValueChange = { viewModel.updateGold(it) },
                label = stringResource(R.string.zakat_gold_label),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(RafiqTheme.spacing.s))

            RafiqTextField(
                value = state.silverWeight,
                onValueChange = { viewModel.updateSilver(it) },
                label = stringResource(R.string.zakat_silver_label),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(RafiqTheme.spacing.s))

            RafiqTextField(
                value = state.cashAmount,
                onValueChange = { viewModel.updateCash(it) },
                label = stringResource(R.string.zakat_cash_label, state.selectedCurrency),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(RafiqTheme.spacing.l))

            RafiqButton(
                text = stringResource(R.string.calculate_zakat),
                onClick = { viewModel.calculate() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(RafiqTheme.spacing.m))

            if (state.isRefreshing) {
                Text(
                    text = stringResource(R.string.zakat_refreshing_prices),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = RafiqTheme.spacing.xs)
                )
            } else if (state.isUsingFallback) {
                Text(
                    text = stringResource(R.string.zakat_offline_prices),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = RafiqTheme.spacing.xs)
                )
            }

            val r = state.result
            RafiqCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = RafiqTheme.spacing.xl),
                shape = RafiqTheme.customShapes.large,
                border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder),
                contentPadding = RafiqTheme.spacing.l
            ) {
                Text(
                    text = stringResource(R.string.zakat_summary),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(RafiqTheme.spacing.s))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(RafiqTheme.spacing.s))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.zakat_gold_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (r.goldZakat > 0) formatVal(r.goldZakat) else stringResource(R.string.zakat_gold_below_nisab),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (r.goldZakat > 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (r.goldZakat > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(RafiqTheme.spacing.xs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.zakat_silver_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (r.silverZakat > 0) formatVal(r.silverZakat) else stringResource(R.string.zakat_silver_below_nisab),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (r.silverZakat > 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (r.silverZakat > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(RafiqTheme.spacing.xs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.zakat_cash_label, state.selectedCurrency),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (r.cashZakat > 0) formatVal(r.cashZakat) else stringResource(R.string.zakat_cash_below_nisab),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (r.cashZakat > 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (r.cashZakat > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(RafiqTheme.spacing.s))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(RafiqTheme.spacing.s))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.zakat_total_due, ""),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatVal(r.totalZakat),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (r.goldPricePerGram > 0) {
                    Spacer(Modifier.height(RafiqTheme.spacing.xs))
                    Text(
                        text = stringResource(
                            R.string.zakat_prices_summary,
                            formatVal(r.goldPricePerGram),
                            formatVal(r.silverPricePerGram)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
