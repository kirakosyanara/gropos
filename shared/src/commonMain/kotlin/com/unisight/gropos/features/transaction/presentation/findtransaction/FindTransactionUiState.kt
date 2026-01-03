package com.unisight.gropos.features.transaction.presentation.findtransaction

import com.unisight.gropos.features.transaction.domain.model.Transaction

/**
 * UI State for the Find Transaction Screen.
 * 
 * Per REMEDIATION_CHECKLIST: Find Transaction Screen for returns lookup.
 * Per RETURNS.md: Search by receipt number, date range, or amount.
 */
data class FindTransactionUiState(
    /** Current search query (receipt number) */
    val searchQuery: String = "",
    
    /** Search results */
    val results: List<TransactionSearchResultUiModel> = emptyList(),
    
    /** Whether a search is in progress */
    val isSearching: Boolean = false,
    
    /** Error message if search failed */
    val errorMessage: String? = null,
    
    /** Selected transaction for details */
    val selectedTransaction: Transaction? = null,
    
    /** Whether to show transaction details dialog */
    val showDetailsDialog: Boolean = false,
    
    /** Whether search has been performed */
    val hasSearched: Boolean = false
) {
    companion object {
        fun initial() = FindTransactionUiState()
    }
}

/**
 * UI model for a transaction search result.
 */
data class TransactionSearchResultUiModel(
    val id: Long,
    val receiptNumber: String,
    val dateTime: String,
    val grandTotal: String,
    val itemCount: String,
    val cashierName: String,
    val paymentMethod: String
)

