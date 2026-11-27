package com.example.cashlog.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType { INITIAL, EXPENSE, INCOME, ADJUSTMENT }

@Entity(tableName = "transactions")
data class CashTransaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: TransactionType,
    val counts: Map<Denomination, Int>,
    val total: Long,
    val memo: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
