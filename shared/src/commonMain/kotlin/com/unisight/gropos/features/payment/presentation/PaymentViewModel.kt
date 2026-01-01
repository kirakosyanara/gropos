package com.unisight.gropos.features.payment.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.core.util.CurrencyFormatter
import com.unisight.gropos.features.checkout.domain.model.Cart
import com.unisight.gropos.features.checkout.domain.model.CartItem
import com.unisight.gropos.features.checkout.domain.repository.CartRepository
import com.unisight.gropos.features.payment.domain.model.AppliedPayment
import com.unisight.gropos.features.payment.domain.model.PaymentStatus
import com.unisight.gropos.features.payment.domain.model.PaymentType
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
 */
class PaymentViewModel(
    private val cartRepository: CartRepository,
    private val currencyFormatter: CurrencyFormatter,
    private val scope: CoroutineScope? = null
) : ScreenModel {
    
    private val _state = MutableStateFlow(PaymentUiState.initial())
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()
    
    // Internal list of applied payments (for split tender)
    private val appliedPayments = mutableListOf<AppliedPayment>()
    private var totalPaid = BigDecimal.ZERO
    
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
            .onEach { cart -> updateStateFromCart(cart) }
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
                isComplete = isComplete,
                showChangeDialog = isComplete && change > BigDecimal.ZERO,
                changeAmount = currencyFormatter.format(change)
            )
            
            // Clear cart if complete
            if (isComplete) {
                cartRepository.clearCart()
            }
        }
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

