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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBackButton
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBadge
import com.smiledev.rafiq_quran.ui.components.RafiqSharedCard
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeader
import com.smiledev.rafiq_quran.ui.theme.PrimaryTeal
import com.smiledev.rafiq_quran.ui.theme.RafiqMultiplatformTheme
import com.smiledev.rafiq_quran.ui.theme.SecondaryGold
import com.smiledev.rafiq_quran.ui.theme.rafiqPressable

@Composable
fun SharedZakatScreen(
    viewModel: SharedZakatViewModel = remember { SharedZakatViewModel() },
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    fun formatVal(value: Double): String {
        val longVal = value.toLong()
        val formatted = if (longVal >= 1000) {
            val str = longVal.toString()
            val result = StringBuilder()
            for (i in str.indices) {
                if (i > 0 && (str.length - i) % 3 == 0) {
                    result.append(',')
                }
                result.append(str[i])
            }
            result.toString()
        } else {
            longVal.toString()
        }
        val cents = ((value - longVal) * 100).toInt()
        val decimalStr = if (cents > 0) ".${cents.toString().padStart(2, '0')}" else ""

        return if (state.selectedCurrency == "IDR") "Rp$formatted$decimalStr" else "$$formatted$decimalStr"
    }

    Scaffold(
        topBar = {
            RafiqSharedHeader(
                title = "Zakat Calculator",
                subtitle = "Gold, Silver, and Wealth (Maal)",
                navigationIcon = onBackClick?.let { action ->
                    { RafiqSharedBackButton(onClick = action) }
                },
                actions = {
                    RafiqSharedBadge(
                        text = state.selectedCurrency,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.70f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Currency Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("USD", "IDR").forEach { curr ->
                    val isSelected = state.selectedCurrency == curr
                    if (isSelected) {
                        Button(
                            onClick = { viewModel.updateCurrency(curr) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .rafiqPressable(targetScale = 0.95f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(curr, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.updateCurrency(curr) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .rafiqPressable(targetScale = 0.95f)
                        ) {
                            Text(curr, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Asset Inputs
            RafiqSharedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                contentPadding = 18.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter Your Assets",
                        style = MaterialTheme.typography.titleMedium.copy(
                            letterSpacing = (-0.1).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = state.goldWeight,
                        onValueChange = { viewModel.updateGold(it) },
                        label = { Text("Gold Weight (grams)") },
                        placeholder = { Text("e.g. 100") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = state.silverWeight,
                        onValueChange = { viewModel.updateSilver(it) },
                        label = { Text("Silver Weight (grams)") },
                        placeholder = { Text("e.g. 600") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = state.cashAmount,
                        onValueChange = { viewModel.updateCash(it) },
                        label = { Text("Cash & Liquid Savings (${state.selectedCurrency})") },
                        placeholder = { Text("e.g. 5000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Button(
                        onClick = { viewModel.calculate() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .rafiqPressable(targetScale = 0.96f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Calculate Zakat", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            // Price Status Note
            if (state.isRefreshing) {
                Text(
                    text = "Refreshing live precious metal rates...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (state.isUsingFallback) {
                Text(
                    text = "Using standard reference rates (Nisab: 85g Gold / 595g Silver)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                )
            }

            // Zakat Summary Card
            val r = state.result
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, RafiqMultiplatformTheme.colors.cardBorder),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Calculation Summary",
                        style = MaterialTheme.typography.titleMedium.copy(
                            letterSpacing = (-0.1).sp
                        ),
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SummaryRow(
                        label = "Gold Zakat (85g Nisab):",
                        value = if (r.goldZakat > 0) formatVal(r.goldZakat) else "Below Nisab"
                    )
                    SummaryRow(
                        label = "Silver Zakat (595g Nisab):",
                        value = if (r.silverZakat > 0) formatVal(r.silverZakat) else "Below Nisab"
                    )
                    SummaryRow(
                        label = "Cash & Savings Zakat:",
                        value = if (r.cashZakat > 0) formatVal(r.cashZakat) else "Below Nisab"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Zakat Due:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatVal(r.totalZakat),
                            style = MaterialTheme.typography.titleLarge.copy(
                                letterSpacing = (-0.5).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }

                    if (r.goldPricePerGram > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rates: Gold ${formatVal(r.goldPricePerGram)}/g, Silver ${formatVal(r.silverPricePerGram)}/g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                        )
                    }
                }
            }

            // Info Notes
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.35f)),
                colors = CardDefaults.cardColors(containerColor = SecondaryGold.copy(alpha = 0.10f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "💡 Zakat Nisab Guidelines",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Zakat is obligatory at 2.5% on wealth that meets or exceeds the Nisab threshold held for a full lunar year (Hawl).",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
