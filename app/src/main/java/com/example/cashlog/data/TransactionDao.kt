package com.example.cashlog.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.cashlog.model.CashTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<CashTransaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSnapshot(): List<CashTransaction>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): CashTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CashTransaction)

    @Update
    suspend fun updateTransaction(transaction: CashTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: CashTransaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE type = 'INITIAL')")
    suspend fun hasInitialTransaction(): Boolean

    @Query("SELECT MAX(timestamp) FROM transactions WHERE type IN ('INITIAL', 'ADJUSTMENT')")
    suspend fun getLatestResetTimestamp(): Long?
}
