package com.unisight.gropos.features.transaction.data.dto

/**
 * Transaction-related enum constants for API communication.
 * 
 * **Source of Truth:** docs/development-plan/TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md
 * 
 * **CRITICAL:** These values MUST match the backend enum values exactly.
 * Do NOT change these values without confirming with the backend team.
 */

// ============================================================================
// Transaction Status
// ============================================================================

/**
 * Transaction status values per backend API.
 * 
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md - Gap Analysis:
 * Backend values differ from original local values.
 * 
 * | Value | Name       | Description                        |
 * |-------|------------|------------------------------------|
 * | 0     | Open       | Transaction in progress            |
 * | 1     | Processing | Payment being processed            |
 * | 2     | Errored    | Submission failed (queued retry)   |
 * | 3     | Voided     | Transaction cancelled              |
 * | 4     | Completed  | Successfully completed             |
 * | 5     | Hold       | Transaction on hold                |
 */
object TransactionStatusApi {
    const val OPEN = 0
    const val PROCESSING = 1
    const val ERRORED = 2
    const val VOIDED = 3
    const val COMPLETED = 4
    const val HOLD = 5
    
    fun fromLocalStatus(localStatus: Int): Int {
        // Domain model now uses API-aligned values, so pass through directly
        // Transaction.COMPLETED = 4 (same as API)
        // Transaction.HOLD = 5 (same as API)
        return when (localStatus) {
            OPEN -> OPEN
            PROCESSING -> PROCESSING
            ERRORED -> ERRORED
            VOIDED -> VOIDED
            COMPLETED -> COMPLETED
            HOLD -> HOLD
            else -> COMPLETED // Default to COMPLETED for unknown
        }
    }
    
    fun toDisplayString(status: Int): String {
        return when (status) {
            OPEN -> "Open"
            PROCESSING -> "Processing"
            ERRORED -> "Errored"
            VOIDED -> "Voided"
            COMPLETED -> "Completed"
            HOLD -> "On Hold"
            else -> "Unknown"
        }
    }
}

// ============================================================================
// Payment Type
// ============================================================================

/**
 * Payment type values per backend API.
 * 
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md - Gap Analysis:
 * Backend values differ from original local values.
 * 
 * | Value | Name          | Description                   |
 * |-------|---------------|-------------------------------|
 * | 0     | Cash          | Cash payment                  |
 * | 1     | CashChange    | Change given back             |
 * | 2     | Credit        | Credit card                   |
 * | 3     | Debit         | Debit card                    |
 * | 4     | UNUSED        | Reserved                      |
 * | 5     | EBTFoodstamp  | SNAP/EBT Food                 |
 * | 6     | EBTCash       | EBT Cash                      |
 * | 7     | Check         | Check payment                 |
 */
object PaymentTypeApi {
    const val CASH = 0
    const val CASH_CHANGE = 1
    const val CREDIT = 2
    const val DEBIT = 3
    const val UNUSED = 4
    const val EBT_FOODSTAMP = 5
    const val EBT_CASH = 6
    const val CHECK = 7
    
    /**
     * Maps local payment method ID to API payment type ID.
     * 
     * Local values were:
     * - CASH = 1, CREDIT = 2, DEBIT = 3, EBT_SNAP = 4, EBT_CASH = 5, CHECK = 6
     * 
     * API values:
     * - CASH = 0, CREDIT = 2, DEBIT = 3, EBT_FOODSTAMP = 5, EBT_CASH = 6, CHECK = 7
     */
    fun fromLocalPaymentType(localType: Int): Int {
        return when (localType) {
            1 -> CASH           // Local CASH = 1 -> API CASH = 0
            2 -> CREDIT         // Local CREDIT = 2 -> API CREDIT = 2
            3 -> DEBIT          // Local DEBIT = 3 -> API DEBIT = 3
            4 -> EBT_FOODSTAMP  // Local EBT_SNAP = 4 -> API EBT_FOODSTAMP = 5
            5 -> EBT_CASH       // Local EBT_CASH = 5 -> API EBT_CASH = 6
            6 -> CHECK          // Local CHECK = 6 -> API CHECK = 7
            7 -> CASH           // Local OnAccount = 7 -> Default to CASH
            else -> CASH
        }
    }
    
