package com.unisight.gropos.features.transaction.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API DTOs for Transaction Submission.
 * 
 * **Source of Truth:** docs/development-plan/TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md
 * **Backend Endpoint:** POST /api/v1/transactions/create-transaction
 * 
 * **CRITICAL - Precision Constraint:**
 * All monetary values are String to prevent floating-point precision loss.
 * Backend expects exact decimal representation (e.g., "46.00", "2.500").
 * 
 * **Zero-Trust:** Do not log PII or sensitive transaction data.
 */

// ============================================================================
// Root Request DTO
// ============================================================================

/**
 * Top-level request wrapper for transaction creation.
 * 
 * Per END_OF_TRANSACTION_API_SUBMISSION.md:
 * ```json
 * {
 *   "transaction": { AddEditTransactionRequest },
 *   "items": [ AddEditTransactionItemRequest ],
 *   "payments": [ AddEditTransactionPaymentRequest ]
 * }
 * ```
 */
@Serializable
data class CreateTransactionRequest(
    val transaction: AddEditTransactionRequest,
    val items: List<AddEditTransactionItemRequest>,
    val payments: List<AddEditTransactionPaymentRequest>
)

// ============================================================================
// Transaction Header DTO
// ============================================================================

/**
 * Transaction header data for API submission.
 * 
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md - Transaction Header Mapping.
 * All monetary values as String for precision.
 */
@Serializable
data class AddEditTransactionRequest(
    /** Backend transaction ID (null for new transactions) */
    val id: Int? = null,
    
    /** Unique transaction identifier (UUID) */
    val guid: String,
    
    /** Associated customer ID (optional) */
    val customerId: Int? = null,
    
    /** Transaction status per TransactionStatusApi constants */
    val transactionStatusId: Int,
    
    /** When transaction started (ISO-8601) */
    val startDate: String,
    
    /** First payment timestamp (ISO-8601, nullable) */
    val paymentDate: String? = null,
    
    /** Transaction completion time (ISO-8601) */
    val completedDate: String,
    
    /** Total line items including removed/voided */
    val rowCount: Int,
    
    /** Active (non-removed) item count */
    val itemCount: Int,
    
    /** Distinct product count */
    val uniqueProductCount: Int,
    
    /** Distinct products with sale price > 0 */
    val uniqueSaleProductCount: Int,
    
    /** Sum of all quantities (String for precision) */
    val totalPurchaseCount: String,
    
    /** Total cost of goods (3 decimal places) */
    val costTotal: String,
    
    /** Total customer savings */
    val savingsTotal: String,
    
    /** Total tax amount */
    val taxTotal: String,
    
    /** Subtotal before tax */
    val subTotal: String,
    
    /** California Redemption Value total */
    val crvTotal: String,
    
    /** Additional fees */
    val fee: String,
    
    /** Final transaction total (2 decimal places) */
    val grandTotal: String
)

// ============================================================================
// Transaction Line Item DTO
// ============================================================================

/**
 * Line item data for API submission.
 * 
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md - Transaction Item Mapping.
 * All monetary values as String for precision.
 */
