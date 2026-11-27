package com.example.cashlog.data

import com.example.cashlog.model.CashTransaction
import com.example.cashlog.model.Denomination
import com.example.cashlog.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WalletRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<CashTransaction>> = transactionDao.getAllTransactions()

    fun getWalletState(): Flow<Map<Denomination, Int>> {
        return allTransactions.map { transactions ->
            calculateWalletState(transactions)
        }
    }

    fun calculateWalletState(transactions: List<CashTransaction>): Map<Denomination, Int> {
        val state = mutableMapOf<Denomination, Int>().apply {
            for (denom in Denomination.values()) {
                put(denom, 0)
            }
        }

        val sortedTransactions = transactions.sortedBy { it.timestamp }

        for (transaction in sortedTransactions) {
            when (transaction.type) {
                // FIX: Treat INITIAL and ADJUSTMENT as additive deltas, not absolute resets.
                TransactionType.INITIAL, TransactionType.ADJUSTMENT, TransactionType.INCOME -> {
                    for (entry in transaction.counts.entries) {
                        state[entry.key] = (state[entry.key] ?: 0) + entry.value
                    }
                }
                TransactionType.EXPENSE -> {
                    for (entry in transaction.counts.entries) {
                        state[entry.key] = (state[entry.key] ?: 0) - entry.value
                    }
                }
            }
        }
        return state
    }

    suspend fun insertTransaction(transaction: CashTransaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: CashTransaction) {
        transactionDao.deleteTransaction(transaction)
    }
    
    suspend fun deleteTransactionById(id: String) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun hasInitialTransaction(): Boolean {
        return transactionDao.hasInitialTransaction()
    }

   suspend fun getLastResetTimestamp(transactions: List<CashTransaction>): Long? {
       return transactions
           .filter { it.type == TransactionType.INITIAL || it.type == TransactionType.ADJUSTMENT }
           .maxByOrNull { it.timestamp }
           ?.timestamp
   }
   suspend fun getAllTransactionsSnapshot(): List<CashTransaction> {
       return transactionDao.getAllTransactionsSnapshot()
   }
}
