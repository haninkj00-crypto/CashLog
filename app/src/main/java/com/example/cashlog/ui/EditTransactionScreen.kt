package com.example.cashlog.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cashlog.CashLogViewModel
import com.example.cashlog.model.Denomination
import com.example.cashlog.model.TransactionType
import com.example.cashlog.ui.components.KeypadDenominationGrid
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    transactionId: String,
    viewModel: CashLogViewModel,
    onNavigateBack: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val walletState by viewModel.walletState.collectAsState()
    val transaction = transactions.find { it.id == transactionId }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showInventory by remember { mutableStateOf(false) }

    if (transaction == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Transaction not found")
        }
        return
    }

    var editedCounts by remember { mutableStateOf<Map<Denomination, Int>>(transaction.counts) }
    var editedMemo by remember { mutableStateOf(transaction.memo ?: "") }
    var editedType by remember { mutableStateOf(transaction.type) }
    var isAdjustment by remember { mutableStateOf(transaction.type == TransactionType.ADJUSTMENT || transaction.type == TransactionType.INITIAL) }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.KOREA)

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction? This will affect the current wallet balance.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(transaction)
                    onNavigateBack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Collapsible Inventory Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showInventory = !showInventory },
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Current Wallet State",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Icon(
                            if (showInventory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    
                    AnimatedVisibility(visible = showInventory) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
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

            // Type Selector & Adjustment Flag
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
                    for (type in types) {
                        FilterChip(
                            selected = editedType == type && !isAdjustment,
                            onClick = { 
                                editedType = type
                                isAdjustment = false
                            },
                            label = { Text(type.name) },
                            enabled = !isAdjustment
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isAdjustment,
                        onCheckedChange = { 
                            isAdjustment = it
                            if (it) editedType = TransactionType.ADJUSTMENT
                        }
                    )
                    Text("Is Reconcile", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Keypad Grid for direct input during edit
            KeypadDenominationGrid(
                counts = editedCounts,
                onCountChange = { denom, count ->
                    val newMap = HashMap(editedCounts)
                    newMap[denom] = count
                    editedCounts = newMap
                },
                modifier = Modifier.weight(1f)
            )

            TextField(
                value = editedMemo,
                onValueChange = { editedMemo = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                label = { Text("Memo") }
            )

            Button(
                onClick = {
                    var total = 0L
                    for (denom in Denomination.values()) {
                        total += denom.faceValue.toLong() * (editedCounts[denom] ?: 0)
                    }
                    val updated = transaction.copy(
                        type = if (isAdjustment) TransactionType.ADJUSTMENT else editedType,
                        counts = editedCounts,
                        total = total,
                        memo = editedMemo.ifBlank { null },
                        updatedAt = System.currentTimeMillis()
                    )
                    viewModel.updateTransaction(updated)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Save Changes")
            }
        }
    }
}
