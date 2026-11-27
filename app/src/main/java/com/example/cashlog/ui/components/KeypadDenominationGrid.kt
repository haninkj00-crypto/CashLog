package com.example.cashlog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashlog.model.Denomination
import java.text.NumberFormat
import java.util.*

@Composable
fun KeypadDenominationGrid(
    counts: Map<Denomination, Int>,
    onCountChange: (Denomination, Int) -> Unit,
    modifier: Modifier = Modifier,
    diffs: Map<Denomination, Int>? = null
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.KOREA)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(Denomination.values()) { denom ->
            val count = counts[denom] ?: 0
            val diff = diffs?.get(denom)
            KeypadDenominationCard(
                denomination = denom,
                count = count,
                onCountChange = { onCountChange(denom, it) },
                currencyFormatter = currencyFormatter,
                diff = diff
            )
        }
    }
}

@Composable
fun KeypadDenominationCard(
    denomination: Denomination,
    count: Int,
    onCountChange: (Int) -> Unit,
    currencyFormatter: NumberFormat,
    diff: Int? = null
) {
    var textValue by remember(count) { mutableStateOf(count.toString()) }

    Card(
        modifier = Modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = denomination.label,
                    style = MaterialTheme.typography.labelMedium
                )
                if (diff != null && diff != 0) {
                    Text(
                        text = if (diff > 0) " (+$diff)" else " ($diff)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (diff > 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        textValue = newValue
                        val newCount = newValue.toIntOrNull() ?: 0
                        onCountChange(newCount)
                    }
                },
                modifier = Modifier.width(80.dp),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Text(
                text = currencyFormatter.format(denomination.faceValue.toLong() * count),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
