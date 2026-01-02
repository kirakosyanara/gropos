package com.unisight.gropos.features.payment.domain.terminal

/**
 * Sealed class representing the result of a payment terminal transaction.
 * 
 * Per PAYMENT_PROCESSING.md: Payment flow states include Approved, Declined, Error.
 * 
 * Per reliability-stability.mdc: Use sealed classes for state modeling,
 * avoiding stringly-typed status values.
 */
sealed class PaymentResult {
    
    /**
     * Payment was approved by the issuer.
     * 
     * @param transactionId Unique identifier from the payment processor
     * @param cardType Card brand (e.g., "VISA", "MASTERCARD", "DISCOVER", "AMEX")
     * @param lastFour Last 4 digits of the card number for display/receipt
     * @param authCode Authorization code from the issuer
     */
    data class Approved(
        val transactionId: String,
        val cardType: String,
        val lastFour: String,
        val authCode: String
    ) : PaymentResult()
    
    /**
     * Payment was declined by the issuer.
     * 
     * @param reason Human-readable reason for the decline
     *               (e.g., "Insufficient Funds", "Card Expired")
     */
    data class Declined(
        val reason: String
    ) : PaymentResult()
    
    /**
     * An error occurred during payment processing.
     * 
     * This represents a technical failure, not a business decline:
     * - Network timeout
     * - Terminal disconnected
     * - Communication error
     * 
     * @param message Description of the error
     */
    data class Error(
        val message: String
    ) : PaymentResult()
    
    /**
     * Transaction was cancelled by the user.
     * 
     * Occurs when the operator presses "Cancel" on the POS
     * while waiting for card input.
     */
    data object Cancelled : PaymentResult()
}

