package com.example.cashlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashlog.CashLogViewModel
import com.example.cashlog.model.Denomination
import com.example.cashlog.ui.components.KeypadDenominationGrid
import java.text.NumberFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: CashLogViewModel
) {
    val defaultPreset by viewModel.defaultAdjustmentPreset.collectAsState(initial = emptyMap())
    var editedPreset by remember(defaultPreset) { mutableStateOf(defaultPreset) }
    
    val total = editedPreset.entries.sumOf { it.key.faceValue.toLong() * it.value }
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.KOREA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Define your default daily-reset cash amounts. This preset can be applied quickly in the Reconcile screen.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        KeypadDenominationGrid(
            counts = editedPreset,
            onCountChange = { denom, count ->
                val newMap = editedPreset.toMutableMap()
                newMap[denom] = count
                editedPreset = newMap
            },
            modifier = Modifier.weight(1f)
        )

        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = "Preset Total: ${currencyFormatter.format(total)}",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Button(
            onClick = {
                viewModel.saveDefaultAdjustmentPreset(editedPreset)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Preset")
        }
    }
}
