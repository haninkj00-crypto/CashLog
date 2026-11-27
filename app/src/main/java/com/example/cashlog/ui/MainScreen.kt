package com.example.cashlog.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashlog.CashLogViewModel
import com.example.cashlog.model.Denomination
import com.example.cashlog.model.TransactionType
import com.example.cashlog.ui.theme.IncomeRed
import com.example.cashlog.ui.theme.ExpenseBlue
import java.text.NumberFormat
import java.util.*

@Composable
fun MainScreen(
    viewModel: CashLogViewModel,
    onSave: (TransactionType) -> Unit
) {
    val walletState by viewModel.walletState.collectAsState()
    val pendingCounts by viewModel.pendingCounts.collectAsState()
    val memo by viewModel.memo.collectAsState()
    
    var walletTotal = 0L
    for (denom in Denomination.values()) {
        walletTotal += denom.faceValue.toLong() * (walletState[denom] ?: 0)
    }
    
    var pendingTotal = 0L
    for (denom in Denomination.values()) {
        pendingTotal += denom.faceValue.toLong() * (pendingCounts[denom] ?: 0)
    }
    
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.KOREA)
    var showInventory by remember { mutableStateOf(false) }

    val willBeNegative = Denomination.values().any { denom ->
        val currentCount = walletState[denom] ?: 0
        val pendingCount = pendingCounts[denom] ?: 0
        currentCount - pendingCount < 0
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Collapsible Inventory Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showInventory = !showInventory },
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Wallet total: ${currencyFormatter.format(walletTotal)}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Icon(
                        if (showInventory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
                
                AnimatedVisibility(visible = showInventory) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text(
                            text = "Current Inventory:",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        // Show all 9 in a small summary
                        Denomination.values().toList().chunked(3).forEach { rowDenoms ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                rowDenoms.forEach { d ->
                                    Text(
                                        text = "${d.label}: ${walletState[d] ?: 0}",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // §4: 3x3 Grid of Denominations
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Denomination.values()) { denom ->
                val count = pendingCounts[denom] ?: 0
                DenominationCard(
                    denomination = denom,
                    count = count,
                    onIncrement = { viewModel.incrementPending(denom) },
                    onDecrement = { viewModel.decrementPending(denom) },
                    currencyFormatter = currencyFormatter
                )
            }
        }

        if (willBeNegative) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = IncomeRed.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "⚠️ Caution: Expense exceeds inventory.",
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = "Pending total: ${currencyFormatter.format(pendingTotal)}",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        TextField(
            value = memo,
            onValueChange = { viewModel.updateMemo(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Memo (optional)") },
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onSave(TransactionType.INCOME) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = IncomeRed),
                enabled = pendingTotal > 0
            ) {
                Text("수입 Income")
            }
            Button(
                onClick = { onSave(TransactionType.EXPENSE) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseBlue),
                enabled = pendingTotal > 0
            ) {
                Text("지출 Expense")
            }
        }
        
        TextButton(
            onClick = { viewModel.clearPending() },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        ) {
            Text("Clear")
        }
    }
}

@Composable
fun DenominationCard(
    denomination: Denomination,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    currencyFormatter: NumberFormat
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onIncrement() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Label on top
            Text(
                text = denomination.label,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                style = MaterialTheme.typography.labelMedium
            )

            // Large pending count in middle
            Text(
                text = "$count",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )

            // Value at bottom
            Text(
                text = currencyFormatter.format(denomination.faceValue.toLong() * count),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            // §3: Visually minor decrement button
            IconButton(
                onClick = { onDecrement() },
                enabled = count > 0,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    Icons.Default.Remove, 
                    contentDescription = "Decrease",
                    modifier = Modifier.size(16.dp),
                    tint = if (count > 0) MaterialTheme.colorScheme.error else Color.Gray
                )
            }
        }
    }
}
