package com.unisight.gropos.features.transaction.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.core.util.CurrencyFormatter
import com.unisight.gropos.features.transaction.domain.model.Transaction
import com.unisight.gropos.features.transaction.domain.model.TransactionItem
import com.unisight.gropos.features.transaction.domain.model.TransactionPayment
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * ViewModel for the Transaction History screen.
 * 
 * Per FUNCTIONS_MENU.md - Return/Invoice:
 * - Viewing transaction history
 * - Selecting transactions for detail view
 * 
 * Per SCREEN_LAYOUTS.md - Order Report Screen:
 * - Date range selection (future)
 * - Transaction filtering (future)
 */
class TransactionHistoryViewModel(
    private val transactionRepository: TransactionRepository,
    private val currencyFormatter: CurrencyFormatter,
    private val scope: CoroutineScope? = null
) : ScreenModel {
    
    private val _state = MutableStateFlow(TransactionHistoryUiState.initial())
    val state: StateFlow<TransactionHistoryUiState> = _state.asStateFlow()
    
    private val effectiveScope: CoroutineScope
        get() = scope ?: screenModelScope
    
    // Date formatters for display
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, h:mm a", Locale.US)
    private val fullDateTimeFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' h:mm:ss a", Locale.US)
    
    init {
        loadRecentTransactions()
    }
    
    /**
     * Loads recent transactions from the database.
     * 
     * Per DATABASE_SCHEMA.md: Query by completedDateTime descending.
     */
    fun loadRecentTransactions(limit: Int = 50) {
        effectiveScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val transactions = transactionRepository.getRecent(limit)
                
                val listItems = transactions.map { mapToListItem(it) }
                
                _state.value = _state.value.copy(
                    transactions = listItems,
                    isLoading = false,
                    isEmpty = listItems.isEmpty(),
                    // Auto-select first transaction if available
                    selectedTransaction = transactions.firstOrNull()?.let { mapToDetail(it) }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load transactions: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Selects a transaction to view its details.
     */
    fun onTransactionSelect(transactionId: Long) {
        effectiveScope.launch {
            val transaction = transactionRepository.getById(transactionId)
            transaction?.let {
                _state.value = _state.value.copy(
                    selectedTransaction = mapToDetail(it)
                )
            }
        }
    }
    
    /**
     * Refreshes the transaction list.
     */
    fun refresh() {
        loadRecentTransactions()
    }
    
    /**
     * Dismisses any error message.
     */
    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
    
    // ========================================================================
    // Mapping Functions
    // ========================================================================
    
    private fun mapToListItem(transaction: Transaction): TransactionListItemUiModel {
        val formattedTime = formatDateTime(transaction.completedDateTime)
        val paymentMethod = transaction.payments.firstOrNull()?.paymentMethodName ?: "Unknown"
        
        return TransactionListItemUiModel(
            id = transaction.id,
            formattedId = "#${transaction.id}",
            formattedTime = formattedTime,
            formattedTotal = currencyFormatter.format(transaction.grandTotal),
            itemCount = formatItemCount(transaction.itemCount),
            typeName = transaction.transactionTypeName,
            paymentMethod = paymentMethod
        )
    }
    
    private fun mapToDetail(transaction: Transaction): TransactionDetailUiModel {
        val items = transaction.items.map { mapToItemUiModel(it) }
        val payments = transaction.payments.map { mapToPaymentUiModel(it) }
        
        return TransactionDetailUiModel(
            id = transaction.id,
            formattedId = "#${transaction.id}",
            formattedDateTime = formatFullDateTime(transaction.completedDateTime),
            stationId = "Station ${transaction.stationId}",
            employeeName = transaction.employeeName,
            typeName = transaction.transactionTypeName,
            subtotal = currencyFormatter.format(transaction.subTotal),
            discountTotal = if (transaction.discountTotal > BigDecimal.ZERO) {
                currencyFormatter.formatWithSign(transaction.discountTotal.negate(), false)
            } else null,
            taxTotal = currencyFormatter.format(transaction.taxTotal),
            crvTotal = if (transaction.crvTotal > BigDecimal.ZERO) {
                currencyFormatter.format(transaction.crvTotal)
            } else null,
            grandTotal = currencyFormatter.format(transaction.grandTotal),
            items = items,
            payments = payments
        )
    }
    
    private fun mapToItemUiModel(item: TransactionItem): TransactionItemUiModel {
        val quantity = if (item.quantityUsed.scale() == 0 || 
            item.quantityUsed.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            item.quantityUsed.toInt().toString()
        } else {
            item.quantityUsed.setScale(2).toString()
        }
        
        return TransactionItemUiModel(
            id = item.id,
            productName = item.branchProductName,
            quantity = quantity,
            unitPrice = currencyFormatter.format(item.priceUsed),
            lineTotal = currencyFormatter.format(item.subTotal),
            taxAmount = if (item.taxTotal > BigDecimal.ZERO) {
                currencyFormatter.format(item.taxTotal)
            } else null,
            crvAmount = if (item.crvRatePerUnit > BigDecimal.ZERO) {
                currencyFormatter.format(item.crvRatePerUnit.multiply(item.quantityUsed))
            } else null,
            isSnapEligible = item.isFoodStampEligible,
            taxIndicator = item.taxIndicator
        )
    }
    
    private fun mapToPaymentUiModel(payment: TransactionPayment): TransactionPaymentUiModel {
        return TransactionPaymentUiModel(
            id = payment.id,
            methodName = payment.paymentMethodName,
            amount = currencyFormatter.format(payment.value),
            cardInfo = payment.cardLastFour?.let { "****$it" }
        )
    }
    
    // ========================================================================
    // Date Formatting
    // ========================================================================
    
    /**
     * Formats an ISO-8601 timestamp into a short display format.
     * 
     * Per requirement: Format "Jan 01, 2:30 PM"
     */
    private fun formatDateTime(isoString: String): String {
        return try {
            val instant = Instant.parse(isoString)
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            dateTimeFormatter.format(zonedDateTime)
        } catch (e: Exception) {
            // Fallback if parsing fails
            isoString.take(16).replace("T", " ")
        }
    }
    
    /**
     * Formats an ISO-8601 timestamp into a full display format.
     * 
     * Example: "January 01, 2026 at 2:30:45 PM"
     */
    private fun formatFullDateTime(isoString: String): String {
        return try {
            val instant = Instant.parse(isoString)
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            fullDateTimeFormatter.format(zonedDateTime)
        } catch (e: Exception) {
            isoString
        }
    }
    
    private fun formatItemCount(count: Int): String {
        return if (count == 1) "1 item" else "$count items"
    }
}

