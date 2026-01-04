package com.unisight.gropos.features.payment.domain.model

import com.unisight.gropos.features.transaction.domain.model.PaxData
import java.math.BigDecimal

/**
 * Payment types supported by the POS.
 * 
 * Per PAYMENT_PROCESSING.md: Cash, Credit, Debit, EBT Food, EBT Cash.
 */
enum class PaymentType {
    Cash,
    Credit,
    Debit,
    EbtSnap,      // SNAP (Food Stamp) benefits
    EbtCash,      // Cash benefits
    Check,
    OnAccount
}

/**
 * Status of a payment transaction.
 * 
 * Per PAYMENT_PROCESSING.md: Payment flow states.
 */
enum class PaymentStatus {
    Approved,
    Declined,
    Partial,
    Error,
    Pending
}

/**
 * Response from a payment processing attempt.
 * 
 * Per PAYMENT_PROCESSING.md: PaymentResponse data model.
 */
data class PaymentResponse(
    val status: PaymentStatus,
    val approvedAmount: BigDecimal = BigDecimal.ZERO,
    val changeAmount: BigDecimal = BigDecimal.ZERO,
    val authCode: String? = null,
    val refNum: String? = null,
    val cardType: String? = null,
    val lastFour: String? = null,
    val entryMode: String? = null,
    val errorMessage: String? = null
)

/**
 * Represents an applied payment on a transaction.
 * 
 * Per PAYMENT_PROCESSING.md: Payment record.
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md: Full API field alignment.
 */
data class AppliedPayment(
    val id: String,
    val type: PaymentType,
    val amount: BigDecimal,
    val displayName: String,
    /** Authorization code from payment processor */
    val authCode: String? = null,
    /** Last 4 digits of card number */
    val lastFour: String? = null,
    /** Reference number from payment processor */
    val referenceNumber: String? = null,
    /** Card type/brand (e.g., VISA, MASTERCARD) */
    val cardType: String? = null,
    /** Full PAX terminal response data */
    val paxData: PaxData? = null
)