    fun toDisplayString(type: Int): String {
        return when (type) {
            CASH -> "Cash"
            CASH_CHANGE -> "Change"
            CREDIT -> "Credit"
            DEBIT -> "Debit"
            EBT_FOODSTAMP -> "EBT SNAP"
            EBT_CASH -> "EBT Cash"
            CHECK -> "Check"
            else -> "Unknown"
        }
    }
}

// ============================================================================
// Payment Status
// ============================================================================

/**
 * Payment status values per backend API.
 * 
 * | Value | Name    | Description            |
 * |-------|---------|------------------------|
 * | 0     | Success | Payment approved       |
 * | 1     | Error   | Payment error          |
 * | 2     | Timeout | Request timed out      |
 * | 3     | Aborted | User cancelled         |
 * | 4     | Voided  | Payment voided         |
 * | 5     | Decline | Card declined          |
 * | 6     | Refund  | Refund issued          |
 * | 7     | Cancel  | Payment cancelled      |
 */
object PaymentStatusApi {
    const val SUCCESS = 0
    const val ERROR = 1
    const val TIMEOUT = 2
    const val ABORTED = 3
    const val VOIDED = 4
    const val DECLINE = 5
    const val REFUND = 6
    const val CANCEL = 7
    
    fun toDisplayString(status: Int): String {
        return when (status) {
            SUCCESS -> "Success"
            ERROR -> "Error"
            TIMEOUT -> "Timeout"
            ABORTED -> "Aborted"
            VOIDED -> "Voided"
            DECLINE -> "Declined"
            REFUND -> "Refund"
            CANCEL -> "Cancelled"
            else -> "Unknown"
        }
    }
}

// ============================================================================
// Discount Type
// ============================================================================

/**
 * Discount type values per backend API.
 * 
 * | Value | Name                     | Description                 |
 * |-------|--------------------------|-----------------------------|
 * | 0     | ItemPercentage           | Percentage off item         |
 * | 1     | ItemAmountPerUnit        | Fixed amount off per unit   |
 * | 2     | ItemAmountTotal          | Fixed amount off total      |
 * | 3     | TransactionAmountTotal   | Fixed amount off transaction|
 * | 4     | TransactionPercentTotal  | Percentage off transaction  |
 */
object DiscountTypeApi {
    const val ITEM_PERCENTAGE = 0
    const val ITEM_AMOUNT_PER_UNIT = 1
    const val ITEM_AMOUNT_TOTAL = 2
    const val TRANSACTION_AMOUNT_TOTAL = 3
    const val TRANSACTION_PERCENT_TOTAL = 4
    
    fun toDisplayString(type: Int): String {
        return when (type) {
            ITEM_PERCENTAGE -> "Item % Off"
            ITEM_AMOUNT_PER_UNIT -> "Item $ Off/Unit"
            ITEM_AMOUNT_TOTAL -> "Item $ Off Total"
            TRANSACTION_AMOUNT_TOTAL -> "Order $ Off"
            TRANSACTION_PERCENT_TOTAL -> "Order % Off"
            else -> "Unknown"
        }
    }
}

// ============================================================================
// Sync Status (for local tracking)
// ============================================================================

/**
 * Local sync status for tracking transaction transmission state.
 * 
 * Used in local database to track whether transaction
 * has been successfully sent to backend.
 */
object SyncStatus {
    /** Not yet attempted to sync */
    const val PENDING = 0
    
    /** Currently syncing */
    const val IN_PROGRESS = 1
    
    /** Successfully synced to backend */
    const val SYNCED = 2
    
    /** Sync failed, will retry */
    const val FAILED = 3
    
    /** Permanently failed after max retries */
    const val ABANDONED = 4
}

