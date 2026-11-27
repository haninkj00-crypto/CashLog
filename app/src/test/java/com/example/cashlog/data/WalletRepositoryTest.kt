package com.example.cashlog.data

import com.example.cashlog.model.CashTransaction
import com.example.cashlog.model.Denomination
import com.example.cashlog.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class WalletRepositoryTest {

    private fun createTx(
        type: TransactionType,
        counts: Map<Denomination, Int>,
        timestamp: Long
    ): CashTransaction {
        var total = 0L
        for (entry in counts.entries) {
            total += entry.key.faceValue.toLong() * entry.value
        }
        return CashTransaction(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            type = type,
            counts = counts,
            total = total
        )
    }

    @Test
    fun testWalletReplayLogic() {
        val repo = WalletRepository(FakeTransactionDao())

        val transactions = listOf(
            createTx(TransactionType.INITIAL, mapOf(Denomination.W50000 to 2), 1000),
            createTx(TransactionType.INCOME, mapOf(Denomination.W10000 to 1), 2000),
            createTx(TransactionType.EXPENSE, mapOf(Denomination.W5000 to 1), 3000),
            createTx(TransactionType.ADJUSTMENT, mapOf(Denomination.W50000 to 1), 4000)
        )

        val finalState = repo.calculateWalletState(transactions)

        assertEquals(1, finalState[Denomination.W50000] ?: 0)
        assertEquals(1, finalState[Denomination.W10000] ?: 0)
        assertEquals(-1, finalState[Denomination.W5000] ?: 0)
    }

    @Test
    fun testMixedIncomeExpense() {
        val repo = WalletRepository(FakeTransactionDao())

        val transactions = listOf(
            createTx(TransactionType.INITIAL, mapOf(Denomination.C100 to 10), 1000),
            createTx(TransactionType.EXPENSE, mapOf(Denomination.C100 to 3), 2000),
            createTx(TransactionType.INCOME, mapOf(Denomination.C100 to 5), 3000)
        )

        val finalState = repo.calculateWalletState(transactions)
        assertEquals(12, finalState[Denomination.C100] ?: 0)
    }

    class FakeTransactionDao : TransactionDao {
        override fun getAllTransactions(): Flow<List<CashTransaction>> = flowOf(emptyList())
        override suspend fun getTransactionById(id: String) = throw UnsupportedOperationException()
        override suspend fun insertTransaction(transaction: CashTransaction) {}
        override suspend fun updateTransaction(transaction: CashTransaction) {}
        override suspend fun deleteTransaction(transaction: CashTransaction) {}
        override suspend fun deleteTransactionById(id: String) {}
        override suspend fun hasInitialTransaction() = false
    }
}
