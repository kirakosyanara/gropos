package com.unisight.gropos.features.payment.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.core.util.CurrencyFormatter
import com.unisight.gropos.features.checkout.domain.model.Cart
import com.unisight.gropos.features.checkout.domain.repository.CartRepository
import com.unisight.gropos.features.payment.domain.model.AppliedPayment
import com.unisight.gropos.features.payment.domain.model.PaymentType
import com.unisight.gropos.features.transaction.domain.mapper.toTransaction
import com.unisight.gropos.features.transaction.domain.model.Transaction
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * ViewModel for the Payment screen.
 * 
 * Per ARCHITECTURE_BLUEPRINT.md:
 * - Observes the shared CartRepository singleton for cart totals
 * - Same cart state is visible in Checkout and Customer Display
 * 
 * Per PAYMENT_PROCESSING.md:
 * - Supports Cash, Credit, Debit, EBT payments
 * - Handles split tender (multiple payment types)
 * - Tracks SNAP eligible vs non-SNAP totals
 * 
 * Per DATABASE_SCHEMA.md:
 * - Saves completed transactions to LocalTransaction collection
 */
class PaymentViewModel(
    private val cartRepository: CartRepository,
    private val currencyFormatter: CurrencyFormatter,
    private val transactionRepository: TransactionRepository,
    private val scope: CoroutineScope? = null
) : ScreenModel {
    
    private val _state = MutableStateFlow(PaymentUiState.initial())
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()
    
    // Internal list of applied payments (for split tender)
    private val appliedPayments = mutableListOf<AppliedPayment>()
    private var totalPaid = BigDecimal.ZERO
    
    // Store cart snapshot for transaction creation
    private var cartSnapshot: Cart? = null
    
    private val effectiveScope: CoroutineScope
        get() = scope ?: screenModelScope
    
    init {
        observeCartChanges()
    }
    
    /**
     * Observes cart changes from the shared CartRepository.
     * Updates the summary when cart changes.
     */
    private fun observeCartChanges() {
        cartRepository.cart
            .onEach { cart -> 
                cartSnapshot = cart
                updateStateFromCart(cart) 
            }
            .launchIn(effectiveScope)
    }
    
    /**
     * Updates the UI state from cart data.
     */
    private fun updateStateFromCart(cart: Cart) {
        val snapEligibleTotal = calculateSnapEligible(cart)
        val nonSnapTotal = cart.grandTotal - snapEligibleTotal
        val remainingAmount = cart.grandTotal - totalPaid
        
        val summary = PaymentSummaryUiModel(
            itemSubtotal = currencyFormatter.format(cart.subTotal),
            discountTotal = if (cart.discountTotal > BigDecimal.ZERO) {
                currencyFormatter.formatWithSign(cart.discountTotal.negate(), false)
            } else null,
            subtotal = currencyFormatter.format(cart.subTotal - cart.discountTotal),
            taxTotal = currencyFormatter.format(cart.taxTotal),
            crvTotal = if (cart.crvTotal > BigDecimal.ZERO) {
                currencyFormatter.format(cart.crvTotal)
            } else null,
            grandTotal = currencyFormatter.format(cart.grandTotal),
            itemCount = formatItemCount(cart.itemCount),
            snapEligibleTotal = currencyFormatter.format(snapEligibleTotal),
            nonSnapTotal = currencyFormatter.format(nonSnapTotal)
        )
        
        _state.value = _state.value.copy(
            summary = summary,
            remainingAmount = currencyFormatter.format(remainingAmount.coerceAtLeast(BigDecimal.ZERO)),
            remainingAmountRaw = remainingAmount.coerceAtLeast(BigDecimal.ZERO),
            isCartEmpty = cart.isEmpty
        )
    }
    
    /**
     * Calculates the SNAP-eligible total from cart items.
     * 
     * Per BUSINESS_RULES.md: SNAP eligible items have isSNAPEligible = true.
     */
    private fun calculateSnapEligible(cart: Cart): BigDecimal {
        return cart.items
            .filterNot { it.isRemoved }
            .filter { it.isSnapEligible }
            .fold(BigDecimal.ZERO) { acc, item -> acc + item.subTotal }
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    // ========================================================================
    // Tab Selection
    // ========================================================================
    
    fun onTabSelect(tab: PaymentTab) {
        _state.value = _state.value.copy(selectedTab = tab)
    }
    
    // ========================================================================
    // TenKey Input
    // ========================================================================
    
    fun onDigitPress(digit: String) {
        val current = _state.value.enteredAmount
        val newAmount = current + digit
        _state.value = _state.value.copy(enteredAmount = newAmount)
    }
    
    fun onClearPress() {
        _state.value = _state.value.copy(enteredAmount = "")
    }
    
    fun onBackspacePress() {
        val current = _state.value.enteredAmount
        if (current.isNotEmpty()) {
            _state.value = _state.value.copy(enteredAmount = current.dropLast(1))
        }
    }
    
    // ========================================================================
    // Cash Payment (Walking Skeleton)
    // Per requirement: Assume "Exact Change" for this Skeleton
    // ========================================================================
    
    /**
     * Process a cash payment.
     * 
     * For the Walking Skeleton, this assumes exact change:
     * - Tendered amount = remaining amount
     * - Transaction completes immediately
     */
    fun onCashExactChange() {
        processCashPayment(_state.value.remainingAmountRaw)
    }
    
    /**
     * Process a cash payment with a specific quick amount.
     */
    fun onCashQuickAmount(amount: BigDecimal) {
        processCashPayment(amount)
    }
    
    /**
     * Process a cash payment with the entered amount.
     */
    fun onCashEnteredAmount() {
        val enteredString = _state.value.enteredAmount
        if (enteredString.isBlank()) return
        
        val amount = try {
            BigDecimal(enteredString).setScale(2, RoundingMode.HALF_UP)
        } catch (e: Exception) {
            _state.value = _state.value.copy(errorMessage = "Invalid amount")
            return
        }
        
        processCashPayment(amount)
    }
    
    private fun processCashPayment(tendered: BigDecimal) {
        val remaining = _state.value.remainingAmountRaw
        if (remaining <= BigDecimal.ZERO) {
            _state.value = _state.value.copy(errorMessage = "No amount due")
            return
        }
        
        _state.value = _state.value.copy(isProcessing = true)
        
        effectiveScope.launch {
            // Calculate payment
            val paymentAmount = minOf(tendered, remaining)
            val change = (tendered - remaining).coerceAtLeast(BigDecimal.ZERO)
            
            // Create payment record
            val payment = AppliedPayment(
                id = UUID.randomUUID().toString(),
                type = PaymentType.Cash,
                amount = paymentAmount,
                displayName = "Cash"
            )
            
            appliedPayments.add(payment)
            totalPaid += paymentAmount
            
            // Update state
            val newRemainingRaw = remaining - paymentAmount
            val isComplete = newRemainingRaw <= BigDecimal.ZERO
            
            _state.value = _state.value.copy(
                isProcessing = false,
                appliedPayments = appliedPayments.map { mapPaymentToUiModel(it) },
                remainingAmount = currencyFormatter.format(newRemainingRaw.coerceAtLeast(BigDecimal.ZERO)),
                remainingAmountRaw = newRemainingRaw.coerceAtLeast(BigDecimal.ZERO),
                enteredAmount = "",
                showChangeDialog = isComplete && change > BigDecimal.ZERO,
                changeAmount = currencyFormatter.format(change)
            )
            
            // Complete transaction if fully paid
            if (isComplete) {
                completeTransaction()
            }
        }
    }
    
    /**
     * Completes the transaction by:
     * 1. Converting cart to Transaction
     * 2. Saving to database
     * 3. Printing virtual receipt
     * 4. Clearing cart
     * 
     * Per DATABASE_SCHEMA.md: Save to LocalTransaction collection.
     * Per reliability-stability.mdc: Handle save errors gracefully.
     */
    private suspend fun completeTransaction() {
        val cart = cartSnapshot ?: return
        
        // Convert cart to transaction
        val transaction = cart.toTransaction(
            appliedPayments = appliedPayments.toList()
        )
        
        // Save transaction to database
        val saveResult = transactionRepository.saveTransaction(transaction)
        
        saveResult.fold(
            onSuccess = {
                // Print virtual receipt to console
                printVirtualReceipt(transaction)
                
                // Mark as complete
                _state.value = _state.value.copy(isComplete = true)
                
                // Clear cart
                cartRepository.clearCart()
                
                println("PaymentViewModel: Transaction ${transaction.id} saved successfully")
            },
            onFailure = { error ->
                println("PaymentViewModel: Failed to save transaction - ${error.message}")
                _state.value = _state.value.copy(
                    errorMessage = "Failed to save transaction. Please try again.",
                    isProcessing = false
                )
            }
        )
    }
    
    /**
     * Prints a virtual receipt to the console.
     * 
     * Per requirement: Log the "Receipt" text to the console (simulating a print job).
     * This simulates what would be sent to a physical receipt printer.
     */
    private fun printVirtualReceipt(transaction: Transaction) {
        val receiptBuilder = StringBuilder()
        val divider = "=".repeat(40)
        val thinDivider = "-".repeat(40)
        
        receiptBuilder.appendLine()
        receiptBuilder.appendLine(divider)
        receiptBuilder.appendLine("         *** VIRTUAL RECEIPT ***")
        receiptBuilder.appendLine(divider)
        receiptBuilder.appendLine()
        receiptBuilder.appendLine("Transaction #: ${transaction.id}")
        receiptBuilder.appendLine("Date: ${transaction.completedDateTime}")
        receiptBuilder.appendLine("Station: ${transaction.stationId}")
        receiptBuilder.appendLine()
        receiptBuilder.appendLine(thinDivider)
        receiptBuilder.appendLine("ITEMS:")
        receiptBuilder.appendLine(thinDivider)
        
        transaction.items.forEach { item ->
            val qty = if (item.quantityUsed.scale() == 0 || item.quantityUsed.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
                item.quantityUsed.toInt().toString()
            } else {
                item.quantityUsed.setScale(2, RoundingMode.HALF_UP).toString()
            }
            val priceStr = currencyFormatter.format(item.priceUsed)
            val lineTotal = currencyFormatter.format(item.subTotal)
            val taxFlag = item.taxIndicator
            
            receiptBuilder.appendLine("${item.branchProductName}")
            receiptBuilder.appendLine("  $qty x $priceStr = $lineTotal $taxFlag")
            
            if (item.taxTotal > BigDecimal.ZERO) {
                receiptBuilder.appendLine("    Tax: ${currencyFormatter.format(item.taxTotal)}")
            }
            if (item.crvRatePerUnit > BigDecimal.ZERO) {
                val crvTotal = item.crvRatePerUnit.multiply(item.quantityUsed).setScale(2, RoundingMode.HALF_UP)
                receiptBuilder.appendLine("    CRV: ${currencyFormatter.format(crvTotal)}")
            }
        }
        
        receiptBuilder.appendLine()
        receiptBuilder.appendLine(thinDivider)
        receiptBuilder.appendLine("TOTALS:")
        receiptBuilder.appendLine(thinDivider)
        receiptBuilder.appendLine("Subtotal:       ${currencyFormatter.format(transaction.subTotal)}")
        
        if (transaction.discountTotal > BigDecimal.ZERO) {
            receiptBuilder.appendLine("Discount:      -${currencyFormatter.format(transaction.discountTotal)}")
        }
        
        receiptBuilder.appendLine("Tax:            ${currencyFormatter.format(transaction.taxTotal)}")
        
        if (transaction.crvTotal > BigDecimal.ZERO) {
            receiptBuilder.appendLine("CRV:            ${currencyFormatter.format(transaction.crvTotal)}")
        }
        
        receiptBuilder.appendLine(thinDivider)
        receiptBuilder.appendLine("GRAND TOTAL:    ${currencyFormatter.format(transaction.grandTotal)}")
        receiptBuilder.appendLine(thinDivider)
        
        receiptBuilder.appendLine()
        receiptBuilder.appendLine("PAYMENTS:")
        transaction.payments.forEach { payment ->
            receiptBuilder.appendLine("  ${payment.paymentMethodName}: ${currencyFormatter.format(payment.value)}")
        }
        
        receiptBuilder.appendLine()
        receiptBuilder.appendLine(divider)
        receiptBuilder.appendLine("       Thank you for shopping!")
        receiptBuilder.appendLine(divider)
        receiptBuilder.appendLine()
        
        // Output to console
        println(receiptBuilder.toString())
    }
    
    // ========================================================================
    // Card Payments (Placeholder for Walking Skeleton)
    // ========================================================================
    
    fun onCreditPayment() {
        // For Walking Skeleton: Show message that terminal is not connected
        _state.value = _state.value.copy(
            errorMessage = "Payment terminal not connected. Use Cash for this demo."
        )
    }
    
    fun onDebitPayment() {
        _state.value = _state.value.copy(
            errorMessage = "Payment terminal not connected. Use Cash for this demo."
        )
    }
    
    fun onEbtSnapPayment() {
        _state.value = _state.value.copy(
            errorMessage = "Payment terminal not connected. Use Cash for this demo."
        )
    }
    
    fun onEbtCashPayment() {
        _state.value = _state.value.copy(
            errorMessage = "Payment terminal not connected. Use Cash for this demo."
        )
    }
    
    fun onBalanceCheck() {
        _state.value = _state.value.copy(
            errorMessage = "Payment terminal not connected."
        )
    }
    
    // ========================================================================
    // Dialogs
    // ========================================================================
    
    fun dismissChangeDialog() {
        _state.value = _state.value.copy(showChangeDialog = false)
    }
    
    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
    
    // ========================================================================
    // Helpers
    // ========================================================================
    
    private fun mapPaymentToUiModel(payment: AppliedPayment): AppliedPaymentUiModel {
        return AppliedPaymentUiModel(
            id = payment.id,
            displayName = payment.displayName,
            amount = currencyFormatter.format(payment.amount),
            details = payment.lastFour?.let { "****$it" }
        )
    }
    
    private fun formatItemCount(count: BigDecimal): String {
        val intCount = count.toInt()
        return if (intCount == 1) "1 item" else "$intCount items"
    }
}
