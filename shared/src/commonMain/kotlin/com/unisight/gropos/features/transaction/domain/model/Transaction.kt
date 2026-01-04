package com.unisight.gropos.features.transaction.domain.model

import java.math.BigDecimal

/**
 * Represents a completed transaction document.
 * 
 * Per DATABASE_SCHEMA.md - LocalTransaction collection structure.
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md - Full API field alignment.
 * All monetary values use BigDecimal for precision.
 * 
 * Document ID: {id} or {id}-P (pending sync)
 * Collection: LocalTransaction
 * Scope: local
 */
data class Transaction(
    val id: Long,
    val guid: String,
    
    // ========================================================================
    // Station/Employee/Location Information
    // ========================================================================
    val branchId: Int = 1,
    val stationId: Int = 1,
    val deviceId: Int? = null,
    val employeeId: Int? = null,
    val employeeName: String? = null,
    
    /** Till/Location Account ID for shift reconciliation */
    val locationAccountId: Int? = null,
    
    /** Shift ID for end-of-day reporting */
    val shiftId: Int? = null,
    
    // ========================================================================
    // Customer Information (optional)
    // ========================================================================
    /** Customer ID (for loyalty/returns) */
    val customerId: Int? = null,
    val customerName: String? = null,
    val loyaltyCardNumber: String? = null,
    
    // ========================================================================
    // Transaction Status & Type
    // Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md: 
    // Status values NOW aligned with backend API
    // ========================================================================
    val transactionStatusId: Int = COMPLETED,
    val transactionTypeName: String = "Sale",
    
    /** Sync status for tracking backend transmission */
    val syncStatus: Int = SYNC_PENDING,
    
    /** Remote ID after successful sync (null until synced) */
    val remoteId: Int? = null,
    
    /** For refunds: original transaction ID being refunded */
    val originalTransactionId: Long? = null,
    
    /** For refunds: original transaction GUID */
    val originalTransactionGuid: String? = null,
    
    // ========================================================================
    // Timestamps (ISO-8601 format)
    // ========================================================================
    val startDateTime: String,
    /** First payment timestamp */
    val paymentDate: String? = null,
    val completedDateTime: String,
    val completedDate: String,
    
    // ========================================================================
    // Counts (per API spec)
    // ========================================================================
    /** Total line items including voided/removed */
    val rowCount: Int = 0,
    /** Active (non-removed) item count */
    val itemCount: Int,
    /** Distinct product count */
    val uniqueProductCount: Int = 0,
    /** Distinct products with sale price > 0 */
    val uniqueSaleProductCount: Int = 0,
    /** Sum of all quantities purchased */
    val totalPurchaseCount: BigDecimal = BigDecimal.ZERO,
    
    // ========================================================================
    // Totals (per DATABASE_SCHEMA.md & API spec)
    // ========================================================================
    val subTotal: BigDecimal,
    val discountTotal: BigDecimal,
    val taxTotal: BigDecimal,
    val crvTotal: BigDecimal,
    val grandTotal: BigDecimal,
    /** Total cost of goods (3 decimal places) */
    val costTotal: BigDecimal = BigDecimal.ZERO,
    /** Total customer savings */
    val savingsTotal: BigDecimal = BigDecimal.ZERO,
    /** Additional fees */
    val fee: BigDecimal = BigDecimal.ZERO,
    
    // ========================================================================
    // Line Items & Payments
    // ========================================================================
    val items: List<TransactionItem>,
    val payments: List<TransactionPayment>,
    
    // ========================================================================
    // Metadata
    // ========================================================================
    /** Arbitrary metadata for extensibility */
    val metadata: Map<String, String>? = null
) {
    companion object {
        // ====================================================================
        // Transaction Status IDs - NOW ALIGNED WITH BACKEND API
        // Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md
        // ====================================================================
        const val OPEN = 0
        const val PROCESSING = 1
        const val ERRORED = 2
        const val VOIDED = 3
        const val COMPLETED = 4
        const val HOLD = 5
        
        // Legacy aliases for backward compatibility during migration
        @Deprecated("Use PROCESSING instead", ReplaceWith("PROCESSING"))
        const val PENDING = 1
        @Deprecated("Use HOLD instead", ReplaceWith("HOLD"))
        const val ON_HOLD = 5
        
        // ====================================================================
        // Sync Status (local tracking)
        // ====================================================================
        const val SYNC_PENDING = 0
        const val SYNC_IN_PROGRESS = 1
        const val SYNC_COMPLETED = 2
        const val SYNC_FAILED = 3
        const val SYNC_ABANDONED = 4
    }
}

