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
import com.example.cashlog.model.TransactionType
import com.example.cashlog.ui.components.KeypadDenominationGrid
import java.text.NumberFormat
import java.util.*

@Composable
fun AdjustmentScreen(
    viewModel: CashLogViewModel,
    onAdjustmentComplete: () -> Unit
) {
    var counts by remember { mutableStateOf<Map<Denomination, Int>>(Denomination.values().associateWith { 0 }) }
    var isAdjustment by remember { mutableStateOf(false) }
    val defaultPreset by viewModel.defaultAdjustmentPreset.collectAsState(initial = emptyMap())
    val walletState by viewModel.walletState.collectAsState()
    
    LaunchedEffect(Unit) {
        isAdjustment = viewModel.hasInitialTransaction()
    }

    // Calculate Diffs: Input Value - Current Local Count
    val diffs = remember(counts, walletState) {
        Denomination.values().associateWith { denom ->
            (counts[denom] ?: 0) - (walletState[denom] ?: 0)
        }
    }

    var targetTotal = 0L
    var deltaTotal = 0L
    for (denom in Denomination.values()) {
        targetTotal += denom.faceValue.toLong() * (counts[denom] ?: 0)
        deltaTotal += denom.faceValue.toLong() * (diffs[denom] ?: 0)
    }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.KOREA)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isAdjustment) "Manual Reconciliation" else "Initial Setup",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            
            if (defaultPreset.isNotEmpty()) {
                TextButton(onClick = { counts = defaultPreset }) {
                    Text("Load Preset")
                }
            }
        }
        
        Text(
            text = if (isAdjustment) 
                "Enter target cash amounts. Diffs (+/-) show changes needed." 
                else "Enter your starting on-hand cash.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        KeypadDenominationGrid(
            counts = counts,
            onCountChange = { denom, count ->
                val newMap = HashMap(counts)
                newMap[denom] = count
                counts = newMap
            },
            modifier = Modifier.weight(1f),
            diffs = if (isAdjustment) diffs else null
        )

        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Target Total: ${currencyFormatter.format(targetTotal)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isAdjustment) {
                    Text(
                        text = "Net Change (Delta): ${if (deltaTotal >= 0) "+" else ""}${currencyFormatter.format(deltaTotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (deltaTotal >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Button(
            onClick = {
                // CRITICAL FIX: Pass 'diffs' (Delta) for saving, not absolute target 'counts'
                viewModel.setPendingCounts(if (isAdjustment) diffs else counts)
                viewModel.saveTransaction(if (isAdjustment) TransactionType.ADJUSTMENT else TransactionType.INITIAL)
                onAdjustmentComplete()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = targetTotal > 0 || (isAdjustment && deltaTotal != 0L)
        ) {
            Text(if (isAdjustment) "Save Reconciliation" else "Finish Setup")
        }
    }
}
