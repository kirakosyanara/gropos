package com.unisight.gropos.features.payment.domain.terminal

import java.math.BigDecimal

/**
 * Hardware abstraction interface for payment terminal operations.
 * 
 * Per DESKTOP_HARDWARE.md: This interface abstracts the payment terminal
 * hardware, allowing for implementations that support PAX, Sunmi, JavaPOS,
 * or simulated terminals.
 * 
 * Per PAYMENT_PROCESSING.md: Handles Credit, Debit, EBT card payments.
 * 
 * Why this abstraction exists:
 * - Decouples business logic from hardware implementation
 * - Allows for simulated terminal in development/testing
 * - Supports multiple terminal vendors (PAX A920, S300, etc.)
 * - Enables unit testing without physical hardware
 */
interface PaymentTerminal {
    
    /**
     * Process a payment of the given amount.
     * 
     * This is a suspending function that waits for:
     * 1. Card insertion/swipe/tap
     * 2. PIN entry (if required)
     * 3. Network authorization
     * 
     * Per reliability-stability.mdc: 
     * - Non-blocking operation using coroutines
     * - Handles timeout gracefully
     * 
     * @param amount The payment amount (in dollars, e.g., 10.99)
     * @return PaymentResult indicating success or failure
     */
    suspend fun processPayment(amount: BigDecimal): PaymentResult
    
    /**
     * Cancel the current transaction.
     * 
     * Called when the user presses "Cancel" while waiting for card input.
     * Should abort any pending terminal operation.
     */
    suspend fun cancelTransaction()
}

