package com.example.cashlog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cashlog.data.AppDatabase
import com.example.cashlog.data.SettingsRepository
import com.example.cashlog.data.WalletRepository
import com.example.cashlog.model.CashTransaction
import com.example.cashlog.model.Denomination
import com.example.cashlog.model.TransactionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class HistoryFilterType { TODAY, SINCE_ADJUSTMENT, ALL_TIME, MANUAL }
enum class SortOrder { ASCENDING, DESCENDING }

class CashLogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WalletRepository
    private val settingsRepository: SettingsRepository
    
    val allTransactions: StateFlow<List<CashTransaction>>
    val walletState: StateFlow<Map<Denomination, Int>>
    val defaultAdjustmentPreset: Flow<Map<Denomination, Int>>

    // History Filters
    private val _historyFilterType = MutableStateFlow(HistoryFilterType.ALL_TIME)
    val historyFilterType: StateFlow<HistoryFilterType> = _historyFilterType.asStateFlow()

    private val _manualDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val manualDateRange: StateFlow<Pair<Long, Long>?> = _manualDateRange.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val filteredTransactions: StateFlow<List<CashTransaction>>

    // Pending transaction state (Main Screen)
    private val _pendingCounts = MutableStateFlow<Map<Denomination, Int>>(
        Denomination.values().associateWith { 0 }
    )
    val pendingCounts: StateFlow<Map<Denomination, Int>> = _pendingCounts.asStateFlow()

    private val _memo = MutableStateFlow("")
    val memo: StateFlow<String> = _memo.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowUndoSnackbar(val transactionId: String) : UiEvent()
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = WalletRepository(database.transactionDao())
        settingsRepository = SettingsRepository(application)
        
        allTransactions = repository.allTransactions.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        
        walletState = repository.getWalletState().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), Denomination.values().associateWith { 0 }
        )

        defaultAdjustmentPreset = settingsRepository.defaultAdjustmentPreset

        filteredTransactions = combine(
            allTransactions,
            _historyFilterType,
            _manualDateRange,
            _sortOrder
        ) { txs, filterType, dateRange, sortOrder ->
            var list = when (filterType) {
                HistoryFilterType.TODAY -> {
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    txs.filter { it.timestamp >= todayStart }
                }
                HistoryFilterType.SINCE_ADJUSTMENT -> {
                    val lastReset = txs.filter { it.type == TransactionType.INITIAL || it.type == TransactionType.ADJUSTMENT }
                        .maxByOrNull { it.timestamp }?.timestamp ?: 0L
                    txs.filter { it.timestamp >= lastReset }
                }
                HistoryFilterType.ALL_TIME -> {
                    txs
                }
                HistoryFilterType.MANUAL -> {
                    if (dateRange != null) {
                        txs.filter { it.timestamp in dateRange.first..dateRange.second }
                    } else txs
                }
            }
            
            if (sortOrder == SortOrder.ASCENDING) {
                list.sortedBy { it.timestamp }
            } else {
                list.sortedByDescending { it.timestamp }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun setHistoryFilterType(type: HistoryFilterType) {
        _historyFilterType.value = type
    }

    fun setManualDateRange(start: Long, end: Long) {
        _manualDateRange.value = start to end
        _historyFilterType.value = HistoryFilterType.MANUAL
    }

    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
    }

    fun incrementPending(denomination: Denomination) {
        val current = _pendingCounts.value
        val newMap = current.toMutableMap()
        newMap[denomination] = (current[denomination] ?: 0) + 1
        _pendingCounts.value = newMap
    }

    fun decrementPending(denomination: Denomination) {
        val current = _pendingCounts.value
        val count = current[denomination] ?: 0
        if (count > 0) {
            val newMap = current.toMutableMap()
            newMap[denomination] = count - 1
            _pendingCounts.value = newMap
        }
    }

    fun setPendingCounts(counts: Map<Denomination, Int>) {
        _pendingCounts.value = counts
    }

    fun clearPending() {
        _pendingCounts.value = Denomination.values().associateWith { 0 }
        _memo.value = ""
    }

    fun updateMemo(newMemo: String) {
        _memo.value = newMemo
    }

    fun saveTransaction(type: TransactionType, lat: Double? = null, lon: Double? = null) {
        val counts = _pendingCounts.value
        var total = 0L
        for (entry in counts.entries) {
            total += entry.key.faceValue.toLong() * entry.value
        }
        
        if (total == 0L) return

        viewModelScope.launch {
            val transaction = CashTransaction(
                id = UUID.randomUUID().toString(),
                type = type,
                counts = counts,
                total = total,
                memo = _memo.value.ifBlank { null },
                lat = lat,
                lon = lon
            )
            repository.insertTransaction(transaction)
            clearPending()
            _uiEvent.emit(UiEvent.ShowUndoSnackbar(transaction.id))
        }
    }

    fun undoTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransactionById(transactionId)
        }
    }

    fun deleteTransaction(transaction: CashTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: CashTransaction) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    suspend fun hasInitialTransaction(): Boolean {
        return repository.hasInitialTransaction()
    }

    fun saveDefaultAdjustmentPreset(preset: Map<Denomination, Int>) {
        viewModelScope.launch {
            settingsRepository.saveDefaultAdjustmentPreset(preset)
        }
    }
    suspend fun getAllTransactionsSnapshot(): List<CashTransaction> {
        return repository.getAllTransactionsSnapshot()
    }
}