@Serializable
data class AddEditTransactionItemRequest(
    /** Backend item ID (null for new) */
    val id: Int? = null,
    
    /** Parent transaction GUID */
    val transactionGuid: String,
    
    /** Unique item GUID */
    val transactionItemGuid: String,
    
    /** Product ID at this branch */
    val branchProductId: Int,
    
    /** Item was voided/removed */
    val isRemoved: Boolean,
    
    /** When item was scanned (ISO-8601) */
    val scanDate: String,
    
    /** Display order (1-based) */
    val rowNumber: Int,
    
    /** Quantity was manually entered */
    val isManualQuantity: Boolean,
    
    /** Quantity purchased */
    val quantitySold: String,
    
    /** Quantity returned (default "0.000") */
    val quantityReturned: String,
    
    /** UPC/barcode/PLU */
    val itemNumber: String,
    
    /** Price was manually entered */
    val isPromptedPrice: Boolean,
    
    /** EBT/SNAP eligible */
    val isFoodStampable: Boolean,
    
    /** Manager override on floor price */
    val isFloorPriceOverridden: Boolean,
    
    /** Manager who approved floor price override */
    val floorPriceOverrideEmployeeId: Int? = null,
    
    /** Type of discount applied (per DiscountTypeApi) */
    val discountTypeId: Int? = null,
    
    /** Discount parameter value */
    val discountTypeAmount: String,
    
    /** Invoice-level discount type */
    val transactionDiscountTypeId: Int? = null,
    
    /** Invoice discount value */
    val transactionDiscountTypeAmount: String,
    
    /** Product cost (3 decimal places) */
    val cost: String,
    
    /** Minimum allowed price */
    val floorPrice: String,
    
    /** Regular price */
    val retailPrice: String,
    
    /** Sale/promo price */
    val salePrice: String,
    
    /** Manually entered price */
    val promptedPrice: String,
    
    /** CRV per unit */
    val crvRatePerUnit: String,
    
    /** Final price applied */
    val priceUsed: String,
    
    /** Net quantity (sold - returned) */
    val quantityUsed: String,
    
    /** Total cost for line (3 decimal places) */
    val costTotal: String,
    
    /** Combined tax rate percentage */
    val taxPercentSum: String,
    
    /** Discount per unit */
    val discountAmountPerUnit: String,
    
    /** Invoice discount per unit */
    val transactionDiscountAmountPerUnit: String,
    
    /** Price after discounts */
    val finalPrice: String,
    
    /** Tax per unit */
    val taxPerUnit: String,
    
    /** Price + tax per unit */
    val finalPriceTaxSum: String,
    
    /** Line subtotal */
    val subTotal: String,
    
    /** Amount paid via EBT */
    val snapPaidAmount: String,
    
    /** Percent paid via EBT */
    val snapPaidPercent: String,
    
    /** Taxable amount */
    val subjectToTaxTotal: String,
    
    /** Line tax total */
    val taxTotal: String,
    
    /** Amount not covered by SNAP */
    val nonSNAPTotal: String,
    
    /** Total paid for line */
    val paidTotal: String,
    
    /** Savings per unit */
    val savingsPerUnit: String,
    
    /** Line savings total */
    val savingsTotal: String,
    
    /** Itemized tax breakdown */
    val taxes: List<AddEditTransactionItemTaxRequest>
)

// ============================================================================
// Transaction Item Tax DTO
// ============================================================================

/**
 * Tax breakdown per item for API submission.
 * 
 * Per END_OF_TRANSACTION_API_SUBMISSION.md:
 * Each item can have multiple tax authorities applied.
 */
@Serializable
data class AddEditTransactionItemTaxRequest(
    /** Tax authority ID */
    val taxId: Int,
    
    /** Tax percentage */
    val taxRate: String,
    
    /** Tax amount for this line */
    val amount: String
)

// ============================================================================
// Transaction Payment DTO
// ============================================================================

/**
 * Payment data for API submission.
 * 
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md - Payment Mapping.
 * All monetary values as String for precision.
 */
@Serializable
data class AddEditTransactionPaymentRequest(
    /** Backend payment ID (null for new) */
    val id: Int? = null,
    
    /** Parent transaction GUID */
    val transactionGuid: String,
    
    /** Unique payment GUID */
    val transactionPaymentGuid: String,
    
    /** When payment was made (ISO-8601) */
    val paymentDate: String,
    
    /** Payment method per PaymentTypeApi constants */
    val paymentTypeId: Int,
    
    /** Account type (default 0) */
    val accountTypeId: Int,
    
    /** Payment status per PaymentStatusApi constants */
    val statusId: Int,
    
    /** Payment amount */
    val value: String,
    
    /** Masked card number (e.g., "************1234") */
    val creditCardNumber: String? = null,
    
    /** PAX terminal response data (for card payments) */
    val paxData: PaxDataRequest? = null
)

