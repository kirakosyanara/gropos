package com.unisight.gropos.features.transaction.presentation.findtransaction

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.core.util.CurrencyFormatter
import com.unisight.gropos.features.transaction.domain.model.Transaction
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository
import com.unisight.gropos.features.transaction.domain.repository.TransactionSearchCriteria
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * ViewModel for the Find Transaction Screen.
 * 
 * Per REMEDIATION_CHECKLIST: Find Transaction Screen for returns lookup.
 * Per RETURNS.md: Search by receipt number, date range, or amount.
 */
class FindTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val currencyFormatter: CurrencyFormatter,
    private val scope: CoroutineScope? = null
) : ScreenModel {
    
    private val _state = MutableStateFlow(FindTransactionUiState.initial())
    val state: StateFlow<FindTransactionUiState> = _state.asStateFlow()
    
    private val effectiveScope: CoroutineScope
        get() = scope ?: screenModelScope
    
    /**
     * Updates the search query.
     */
    fun onSearchQueryChanged(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }
    
    /**
     * Performs a search with the current query.
     */
    fun onSearch() {
        val query = _state.value.searchQuery.trim()
        if (query.isEmpty()) {
            loadRecentTransactions()
            return
        }
        
        _state.value = _state.value.copy(isSearching = true, errorMessage = null)
        
        effectiveScope.launch {
            try {
                val criteria = TransactionSearchCriteria(
                    receiptNumber = query,
                    limit = 50
                )
                
                val results = transactionRepository.searchTransactions(criteria)
                
                _state.value = _state.value.copy(
                    isSearching = false,
                    hasSearched = true,
                    results = results.map { mapToUiModel(it) }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSearching = false,
                    hasSearched = true,
                    errorMessage = "Search failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Loads recent transactions (initial state).
     */
    fun loadRecentTransactions() {
        _state.value = _state.value.copy(isSearching = true)
        
        effectiveScope.launch {
            try {
                val recent = transactionRepository.getRecent(20)
                
                _state.value = _state.value.copy(
                    isSearching = false,
                    hasSearched = true,
                    results = recent.map { mapToUiModel(it) }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSearching = false,
                    hasSearched = true,
                    errorMessage = "Failed to load transactions: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Selects a transaction to view details.
     */
    fun onTransactionSelected(id: Long) {
        effectiveScope.launch {
            val transaction = transactionRepository.getById(id)
            transaction?.let {
                _state.value = _state.value.copy(
                    selectedTransaction = it,
                    showDetailsDialog = true
                )
            }
        }
    }
    
    /**
     * Dismisses the transaction details dialog.
     */
    fun onDismissDetails() {
        _state.value = _state.value.copy(
            selectedTransaction = null,
            showDetailsDialog = false
        )
    }
    
    /**
     * Clears the search and shows recent transactions.
     */
    fun onClearSearch() {
        _state.value = _state.value.copy(searchQuery = "")
        loadRecentTransactions()
    }
    
    /**
     * Dismisses error message.
     */
    fun onDismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
    
    /**
     * Maps a Transaction to UI model.
     */
    private fun mapToUiModel(transaction: Transaction): TransactionSearchResultUiModel {
        return TransactionSearchResultUiModel(
            id = transaction.id,
            receiptNumber = transaction.id.toString().padStart(8, '0'),
            dateTime = transaction.completedDateTime ?: "Unknown",
            grandTotal = currencyFormatter.format(transaction.grandTotal),
            itemCount = "${transaction.items.size} items",
            cashierName = transaction.employeeName ?: "Employee #${transaction.employeeId ?: 0}",
            paymentMethod = formatPaymentMethods(transaction)
        )
    }
    
    /**
     * Formats payment methods for display.
     */
    private fun formatPaymentMethods(transaction: Transaction): String {
        return transaction.payments.joinToString(", ") { it.paymentMethodName }
    }
}

