package com.unisight.gropos.features.transaction.presentation

import java.math.BigDecimal

/**
 * UI model for displaying a transaction in the list.
 * 
 * Per Governance: Pre-formatted strings for display.
 */
data class TransactionListItemUiModel(
    val id: Long,
    val formattedId: String,         // e.g., "#1735750200000"
    val formattedTime: String,       // e.g., "Jan 01, 2:30 PM"
    val formattedTotal: String,      // e.g., "$44.61"
    val itemCount: String,           // e.g., "5 items"
    val typeName: String,            // e.g., "Sale"
    val paymentMethod: String        // e.g., "Cash"
)

/**
 * UI model for displaying a transaction item in the detail view.
 */
data class TransactionItemUiModel(
    val id: Long,
    val productName: String,
    val quantity: String,           // e.g., "2"
    val unitPrice: String,          // e.g., "$4.99"
    val lineTotal: String,          // e.g., "$9.98"
    val taxAmount: String?,         // e.g., "$0.85"
    val crvAmount: String?,         // e.g., "$0.10"
    val isSnapEligible: Boolean,
    val taxIndicator: String        // "F" or "T"
)

/**
 * UI model for displaying a payment in the detail view.
 */
data class TransactionPaymentUiModel(
    val id: Long,
    val methodName: String,         // e.g., "Cash"
    val amount: String,             // e.g., "$44.61"
    val cardInfo: String?           // e.g., "****1234"
)

/**
 * UI model for the transaction detail view.
 */
data class TransactionDetailUiModel(
    val id: Long,
    val formattedId: String,
    val formattedDateTime: String,
    val stationId: String,
    val employeeName: String?,
    val typeName: String,
    val subtotal: String,
    val discountTotal: String?,
    val taxTotal: String,
    val crvTotal: String?,
    val grandTotal: String,
    val items: List<TransactionItemUiModel>,
    val payments: List<TransactionPaymentUiModel>
)

/**
 * UI State for the Transaction History screen.
 * 
 * Per kotlin-standards.mdc: Use data class for state.
 * Per SCREEN_LAYOUTS.md: Left list, right detail layout.
 */
data class TransactionHistoryUiState(
    val transactions: List<TransactionListItemUiModel> = emptyList(),
    val selectedTransaction: TransactionDetailUiModel? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEmpty: Boolean = true
) {
    companion object {
        fun initial(): TransactionHistoryUiState = TransactionHistoryUiState(isLoading = true)
    }
}