/**
 * Represents a line item in a transaction.
 * 
 * Per DATABASE_SCHEMA.md - Transaction items array structure.
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md - Full API field alignment.
 */
data class TransactionItem(
    val id: Long,
    val transactionId: Long,
    
    // ========================================================================
    // GUIDs for API (per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md)
    // ========================================================================
    /** Parent transaction GUID */
    val transactionGuid: String,
    /** Unique item GUID */
    val transactionItemGuid: String,
    
    // ========================================================================
    // Product Information
    // ========================================================================
    val branchProductId: Int,
    val branchProductName: String,
    /** UPC/barcode/PLU */
    val itemNumber: String = "",
    
    // ========================================================================
    // Quantity Tracking
    // ========================================================================
    /** Net quantity (sold - returned) */
    val quantityUsed: BigDecimal,
    /** Quantity sold in this transaction */
    val quantitySold: BigDecimal = BigDecimal.ZERO,
    /** Quantity returned (for partial returns) */
    val quantityReturned: BigDecimal = BigDecimal.ZERO,
    val unitType: String,
    /** Display order (1-based) */
    val rowNumber: Int = 0,
    /** Quantity was manually entered */
    val isManualQuantity: Boolean = false,
    
    // ========================================================================
    // Pricing
    // ========================================================================
    val retailPrice: BigDecimal,
    val salePrice: BigDecimal?,
    /** Final price applied (effective price) */
    val priceUsed: BigDecimal,
    val floorPrice: BigDecimal?,
    /** Manually entered price */
    val promptedPrice: BigDecimal = BigDecimal.ZERO,
    /** Price after all discounts */
    val finalPrice: BigDecimal = BigDecimal.ZERO,
    /** Price + tax per unit */
    val finalPriceTaxSum: BigDecimal = BigDecimal.ZERO,
    
    // ========================================================================
    // Cost (for margin calculation)
    // ========================================================================
    /** Unit cost (3 decimal places) */
    val cost: BigDecimal = BigDecimal.ZERO,
    /** Total cost for this line */
    val costTotal: BigDecimal = BigDecimal.ZERO,
    
    // ========================================================================
    // Discounts
    // ========================================================================
    val discountAmountPerUnit: BigDecimal,
    val transactionDiscountAmountPerUnit: BigDecimal,
    /** Discount type applied (per DiscountTypeApi) */
    val discountTypeId: Int? = null,
    /** Discount parameter value */
    val discountTypeAmount: BigDecimal = BigDecimal.ZERO,
    /** Transaction-level discount type */
    val transactionDiscountTypeId: Int? = null,
    /** Transaction discount value */
    val transactionDiscountTypeAmount: BigDecimal = BigDecimal.ZERO,
    
    // ========================================================================
    // Tax
    // ========================================================================
    val taxPerUnit: BigDecimal,
    val taxTotal: BigDecimal,
    /** Combined tax rate percentage */
    val taxPercentSum: BigDecimal = BigDecimal.ZERO,
    /** Taxable amount for this line */
    val subjectToTaxTotal: BigDecimal = BigDecimal.ZERO,
    /** Itemized tax breakdown */
    val taxes: List<TransactionItemTax> = emptyList(),
    
    // ========================================================================
    // CRV (California Redemption Value)
    // ========================================================================
    val crvRatePerUnit: BigDecimal,
    
    // ========================================================================
    // Totals
    // ========================================================================
    val subTotal: BigDecimal,
    val savingsTotal: BigDecimal,
    /** Savings per unit */
    val savingsPerUnit: BigDecimal = BigDecimal.ZERO,
    /** Total paid for this line */
    val paidTotal: BigDecimal = BigDecimal.ZERO,
    
    // ========================================================================
    // SNAP/EBT Tracking
    // ========================================================================
    val isFoodStampEligible: Boolean,
    /** Amount paid via EBT for this line */
    val snapPaidAmount: BigDecimal = BigDecimal.ZERO,
    /** Percent of line paid via EBT */
    val snapPaidPercent: BigDecimal = BigDecimal.ZERO,
    /** Amount not covered by SNAP */
    val nonSNAPTotal: BigDecimal = BigDecimal.ZERO,
    
    // ========================================================================
    // Flags
    // ========================================================================
    val isRemoved: Boolean,
    val isPromptedPrice: Boolean,
    val isFloorPriceOverridden: Boolean,
    /** Employee who approved floor price override */
    val floorPriceOverrideEmployeeId: Int? = null,
    
    // ========================================================================
    // Type Information
    // ========================================================================
    val soldById: String,
    val taxIndicator: String,
    
    // ========================================================================
    // Timestamp
    // ========================================================================
    val scanDateTime: String?
)

