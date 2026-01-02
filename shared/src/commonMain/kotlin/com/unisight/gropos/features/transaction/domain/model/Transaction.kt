package com.unisight.gropos.features.transaction.domain.model

import java.math.BigDecimal

/**
 * Represents a completed transaction document.
 * 
 * Per DATABASE_SCHEMA.md - LocalTransaction collection structure.
 * All monetary values use BigDecimal for precision.
 * 
 * Document ID: {id} or {id}-P (pending sync)
 * Collection: LocalTransaction
 * Scope: local
 */
data class Transaction(
    val id: Long,
    val guid: String,
    
    // Station/Employee information
    val branchId: Int = 1,
    val stationId: Int = 1,
    val employeeId: Int? = null,
    val employeeName: String? = null,
    
    // Transaction status
    val transactionStatusId: Int = COMPLETED,
    val transactionTypeName: String = "Sale",
    
    // Timestamps (ISO-8601 format)
    val startDateTime: String,
    val completedDateTime: String,
    val completedDate: String,
    
    // Totals (per DATABASE_SCHEMA.md)
    val subTotal: BigDecimal,
    val discountTotal: BigDecimal,
    val taxTotal: BigDecimal,
    val crvTotal: BigDecimal,
    val grandTotal: BigDecimal,
    
    // Item count
    val itemCount: Int,
    
    // Customer info (optional)
    val customerName: String? = null,
    val loyaltyCardNumber: String? = null,
    
    // Line items
    val items: List<TransactionItem>,
    
    // Payments
    val payments: List<TransactionPayment>
) {
    companion object {
        // Transaction Status IDs per DATABASE_SCHEMA.md
        const val PENDING = 1
        const val COMPLETED = 2
        const val VOIDED = 3
        const val ON_HOLD = 4
    }
}

/**
 * Represents a line item in a transaction.
 * 
 * Per DATABASE_SCHEMA.md - Transaction items array structure.
 */
data class TransactionItem(
    val id: Long,
    val transactionId: Long,
    val branchProductId: Int,
    val branchProductName: String,
    
    // Quantity and pricing
    val quantityUsed: BigDecimal,
    val unitType: String,
    val retailPrice: BigDecimal,
    val salePrice: BigDecimal?,
    val priceUsed: BigDecimal,
    
    // Discounts
    val discountAmountPerUnit: BigDecimal,
    val transactionDiscountAmountPerUnit: BigDecimal,
    val floorPrice: BigDecimal?,
    
    // Tax
    val taxPerUnit: BigDecimal,
    val taxTotal: BigDecimal,
    
    // CRV
    val crvRatePerUnit: BigDecimal,
    
    // Totals
    val subTotal: BigDecimal,
    val savingsTotal: BigDecimal,
    
    // Flags
    val isRemoved: Boolean,
    val isPromptedPrice: Boolean,
    val isFloorPriceOverridden: Boolean,
    
    // Type information
    val soldById: String,
    val taxIndicator: String,
    val isFoodStampEligible: Boolean,
    
    // Timestamp
    val scanDateTime: String?
)

/**
 * Represents a payment applied to a transaction.
 * 
 * Per DATABASE_SCHEMA.md - Transaction payments array structure.
 */
data class TransactionPayment(
    val id: Long,
    val transactionId: Long,
    val paymentMethodId: Int,
    val paymentMethodName: String,
    val value: BigDecimal,
    
    // Card transaction details (optional)
    val referenceNumber: String?,
    val approvalCode: String?,
    val cardType: String?,
    val cardLastFour: String?,
    
    // Status
    val isSuccessful: Boolean,
    
    // Timestamp
    val paymentDateTime: String
) {
    companion object {
        // Payment Method IDs per PAYMENT_PROCESSING.md
        const val CASH = 1
        const val CREDIT = 2
        const val DEBIT = 3
        const val EBT_SNAP = 4
        const val EBT_CASH = 5
        const val CHECK = 6
    }
}