// ============================================================================
// PAX Terminal Data DTO
// ============================================================================

/**
 * PAX terminal response data for card payments.
 * 
 * Per END_OF_TRANSACTION_API_SUBMISSION.md:
 * Contains all fields returned from PAX terminal after processing.
 */
@Serializable
data class PaxDataRequest(
    /** PAX transaction ID */
    val id: String,
    
    /** Backend payment ID (null for new) */
    val transactionPaymentId: Int? = null,
    
    /** PAX result code (e.g., "000000" for success) */
    val resultCode: String,
    
    /** Result description (e.g., "APPROVED") */
    val resultTxt: String,
    
    /** Authorization code */
    val authCode: String,
    
    /** Approved amount */
    val approvedAmount: String,
    
    /** AVS verification result */
    val avsResponse: String,
    
    /** Masked account number */
    val bogusAccountNum: String,
    
    /** Card brand (e.g., "VISA", "MASTERCARD") */
    val cardType: String,
    
    /** CVV verification result */
    val cvResponse: String,
    
    /** Host response code */
    val hostCode: String,
    
    /** Host response message */
    val hostResponse: String,
    
    /** Display message */
    val message: String,
    
    /** Reference number */
    val refNum: String,
    
    /** Full terminal response (raw) */
    val rawResponse: String,
    
    /** Remaining balance (gift/EBT cards) */
    val remainingBalance: String,
    
    /** Extra balance info */
    val extraBalance: String,
    
    /** Originally requested amount */
    val requestedAmount: String,
    
    /** Terminal timestamp (format: "YYYYMMDDHHmmss") */
    val timestamp: String,
    
    /** Signature file name */
    val sigFileName: String,
    
    /** Signature data (if captured) */
    val signData: String,
    
    /** Extended data */
    val extData: String
)

// ============================================================================
// Transaction Context DTO (Store/Device/Employee Info)
// ============================================================================

/**
 * Context information for transaction submission.
 * 
 * Contains metadata about the transaction environment.
 */
@Serializable
data class TransactionContextDto(
    /** Store/Branch ID */
    val branchId: Int,
    
    /** Device/Station ID */
    val stationId: Int,
    
    /** Device unique identifier */
    val deviceId: Int? = null,
    
    /** Employee who processed the transaction */
    val employeeId: Int? = null,
    
    /** Employee display name */
    val employeeName: String? = null,
    
    /** Till/Location Account ID */
    val locationAccountId: Int? = null,
    
    /** Shift ID (for end-of-day reconciliation) */
    val shiftId: Int? = null
)

// ============================================================================
// Transaction Discount DTO
// ============================================================================

/**
 * Discount details for transaction-level discounts.
 * 
 * Per END_OF_TRANSACTION_API_SUBMISSION.md - Discount Types.
 */
@Serializable
data class TransactionDiscountDto(
    /** Discount type per DiscountTypeApi */
    val discountTypeId: Int,
    
    /** Discount amount or percentage (depending on type) */
    val discountValue: String,
    
    /** Employee who applied the discount */
    val appliedByEmployeeId: Int? = null,
    
    /** Reason code for the discount */
    val reasonCode: String? = null
)

// ============================================================================
// Transaction Void DTO
// ============================================================================

/**
 * Void/Cancel transaction request.
 * 
 * Used when voiding an entire transaction.
 */
@Serializable
data class TransactionVoidDto(
    /** Transaction GUID to void */
    val transactionGuid: String,
    
    /** Reason for voiding */
    val voidReason: String,
    
    /** Employee who performed the void */
    val voidedByEmployeeId: Int,
    
    /** Manager who approved (if required) */
    val approvedByEmployeeId: Int? = null,
    
    /** Void timestamp (ISO-8601) */
    val voidedAt: String
)