/**
 * Tax breakdown for a transaction item.
 * 
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md:
 * Each item can have multiple tax authorities applied.
 */
data class TransactionItemTax(
    /** Tax authority ID */
    val taxId: Int,
    /** Tax rate percentage */
    val taxRate: BigDecimal,
    /** Tax amount for this line */
    val amount: BigDecimal
)

/**
 * Represents a payment applied to a transaction.
 * 
 * Per DATABASE_SCHEMA.md - Transaction payments array structure.
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md - Full API field alignment.
 */
data class TransactionPayment(
    val id: Long,
    val transactionId: Long,
    
    // ========================================================================
    // GUIDs for API
    // ========================================================================
    /** Parent transaction GUID */
    val transactionGuid: String,
    /** Unique payment GUID */
    val transactionPaymentGuid: String,
    
    // ========================================================================
    // Payment Type & Status
    // Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md:
    // Payment type IDs NOW ALIGNED with backend API
    // ========================================================================
    /** Payment method ID (local enum, will be mapped to API value) */
    val paymentMethodId: Int,
    val paymentMethodName: String,
    /** Payment type for API (per PaymentTypeApi constants) */
    val paymentTypeId: Int = 0,
    /** Account type (default 0) */
    val accountTypeId: Int = 0,
    /** Payment status (per PaymentStatusApi constants) */
    val statusId: Int = 0,
    
    // ========================================================================
    // Amount
    // ========================================================================
    val value: BigDecimal,
    
    // ========================================================================
    // Card Transaction Details
    // ========================================================================
    val referenceNumber: String?,
    val approvalCode: String?,
    val cardType: String?,
    val cardLastFour: String?,
    /** Masked card number for API (e.g., "************1234") */
    val creditCardNumber: String? = null,
    
    // ========================================================================
    // PAX Terminal Data (for card payments)
    // ========================================================================
    val paxData: PaxData? = null,
    
    // ========================================================================
    // Status
    // ========================================================================
    val isSuccessful: Boolean,
    
    // ========================================================================
    // Timestamp
    // ========================================================================
    val paymentDateTime: String
) {
    companion object {
        // ====================================================================
        // Payment Method IDs - NOW ALIGNED WITH BACKEND API
        // Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md
        // ====================================================================
        const val CASH = 0
        const val CASH_CHANGE = 1
        const val CREDIT = 2
        const val DEBIT = 3
        const val UNUSED = 4
        const val EBT_SNAP = 5
        const val EBT_CASH = 6
        const val CHECK = 7
        
        // ====================================================================
        // Payment Status IDs
        // ====================================================================
        const val STATUS_SUCCESS = 0
        const val STATUS_ERROR = 1
        const val STATUS_TIMEOUT = 2
        const val STATUS_ABORTED = 3
        const val STATUS_VOIDED = 4
        const val STATUS_DECLINE = 5
        const val STATUS_REFUND = 6
        const val STATUS_CANCEL = 7
    }
}

/**
 * PAX terminal response data for card payments.
 * 
 * Per END_OF_TRANSACTION_API_SUBMISSION.md:
 * Contains all fields returned from PAX terminal after processing.
 */
data class PaxData(
    /** PAX transaction ID */
    val id: String,
    
    /** PAX result code (e.g., "000000" for success) */
    val resultCode: String,
    
    /** Result description (e.g., "APPROVED") */
    val resultTxt: String,
    
    /** Authorization code */
    val authCode: String,
    
    /** Approved amount */
    val approvedAmount: String,
    
    /** AVS verification result */
    val avsResponse: String = "",
    
    /** Masked account number */
    val bogusAccountNum: String = "",
    
    /** Card brand (e.g., "VISA", "MASTERCARD") */
    val cardType: String,
    
    /** CVV verification result */
    val cvResponse: String = "",
    
    /** Host response code */
    val hostCode: String = "",
    
    /** Host response message */
    val hostResponse: String = "",
    
    /** Display message */
    val message: String = "",
    
    /** Reference number */
    val refNum: String = "",
    
    /** Full terminal response (raw) */
    val rawResponse: String = "",
    
    /** Remaining balance (gift/EBT cards) */
    val remainingBalance: String? = null,
    
    /** Extra balance info */
    val extraBalance: String = "",
    
    /** Originally requested amount */
    val requestedAmount: String = "",
    
    /** Terminal timestamp (format: "YYYYMMDDHHmmss") */
    val timestamp: String,
    
    /** Signature file name */
    val sigFileName: String = "",
    
    /** Signature data (if captured) */
    val signData: String = "",
    
    /** Extended data */
    val extData: String = ""
)

