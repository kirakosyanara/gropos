package com.unisight.gropos.features.payment.presentation

import com.unisight.gropos.features.payment.domain.model.AppliedPayment
import java.math.BigDecimal

/**
 * UI model for the totals summary in the payment screen.
 * 
 * Per SCREEN_LAYOUTS.md: Summary panel shows subtotal, tax, SNAP eligible, total.
 */
data class PaymentSummaryUiModel(
    val itemSubtotal: String,
    val discountTotal: String?,
    val subtotal: String,
    val taxTotal: String,
    val crvTotal: String?,
    val grandTotal: String,
    val itemCount: String,
    val snapEligibleTotal: String,
    val nonSnapTotal: String
)

/**
 * UI model for an applied payment in the list.
 */
data class AppliedPaymentUiModel(
    val id: String,
    val displayName: String,
    val amount: String,
    val details: String? = null
)

/**
 * Selected payment tab.
 * 
 * Per SCREEN_LAYOUTS.md: Payment methods tabs.
 */
enum class PaymentTab {
    Charge,  // Credit/Debit
    Ebt,     // SNAP, EBT Cash, Balance Check
    Cash,    // Quick cash buttons
    Other    // Check, On Account
}

/**
 * UI State for the Payment screen.
 * 
 * Per SCREEN_LAYOUTS.md (Payment Screen):
 * - Left side: Summary, SNAP Eligible, Payments Applied, Remaining
 * - Right side: Payment method tabs and TenKey
 * 
 * Per PAYMENT_PROCESSING.md:
 * - Split tender support
 * - SNAP eligibility tracking
 * 
 * Per DESKTOP_HARDWARE.md:
 * - Payment terminal dialog state for card payments
 */
data class PaymentUiState(
    // Summary data
    val summary: PaymentSummaryUiModel = PaymentSummaryUiModel(
        itemSubtotal = "$0.00",
        discountTotal = null,
        subtotal = "$0.00",
        taxTotal = "$0.00",
        crvTotal = null,
        grandTotal = "$0.00",
        itemCount = "0 items",
        snapEligibleTotal = "$0.00",
        nonSnapTotal = "$0.00"
    ),
    
    // Amount tracking
    val remainingAmount: String = "$0.00",
    val remainingAmountRaw: BigDecimal = BigDecimal.ZERO,
    val enteredAmount: String = "",
    
    // Applied payments
    val appliedPayments: List<AppliedPaymentUiModel> = emptyList(),
    
    // Tab state
    val selectedTab: PaymentTab = PaymentTab.Cash,
    
    // Transaction state
    val isProcessing: Boolean = false,
    val isComplete: Boolean = false,
    val showChangeDialog: Boolean = false,
    val changeAmount: String = "$0.00",
    
    // Payment Terminal Dialog state
    // Per DESKTOP_HARDWARE.md: Modal overlay while waiting for card input
    val showTerminalDialog: Boolean = false,
    val terminalDialogAmount: String = "$0.00",
    
    // Error handling
    val errorMessage: String? = null,
    
    // Cart empty state
    val isCartEmpty: Boolean = true
) {
    companion object {
        fun initial(): PaymentUiState = PaymentUiState()
    }
}

