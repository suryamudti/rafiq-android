package com.smiledev.rafiq.ui.zakat

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatCalculatorScreen(
    onBack: () -> Unit,
    viewModel: ZakatCalculatorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zakat Calculator") },
                navigationIcon = {
                    Text("Back", modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        val currencySymbol = if (state.selectedCurrency == "IDR") "Rp" else "$"
        fun formatVal(value: Double): String {
            return if (state.selectedCurrency == "IDR") {
                "Rp%,.2f".format(value)
            } else {
                "$%,.2f".format(value)
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Calculate Your Zakat",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                listOf("USD", "IDR").forEach { curr ->
                    val isSelected = state.selectedCurrency == curr
                    if (isSelected) {
                        Button(onClick = { viewModel.updateCurrency(curr) }) {
                            Text(curr)
                        }
                    } else {
                        androidx.compose.material3.OutlinedButton(onClick = { viewModel.updateCurrency(curr) }) {
                            Text(curr)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.goldWeight,
                onValueChange = { viewModel.updateGold(it) },
                label = { Text("Gold (grams)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.silverWeight,
                onValueChange = { viewModel.updateSilver(it) },
                label = { Text("Silver (grams)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.cashAmount,
                onValueChange = { viewModel.updateCash(it) },
                label = { Text("Cash & Savings (${state.selectedCurrency})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { viewModel.calculate() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calculate Zakat")
            }

            Spacer(Modifier.height(16.dp))

            when {
                state.isLoading -> CircularProgressIndicator()
                state.error != null -> Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                else -> {
                    val r = state.result
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Zakat Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            if (r.goldZakat > 0) {
                                Text("Gold Zakat: ${formatVal(r.goldZakat)}")
                            } else {
                                Text("Gold: Below nisab (85g)")
                            }
                            if (r.silverZakat > 0) {
                                Text("Silver Zakat: ${formatVal(r.silverZakat)}")
                            } else {
                                Text("Silver: Below nisab (595g)")
                            }
                            if (r.cashZakat > 0) {
                                Text("Cash Zakat: ${formatVal(r.cashZakat)}")
                            } else {
                                Text("Cash: Below nisab threshold")
                            }
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Total Zakat Due: ${formatVal(r.totalZakat)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF009688)
                            )
                            if (r.goldPricePerGram > 0) {
                                Text(
                                    text = "Gold price: ${formatVal(r.goldPricePerGram)}/g | Silver: ${formatVal(r.silverPricePerGram)}/g",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
