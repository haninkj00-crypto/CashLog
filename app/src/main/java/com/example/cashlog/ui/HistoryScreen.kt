package com.example.cashlog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashlog.CashLogViewModel
import com.example.cashlog.HistoryFilterType
import com.example.cashlog.model.CashTransaction
import com.example.cashlog.model.Denomination
import com.example.cashlog.model.TransactionType
import com.example.cashlog.ui.theme.IncomeRed
import com.example.cashlog.ui.theme.ExpenseBlue
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: CashLogViewModel,
    onEditTransaction: (String) -> Unit,
    onExport: () -> Unit
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val filterType by viewModel.historyFilterType.collectAsState()
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.KOREA)
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History") },
                actions = {
                    IconButton(onClick = onExport) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilterType.values().filter { it != HistoryFilterType.MANUAL }.forEach { type ->
                    FilterChip(
                        selected = filterType == type,
                        onClick = { viewModel.setHistoryFilterType(type) },
                        label = {
                            Text(
                                when (type) {
                                    HistoryFilterType.TODAY -> "Today"
                                    HistoryFilterType.SINCE_ADJUSTMENT -> "Since Reconcile"
                                    HistoryFilterType.ALL_TIME -> "All Time"
                                    else -> type.name
                                }
                            )
                        }
                    )
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(transactions) { tx ->
                    TransactionItem(
                        transaction = tx,
                        currencyFormatter = currencyFormatter,
                        dateFormatter = dateFormatter,
                        onClick = { onEditTransaction(tx.id) }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: CashTransaction,
    currencyFormatter: NumberFormat,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isAdjustment = transaction.type == TransactionType.ADJUSTMENT || transaction.type == TransactionType.INITIAL

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when (transaction.type) {
                    TransactionType.INCOME -> Icons.Default.ArrowUpward
                    TransactionType.EXPENSE -> Icons.Default.ArrowDownward
                    TransactionType.INITIAL -> Icons.Default.FiberManualRecord
                    TransactionType.ADJUSTMENT -> Icons.Default.SwapHoriz
                },
                contentDescription = null,
                tint = when (transaction.type) {
                    TransactionType.INCOME -> IncomeRed
                    TransactionType.EXPENSE -> ExpenseBlue
                    TransactionType.INITIAL -> MaterialTheme.colorScheme.outline
                    TransactionType.ADJUSTMENT -> MaterialTheme.colorScheme.secondary
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormatter.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = transaction.memo ?: when (transaction.type) {
                        TransactionType.INITIAL -> "Initial Balance"
                        TransactionType.ADJUSTMENT -> "Reconciliation"
                        else -> "No memo"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = if (isAdjustment) {
                    val prefix = if (transaction.total > 0) "Net: +" else if (transaction.total < 0) "Net: " else "Net: "
                    "$prefix${currencyFormatter.format(transaction.total)}"
                } else {
                    currencyFormatter.format(transaction.total)
                },
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    transaction.type == TransactionType.INCOME -> IncomeRed
                    transaction.type == TransactionType.EXPENSE -> ExpenseBlue
                    isAdjustment -> {
                        if (transaction.total > 0) IncomeRed
                        else if (transaction.total < 0) ExpenseBlue
                        else MaterialTheme.colorScheme.onSurface
                    }
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            
            for (denom in Denomination.values()) {
                val count = transaction.counts[denom] ?: 0
                if (count != 0) {
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 32.dp)) {
                        Text(text = denom.label, modifier = Modifier.weight(1f))
                        Text(
                            text = if (isAdjustment) {
                                val sign = if (count > 0) "+" else ""
                                "($sign$count)"
                            } else {
                                "x$count"
                            },
                            color = if (isAdjustment) {
                                if (count > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                            } else Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = currencyFormatter.format(denom.faceValue.toLong() * count))
                    }
                }
            }

            if (transaction.lat != null && transaction.lon != null) {
                Text(
                    text = "Location: ${transaction.lat}, ${transaction.lon}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 32.dp, top = 4.dp)
                )
            }

            TextButton(
                onClick = onClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Edit / Details")
            }
        }
    }
}
